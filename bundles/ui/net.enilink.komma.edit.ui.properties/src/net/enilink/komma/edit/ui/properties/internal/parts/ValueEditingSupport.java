package net.enilink.komma.edit.ui.properties.internal.parts;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;

import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.common.ui.assist.ContentProposals;
import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.properties.internal.wizards.PropertyUtil;
import net.enilink.komma.edit.ui.properties.support.IPropertyEditingSupport;
import net.enilink.komma.edit.ui.properties.support.LiteralEditingSupport;
import net.enilink.komma.edit.ui.properties.support.ManchesterEditingSupport;
import net.enilink.komma.edit.ui.properties.support.ResourceEditingSupport;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.Statement;

class ValueEditingSupport extends EditingSupport {
	private static final int PROPOSAL_DELAY = 1000;

	private Object currentElement;
	private boolean createNew;
	private IEditingDomain editingDomain;

	private CellEditor currentEditor;

	private TextCellEditorWithContentProposal textCellEditor;

	private IPropertyEditingSupport propertyEditingSupport;

	private boolean editPredicate;

	private ICellEditorListener cellEditorListener = new ICellEditorListener() {
		@Override
		public void editorValueChanged(boolean oldValidState,
				boolean newValidState) {
		}

		@Override
		public void cancelEditor() {
			applyEditorValue();
		}

		@Override
		public void applyEditorValue() {
			if (createNew) {
				// ensure that initial state is restored
				((PropertyNode) currentElement)
						.setCreateNewStatementOnEdit(false);
				getViewer().update(currentElement, null);
			}
		}
	};

	private IAdapterFactory delegatingAdapterFactory = new IAdapterFactory() {
		@Override
		public boolean isFactoryForType(Object type) {
			IAdapterFactory factory = getAdapterFactory();
			return factory == null ? false : getAdapterFactory()
					.isFactoryForType(type);
		}

		@Override
		public Object adapt(Object object, Object type) {
			IAdapterFactory factory = getAdapterFactory();
			return factory == null ? null : getAdapterFactory().adapt(object,
					type);
		}
	};

	public ValueEditingSupport(TreeViewer viewer) {
		this(viewer, false);
	}

	public ValueEditingSupport(TreeViewer viewer, boolean editPredicate) {
		super(viewer);
		this.editPredicate = editPredicate;

		textCellEditor = new TextCellEditorWithContentProposal(viewer.getTree()) {
			protected void focusLost() {
			}

			@Override
			public void deactivate() {
				fireApplyEditorValue();
				super.deactivate();
			}
		};
		textCellEditor.getContentProposalAdapter().setProposalAcceptanceStyle(
				ContentProposalAdapter.PROPOSAL_IGNORE);
		textCellEditor.getContentProposalAdapter().setAutoActivationDelay(
				PROPOSAL_DELAY);
		textCellEditor.addListener(cellEditorListener);
	}

	@Override
	protected boolean canEdit(Object element) {
		boolean expandedNode = element instanceof PropertyNode
				&& ((AbstractTreeViewer) getViewer()).getExpandedState(element);
		createNew = expandedNode
				|| element instanceof PropertyNode
				&& (((PropertyNode) element).isCreateNewStatementOnEdit() || ((PropertyNode) element)
						.isIncomplete());
		IStatement stmt = getStatement(element);
		// forbid changing the predicate of existing statements
		if (editPredicate) {
			return !expandedNode && (createNew || stmt.getObject() == null);
		}

		if (stmt.getPredicate() == null || stmt.isInferred()) {
			return false;
		}

		IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport != null) {
			return propertyEditingSupport.canEdit((IEntity) stmt.getSubject(),
					stmt.getPredicate(), stmt.getObject());
		}
		return true;
	}

	protected IPropertyEditingSupport getPropertyEditingSupport(IStatement stmt) {
		// use the resource editor for predicates
		if (editPredicate) {
			return new ResourceEditingSupport(delegatingAdapterFactory, true);
		}

		IAdapterFactory adapterFactory = getAdapterFactory();
		IPropertyEditingSupport support = adapterFactory == null ? null
				: (IPropertyEditingSupport) adapterFactory.adapt(
						stmt.getPredicate(), IPropertyEditingSupport.class);
		if (support != null) {
			return support;
		}

		IProperty property = ((IEntity) stmt.getSubject()).getEntityManager()
				.find(stmt.getPredicate(), IProperty.class);
		if (property.getRdfsRanges().contains(RDFS.TYPE_CLASS)
				|| property.getRdfsRanges().contains(OWL.TYPE_CLASS)) {
			return new ManchesterEditingSupport(delegatingAdapterFactory);
		} else if (!(property instanceof DatatypeProperty || property
				.getRdfsRanges().contains(RDFS.TYPE_LITERAL))
				&& (stmt.getObject() instanceof IReference
						|| property instanceof ObjectProperty
						|| property.getRdfsRanges()
								.contains(RDFS.TYPE_RESOURCE) //
				|| property.getRdfsRanges().contains(OWL.TYPE_THING))) {
			// TODO implement correct selection strategy for resource editor in
			// all possible cases
			return new ResourceEditingSupport(delegatingAdapterFactory);
		}

		return new LiteralEditingSupport();
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		currentElement = unwrap(element);

		if (createNew) {
			((PropertyNode) currentElement).setCreateNewStatementOnEdit(true);
			getViewer().update(element, null);
		}

		IStatement stmt = getStatement(element);

		propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport != null) {
			IPropertyEditingSupport.ProposalSupport proposals = propertyEditingSupport
					.getProposalSupport((IEntity) stmt.getSubject(),
							stmt.getPredicate(), stmt.getObject());
			ContentProposalAdapter proposalAdapter = textCellEditor
					.getContentProposalAdapter();
			if (proposals != null) {
				final IItemLabelProvider labelProvider = proposals
						.getLabelProvider();
				if (labelProvider != null) {
					proposalAdapter.setLabelProvider(new LabelProvider() {
						@Override
						public String getText(Object element) {
							return labelProvider.getText(element);
						}

						@Override
						public Image getImage(Object element) {
							return ExtendedImageRegistry.getInstance()
									.getImage(labelProvider.getImage(element));
						}
					});
				} else {
					proposalAdapter.setLabelProvider(null);
				}
				proposalAdapter.setContentProposalProvider(proposals
						.getProposalProvider());
				proposalAdapter.setAutoActivationCharacters(proposals
						.getAutoActivationCharacters());
			} else {
				proposalAdapter.setLabelProvider(null);
				proposalAdapter
						.setContentProposalProvider(ContentProposals.NULL_PROPOSAL_PROVIDER);
				proposalAdapter.setAutoActivationCharacters(null);
			}
			return currentEditor = textCellEditor;
		}

		return currentEditor = null;
	}

	Object unwrap(Object itemOrData) {
		if (itemOrData instanceof Item) {
			return ((Item) itemOrData).getData();
		}
		return itemOrData;
	}

	IStatement getStatement(Object element) {
		if (createNew) {
			return new Statement(((PropertyNode) element).getResource(),
					((PropertyNode) element).getProperty(), null);
		}

		element = unwrap(element);
		if (element instanceof PropertyNode) {
			element = ((PropertyNode) element).getFirstStatement();
		}
		return (IStatement) element;
	}

	@Override
	protected Object getValue(Object element) {
		IStatement stmt = getStatement(element);

		IPropertyEditingSupport propertyEditingSupport = getPropertyEditingSupport(stmt);
		if (propertyEditingSupport != null) {
			return propertyEditingSupport.getValueForEditor(
					(IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
		}

		return null;
	}

	@Override
	protected void setValue(final Object element, final Object value) {
		if (value == null) {
			return;
		}
		if (value.equals(getValue(element))) {
			return;
		}

		final IStatement stmt = getStatement(element);
		final IResource subject = (IResource) stmt.getSubject();

		ICommand newObjectCommand = null;
		if (currentEditor == textCellEditor) {
			// create value with external property editing support
			newObjectCommand = propertyEditingSupport.convertValueFromEditor(
					value, (IEntity) stmt.getSubject(), stmt.getPredicate(),
					stmt.getObject());
			if (editPredicate) {
				try {
					newObjectCommand.execute(new NullProgressMonitor(), null);
				} catch (ExecutionException e) {
					IStatus status = EditUIUtil.createErrorStatus(e);
					KommaEditUIPlugin.getPlugin().log(status);
				}
				Object newPredicate = newObjectCommand.getCommandResult()
						.getReturnValue();
				if (newPredicate instanceof IProperty) {
					final PropertyNode node = (PropertyNode) element;
					node.setProperty((IProperty) newPredicate);
					getViewer().update(node, null);
					getViewer().getControl().getDisplay()
							.asyncExec(new Runnable() {
								@Override
								public void run() {
									// TODO find a more generic way to
									// do this
									getViewer().editElement(node, 1);
								}
							});

					((PropertyTreeContentProvider) getViewer()
							.getContentProvider()).registerPropertyNode(node);
				}
			}
		}

		if (newObjectCommand != null) {
			ICompositeCommand command = new CompositeCommand() {
				@Override
				protected CommandResult doExecuteWithResult(
						IProgressMonitor progressMonitor, IAdaptable info)
						throws ExecutionException {
					ITransaction transaction = subject.getEntityManager()
							.getTransaction();
					transaction.begin();
					try {
						CommandResult result = super.doExecuteWithResult(
								progressMonitor, info);
						if (!result.getStatus().isOK()) {
							return result;
						}
						// ensure that previously created data is readable (if
						// isolation != READ UNCOMMITTED)
						transaction.commit();
						transaction.begin();

						// if stmt.getObject() == null then this is a new
						// statement
						// and therefore must not be removed
						IStatus status = stmt.getObject() == null ? Status.OK_STATUS
								: addAndExecute(PropertyUtil.getRemoveCommand(
										editingDomain,
										(IResource) stmt.getSubject(),
										(IProperty) stmt.getPredicate(),
										stmt.getObject()), progressMonitor,
										info);
						if (status.isOK()) {
							status = addAndExecute(PropertyUtil.getAddCommand(
									editingDomain, subject,
									(IProperty) stmt.getPredicate(), result
											.getReturnValues().iterator()
											.next()), progressMonitor, info);
						}
						if (status.isOK()) {
							transaction.commit();
						}

						return new CommandResult(status);
					} finally {
						if (transaction.isActive()) {
							transaction.rollback();
						}
					}
				}
			};
			command.add(newObjectCommand);

			IStatus status = Status.CANCEL_STATUS;
			try {
				status = editingDomain.getCommandStack().execute(command, null,
						null);
			} catch (ExecutionException exc) {
				status = EditUIUtil.createErrorStatus(exc);
			}

			if (!status.isOK()) {
				KommaEditUIPlugin.getPlugin().log(status);
			}
		}
	}

	protected IAdapterFactory getAdapterFactory() {
		if (editingDomain instanceof AdapterFactoryEditingDomain) {
			return ((AdapterFactoryEditingDomain) editingDomain)
					.getAdapterFactory();
		}
		return null;
	}

	public void setEditingDomain(IEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}
}
