package net.enilink.komma.edit.ui.celleditor;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.ui.assist.ContentProposals;
import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.properties.IPropertyEditingSupport;
import net.enilink.komma.edit.properties.IResourceProposal;
import net.enilink.komma.edit.properties.ResourceEditingSupport;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.assist.JFaceContentProposal;
import net.enilink.komma.edit.ui.assist.JFaceProposalProvider;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.edit.util.PropertyUtil;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITransaction;

/**
 * An abstract base class for editing of cells which represent RDF statements.
 */
public abstract class StatementEditingSupport extends EditingSupport {
	private static final int PROPOSAL_DELAY = 1000;

	private Object currentElement;

	private CellEditor currentEditor;

	private TextCellEditorWithContentProposal textCellEditor;

	private IPropertyEditingSupport propertyEditingSupport;

	protected final boolean editPredicate;

	private IResourceProposal acceptedResourceProposal;

	private ICellEditorListener cellEditorListener = new ICellEditorListener() {
		@Override
		public void editorValueChanged(boolean oldValidState,
				boolean newValidState) {
			// user modifications reset the last value proposal
			acceptedResourceProposal = null;
		}

		@Override
		public void cancelEditor() {
			applyEditorValue();
		}

		@Override
		public void applyEditorValue() {
			// ensure that initial state is restored
			editorClosed(currentElement);
		}
	};

	protected IAdapterFactory delegatingAdapterFactory = new IAdapterFactory() {
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

	public StatementEditingSupport(ColumnViewer viewer) {
		this(viewer, false, SWT.NONE);
	}

	public StatementEditingSupport(final ColumnViewer viewer,
			boolean editPredicate, final int cellEditorStyle) {
		super(viewer);
		this.editPredicate = editPredicate;

		textCellEditor = new TextCellEditorWithContentProposal(
				(Composite) viewer.getControl(), cellEditorStyle | SWT.BORDER,
				null, null) {
			@Override
			public void deactivate() {
				fireApplyEditorValue();
				super.deactivate();
			}

			protected void focusLost() {
			}

			@Override
			public LayoutData getLayoutData() {
				LayoutData layoutData = super.getLayoutData();
				if ((cellEditorStyle & SWT.MULTI) != 0) {
					layoutData.verticalAlignment = SWT.TOP;
					layoutData.minimumHeight = getItemHeight() * 6;
				}
				return layoutData;
			}
		};
		textCellEditor.getContentProposalAdapter().setAutoActivationDelay(
				PROPOSAL_DELAY);
		textCellEditor.getContentProposalAdapter().addContentProposalListener(
				new IContentProposalListener() {
					@Override
					public void proposalAccepted(IContentProposal proposal) {
						Object delegate = proposal instanceof JFaceContentProposal ? ((JFaceContentProposal) proposal)
								.getDelegate() : proposal;
						if (delegate instanceof IResourceProposal
								&& ((IResourceProposal) delegate)
										.getUseAsValue()) {
							acceptedResourceProposal = (IResourceProposal) delegate;
						}
					}
				});
		textCellEditor.addListener(cellEditorListener);
	}

	protected int getItemHeight() {
		Control control = getViewer().getControl();
		if (control instanceof Tree) {
			return ((Tree) control).getItemHeight();
		}
		if (control instanceof Table) {
			return ((Table) control).getItemHeight();
		}
		return 0;
	}

	@Override
	protected boolean canEdit(Object element) {
		IStatement stmt = getStatement(element);
		// forbid changing the predicate of existing statements
		if (editPredicate) {
			return stmt.getObject() == null;
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
		return PropertyUtil.getEditingSupport(delegatingAdapterFactory,
				(IEntity) stmt.getSubject(), stmt.getPredicate(),
				stmt.getObject());
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		acceptedResourceProposal = null;
		currentElement = unwrap(element);

		IStatement stmt = getStatement(currentElement);

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
							if (element instanceof JFaceContentProposal) {
								element = ((JFaceContentProposal) element)
										.getDelegate();
							}
							return labelProvider.getText(element);
						}

						@Override
						public Image getImage(Object element) {
							if (element instanceof JFaceContentProposal) {
								element = ((JFaceContentProposal) element)
										.getDelegate();
							}
							return ExtendedImageRegistry.getInstance()
									.getImage(labelProvider.getImage(element));
						}
					});
				} else {
					proposalAdapter.setLabelProvider(null);
				}
				proposalAdapter
						.setContentProposalProvider(JFaceProposalProvider
								.wrap(proposals.getProposalProvider()));
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

	protected Object unwrap(Object itemOrData) {
		if (itemOrData instanceof Item) {
			return ((Item) itemOrData).getData();
		}
		return itemOrData;
	}

	/**
	 * Returns the editing domain for executing commands.
	 */
	abstract protected IEditingDomain getEditingDomain();

	/**
	 * Returns the statement that is represented by the current cell.
	 */
	abstract protected IStatement getStatement(Object element);

	/**
	 * Notifies that the editor for the given element was recently closed.
	 */
	protected void editorClosed(Object element) {
	}

	/**
	 * Assigns a property to an element if operating in property editing mode.
	 */
	protected void setProperty(Object element, IProperty property) {
	}

	/**
	 * Notifies about the resulting status of an editing operation and related
	 * commands.
	 */
	protected void setEditStatus(Object element, IStatus status, Object value) {
	}

	@Override
	protected Object getValue(Object element) {
		return getValueFromStatement(getStatement(unwrap(element)));
	}

	protected Object getValueFromStatement(IStatement stmt) {
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
		if (value == null || value.equals(getValue(element))) {
			return;
		}

		final IStatement stmt = getStatement(element);
		final IResource subject = (IResource) stmt.getSubject();

		ICommand newObjectCommand = null;
		if (currentEditor == textCellEditor) {
			// if value is completely defined by last accepted proposal
			if (acceptedResourceProposal != null) {
				newObjectCommand = new IdentityCommand(
						acceptedResourceProposal.getResource());
			} else {
				// create value with external property editing support
				newObjectCommand = propertyEditingSupport
						.convertValueFromEditor(value,
								(IEntity) stmt.getSubject(),
								stmt.getPredicate(), stmt.getObject());
			}
			if (editPredicate && newObjectCommand != null) {
				try {
					newObjectCommand.execute(new NullProgressMonitor(), null);
				} catch (ExecutionException e) {
					IStatus status = EditUIUtil.createErrorStatus(e);
					KommaEditUIPlugin.getPlugin().log(status);
				}
				Object newPredicate = newObjectCommand.getCommandResult()
						.getReturnValue();
				if (newPredicate instanceof IProperty) {
					setProperty(element, (IProperty) newPredicate);
				}
				newObjectCommand = null;
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
										getEditingDomain(),
										(IResource) stmt.getSubject(),
										(IProperty) stmt.getPredicate(),
										stmt.getObject()), progressMonitor,
										info);
						if (status.isOK()) {
							status = addAndExecute(PropertyUtil.getAddCommand(
									getEditingDomain(), subject,
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
				status = getEditingDomain().getCommandStack().execute(command,
						null, null);
			} catch (ExecutionException exc) {
				status = EditUIUtil.createErrorStatus(exc);
			}

			setEditStatus(element, status, value);
		}
	}

	protected IAdapterFactory getAdapterFactory() {
		IEditingDomain editingDomain = getEditingDomain();
		if (editingDomain instanceof AdapterFactoryEditingDomain) {
			return ((AdapterFactoryEditingDomain) editingDomain)
					.getAdapterFactory();
		}
		return null;
	}
}
