package net.enilink.komma.edit.ui.properties.internal.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Item;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

import com.google.inject.Provider;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.ICompositeCommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.common.ui.assist.ContentProposalExt;
import net.enilink.komma.common.ui.celleditor.TextCellEditorWithContentProposal;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.assist.ParboiledProposalProvider;
import net.enilink.komma.edit.ui.properties.internal.assist.ManchesterProposals;
import net.enilink.komma.edit.ui.properties.internal.wizards.PropertyUtil;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.parser.manchester.IManchesterActions;
import net.enilink.komma.parser.manchester.ManchesterSyntaxParser;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.BooleanLiteral;
import net.enilink.komma.parser.sparql.tree.DoubleLiteral;
import net.enilink.komma.parser.sparql.tree.GenericLiteral;
import net.enilink.komma.parser.sparql.tree.IntegerLiteral;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.Literal;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.parser.sparql.tree.visitor.TreeWalker;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.util.ISparqlConstants;

class ValueEditingSupport extends EditingSupport {
	class ResourceProposal extends ContentProposalExt {
		IEntity resource;

		public ResourceProposal(String content, int cursorPosition,
				IEntity resource) {
			super(content, Type.REPLACE, labelProvider.getText(resource),
					labelProvider.getText(resource), cursorPosition);
			this.resource = resource;
		}

		public IEntity getResource() {
			return resource;
		}
	}

	class ResourceProposalProvider implements IContentProposalProvider {
		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			for (IEntity resource : getResourceProposals(
					contents.substring(0, position), 20)) {
				String content = getText(resource);
				if (content.length() > 0) {
					proposals.add(new ResourceProposal(content, content
							.length(), resource));
				}
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}
	}

	class ManchesterActions implements IManchesterActions {
		final Map<BNode, IReference> bNodes = new HashMap<BNode, IReference>();
		final Provider<IModel> modelProvider;

		public ManchesterActions(Provider<IModel> modelProvider) {
			this.modelProvider = modelProvider;
		}

		@Override
		public boolean createStmt(Object subject, Object predicate,
				Object object) {
			return true;
		}

		@Override
		public boolean isObjectProperty(Object property) {
			return !isDataProperty(property);
		}

		@Override
		public boolean isDataProperty(Object property) {
			IModel model = modelProvider.get();
			if (model == null) {
				return false;
			}
			return model.getManager().find(
					(IReference) toValue(model, property, bNodes)) instanceof DatatypeProperty;
		}
	}

	private Object currentElement;
	private boolean createNew;
	private IEditingDomain editingDomain;
	private ILabelProvider labelProvider;

	private CellEditor currentEditor;

	private TextCellEditor literalEditor;

	private TextCellEditorWithContentProposal resourceEditor;

	private TextCellEditorWithContentProposal manchesterEditor;

	private final ManchesterSyntaxParser manchesterParser = Parboiled
			.createParser(ManchesterSyntaxParser.class, new ManchesterActions(
					new Provider<IModel>() {
						public IModel get() {
							final IStatement stmt = getStatement(currentElement);
							final IResource subject = (IResource) stmt
									.getSubject();
							if (subject instanceof IObject) {
								return ((IObject) subject).getModel();
							}
							return null;
						}
					}));

	private boolean editPredicate;

	public ValueEditingSupport(TreeViewer viewer) {
		this(viewer, false);
	}

	public ValueEditingSupport(TreeViewer viewer, boolean editPredicate) {
		super(viewer);
		this.editPredicate = editPredicate;

		literalEditor = new TextCellEditor(viewer.getTree());
		addListener(literalEditor);

		resourceEditor = new TextCellEditorWithContentProposal(
				viewer.getTree(), new ResourceProposalProvider(), null);
		resourceEditor.getContentProposalAdapter().setLabelProvider(
				new LabelProvider() {
					@Override
					public Image getImage(Object object) {
						return labelProvider == null ? null : labelProvider
								.getImage(((ResourceProposal) object)
										.getResource());
					}

					@Override
					public String getText(Object object) {
						return ValueEditingSupport.this
								.getText(((ResourceProposal) object)
										.getResource());
					}
				});
		resourceEditor.getContentProposalAdapter().setProposalAcceptanceStyle(
				ContentProposalAdapter.PROPOSAL_REPLACE);
		addListener(resourceEditor);

		ManchesterProposals manchesterProposals = new ManchesterProposals() {
			public IContentProposal[] IriRef(ParsingResult<?> result,
					int index, String prefix) {
				StringBuilder text = new StringBuilder();
				for (int l = 1; l <= result.inputBuffer.getLineCount(); l++) {
					text.append(result.inputBuffer.extractLine(l));
				}

				int insertPos = index - prefix.length();

				List<IContentProposal> proposals = new ArrayList<IContentProposal>();
				for (IEntity resource : getResourceProposals(prefix, 20)) {
					String label = getText(resource);
					String origText = text.substring(insertPos, index);
					// insert proposal text
					text.replace(insertPos, index, label);
					// create proposal
					proposals.add(new ResourceProposal(text.toString(),
							insertPos + label.length(), resource));
					// restore original text
					text.replace(insertPos, insertPos + label.length(),
							origText);
				}
				return proposals
						.toArray(new IContentProposal[proposals.size()]);
			}
		};
		manchesterEditor = new TextCellEditorWithContentProposal(
				viewer.getTree(), new ParboiledProposalProvider(
						manchesterParser.Description(), manchesterProposals),
				null) {
			protected void focusLost() {
			}

			@Override
			public void deactivate() {
				fireApplyEditorValue();
				super.deactivate();
			}
		};
		manchesterEditor.getContentProposalAdapter()
				.setProposalAcceptanceStyle(
						ContentProposalAdapter.PROPOSAL_IGNORE);
		addListener(manchesterEditor);
		manchesterEditor.setStyle(SWT.MULTI | SWT.WRAP);
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
		return stmt.getPredicate() != null && !stmt.isInferred();
	}

	protected void addListener(CellEditor editor) {
		editor.addListener(new ICellEditorListener() {
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
		});
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		currentElement = unwrap(element);

		if (createNew) {
			((PropertyNode) currentElement).setCreateNewStatementOnEdit(true);
			getViewer().update(element, null);
		}

		IStatement stmt = getStatement(element);
		// use the resource editor for predicates
		if (editPredicate) {
			return currentEditor = resourceEditor;
		}
		IProperty property = ((IEntity) stmt.getSubject()).getEntityManager()
				.find(stmt.getPredicate(), IProperty.class);
		if (property.getRdfsRanges().contains(RDFS.TYPE_CLASS)
				|| property.getRdfsRanges().contains(OWL.TYPE_CLASS)) {
			currentEditor = manchesterEditor;
		} else if (stmt.getObject() instanceof IReference
				&& !(property instanceof DatatypeProperty)
				|| property.getRdfsRanges().contains(RDFS.TYPE_RESOURCE)) {
			// TODO implement correct selection strategy for resource editor in
			// all possible cases
			currentEditor = resourceEditor;
		} else {
			currentEditor = literalEditor;
		}
		return currentEditor;
	}

	IExtendedIterator<IResource> getResourceProposals(String template, int limit) {
		template = template.trim();

		IStatement stmt = getStatement(currentElement);

		String uriPattern = template;
		if (!template.matches("[#/]")) {
			uriPattern = "#" + template;

			int colonIndex = template.lastIndexOf(':');
			if (colonIndex == 0) {
				uriPattern = "#" + template.substring(1);
			} else if (colonIndex > 0) {
				String prefix = template.substring(0, colonIndex);
				URI namespaceUri = ((IEntity) stmt.getSubject())
						.getEntityManager().getNamespace(prefix);
				if (namespaceUri != null) {
					uriPattern = namespaceUri.appendFragment(
							template.substring(colonIndex + 1)).toString();
				}
			}
		}

		// TODO incorporate correct ranges
		return ((IEntity) stmt.getSubject())
				.getEntityManager()
				.createQuery(
						ISparqlConstants.PREFIX
								+ "SELECT DISTINCT ?s WHERE {{?s ?p ?o . FILTER regex(str(?s), ?uriPattern)}"
								+ " UNION "
								+ "{?s rdfs:label ?l . FILTER regex(str(?l), ?template)}"
								+ (editPredicate ? "?s a rdf:Property" : "")
								+ "} LIMIT " + limit) //
				.setParameter("uriPattern", uriPattern) //
				.setParameter("template", "^" + template) //
				.evaluate(IResource.class);
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

		Object object = stmt.getObject();
		if (object instanceof ILiteral) {
			ILiteral literal = (ILiteral) object;
			return literal.getLabel();
		}

		return getText(object);
	}

	protected String getText(Object element) {
		return labelProvider == null ? ModelUtil.getLabel(element)
				: labelProvider.getText(element);
	}

	protected ICommand getAddStmtsCommand(final IModel model,
			final Object subject, final List<Object[]> stmts) {
		return new SimpleCommand() {
			@Override
			protected CommandResult doExecuteWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				Map<BNode, IReference> bNodes = new HashMap<BNode, IReference>();
				List<IStatement> realStmts = new ArrayList<IStatement>();
				for (Object[] stmt : stmts) {
					Object s = stmt[0], p = stmt[1], o = stmt[2];
					realStmts.add(new Statement((IReference) toValue(model, s,
							bNodes), (IReference) toValue(model, p, bNodes),
							toValue(model, o, bNodes)));

				}
				model.getManager().add(realStmts);

				return CommandResult.newOKCommandResult(toValue(model, subject,
						bNodes));
			}
		};
	}

	protected IValue toValue(final IModel model, Object value,
			final Map<BNode, IReference> bNodes) {
		final IEntityManager em = model.getManager();
		if (value instanceof BNode) {
			IReference reference = bNodes.get(value);
			if (reference == null) {
				bNodes.put((BNode) value, reference = em.create());
			}
			return reference;
		} else if (value instanceof IriRef) {
			return URIImpl.createURI(((IriRef) value).getIri());
		} else if (value instanceof QName) {
			String prefix = ((QName) value).getPrefix();
			String localPart = ((QName) value).getLocalPart();
			URI ns;
			if (prefix == null || prefix.trim().length() == 0) {
				ns = model.getURI();
			} else {
				ns = model.getManager().getNamespace(prefix);
			}
			if (ns != null) {
				return ns.appendFragment(localPart);
			}
			throw new IllegalArgumentException("Unknown prefix");
		} else if (value instanceof Literal) {
			final IValue[] result = new IValue[1];
			((Literal) value).accept(new TreeWalker<Void>() {
				@Override
				public Boolean integerLiteral(IntegerLiteral numericLiteral,
						Void data) {
					result[0] = em.toValue(numericLiteral.getValue());
					return false;
				}

				@Override
				public Boolean booleanLiteral(BooleanLiteral booleanLiteral,
						Void data) {
					result[0] = em.toValue(booleanLiteral.getValue());
					return false;
				}

				@Override
				public Boolean doubleLiteral(DoubleLiteral doubleLiteral,
						Void data) {
					result[0] = em.toValue(doubleLiteral.getValue());
					return false;
				}

				@Override
				public Boolean genericLiteral(GenericLiteral genericLiteral,
						Void data) {
					result[0] = em.createLiteral(
							genericLiteral.getLabel(),
							(URI) toValue(model, genericLiteral.getDatatype(),
									bNodes), genericLiteral.getLanguage());
					return false;
				}
			}, null);
			return (IValue) result[0];
		}
		return (IReference) value;
	}

	@Override
	protected void setValue(final Object element, Object value) {
		if (value == null) {
			return;
		}
		if (value.equals(getValue(element))) {
			return;
		}

		final IStatement stmt = getStatement(element);
		final IResource subject = (IResource) stmt.getSubject();
		Object object = stmt.getObject();

		ICommand newObjectCommand = null;
		if (value instanceof String) {
			if (currentEditor == manchesterEditor) {
				final List<Object[]> newStmts = new ArrayList<Object[]>();
				ParsingResult<Object> result = new BasicParseRunner<Object>(
						Parboiled.createParser(ManchesterSyntaxParser.class,
								new ManchesterActions(new Provider<IModel>() {
									@Override
									public IModel get() {
										return ((IObject) subject).getModel();
									}
								}) {
									@Override
									public boolean createStmt(Object subject,
											Object predicate, Object object) {
										newStmts.add(new Object[] { subject,
												predicate, object });
										return true;
									}
								}).Description()).run((String) value);
				if (result.resultValue != null) {
					newObjectCommand = getAddStmtsCommand(
							((IObject) subject).getModel(), result.resultValue,
							newStmts);
				}
			} else if (currentEditor == resourceEditor) {
				IExtendedIterator<IResource> resources = getResourceProposals(
						(String) value, 1);
				if (resources.hasNext()) {
					IEntity resource = resources.next();
					if (!resource.equals(object)
							&& getText(resource)
									.equals(((String) value).trim())) {
						if (editPredicate && resource instanceof IProperty) {
							final PropertyNode node = (PropertyNode) element;
							node.setProperty((IProperty) resource);
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
						} else {
							newObjectCommand = new IdentityCommand(resource);
						}
					}
				}
			} else {
				URI literalType = null;
				String literalLanguage = null;
				if (object instanceof ILiteral) {
					literalType = ((ILiteral) object).getDatatype();
					literalLanguage = ((ILiteral) object).getLanguage();
				}
				IValue newLiteral = subject.getEntityManager().createLiteral(
						(String) value, literalType, literalLanguage);
				if (!newLiteral.equals(object)) {
					newObjectCommand = new IdentityCommand(newLiteral);
				}
			}

			if (newObjectCommand != null) {
				ICompositeCommand command = new CompositeCommand() {
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						subject.getEntityManager().getTransaction().begin();
						try {
							CommandResult result = super.doExecuteWithResult(
									progressMonitor, info);
							if (!result.getStatus().isOK()) {
								return result;
							}
							// if stmt.getObject() == null then this is a new
							// statement
							// and therefore must not be removed
							IStatus status = stmt.getObject() == null ? Status.OK_STATUS
									: addAndExecute(
											PropertyUtil.getRemoveCommand(
													editingDomain,
													(IResource) stmt
															.getSubject(),
													(IProperty) stmt
															.getPredicate(),
													stmt.getObject()),
											progressMonitor, info);
							if (status.isOK()) {
								status = addAndExecute(
										PropertyUtil.getAddCommand(
												editingDomain,
												subject,
												(IProperty) stmt.getPredicate(),
												result.getReturnValues()
														.iterator().next()),
										progressMonitor, info);
							}
							if (status.isOK()) {
								subject.getEntityManager().getTransaction()
										.commit();
								// remove transitional template node
								if (element instanceof PropertyNode
										&& ((PropertyNode) element)
												.isIncomplete()) {
									((AbstractTreeViewer) getViewer())
											.remove(element);
								}
							}

							return new CommandResult(status);
						} finally {
							if (subject.getEntityManager().getTransaction()
									.isActive()) {
								subject.getEntityManager().getTransaction()
										.rollback();
							}
						}
					}
				};
				command.add(newObjectCommand);

				IStatus status = Status.CANCEL_STATUS;
				try {
					status = editingDomain.getCommandStack().execute(command,
							null, null);
				} catch (ExecutionException exc) {
					status = EditUIUtil.createErrorStatus(exc);
				}

				if (!status.isOK()) {
					KommaEditUIPlugin.getPlugin().log(status);
				}
			}
		}

		// -- is handled by listener --
		// getViewer().update(element, null);
	}

	public void setEditingDomain(IEditingDomain editingDomain) {
		this.editingDomain = editingDomain;
	}

	public void setLabelProvider(ILabelProvider labelProvider) {
		this.labelProvider = labelProvider;
	}
}
