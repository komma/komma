package net.enilink.komma.edit.ui.properties.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

import com.google.inject.Provider;

import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.ui.assist.ParboiledProposalProvider;
import net.enilink.komma.edit.ui.assist.ReflectiveSemanticProposals;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
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
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

public class ManchesterEditingSupport extends ResourceEditingSupport {
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

	class ManchesterProposals extends ReflectiveSemanticProposals {
		IEntity subject;

		public ManchesterProposals(IEntity subject) {
			this.subject = subject;
		}

		public IContentProposal[] IriRef(ParsingResult<?> result, int index,
				String prefix) {
			StringBuilder text = new StringBuilder();
			for (int l = 1; l <= result.inputBuffer.getLineCount(); l++) {
				text.append(result.inputBuffer.extractLine(l));
			}

			int insertPos = index - prefix.length();

			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			for (IEntity resource : getResourceProposals(subject, null, prefix,
					20)) {
				String label = getLabel(resource);
				String origText = text.substring(insertPos, index);
				// insert proposal text
				text.replace(insertPos, index, label);
				// create proposal
				proposals.add(new ResourceProposal(text.toString(), insertPos
						+ label.length(), resource));
				// restore original text
				text.replace(insertPos, insertPos + label.length(), origText);
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}
	};

	public ManchesterEditingSupport(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	protected ICommand addStatements(final IModel model, final Object subject,
			final List<Object[]> stmts) {
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
		} else if (value instanceof IriRef || value instanceof QName) {
			return toURI(model, value);
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
	public ProposalSupport getProposalSupport(final IEntity subject,
			IReference property, Object value) {
		final IItemLabelProvider resourceLabelProvider = super
				.getProposalSupport(subject, property, value)
				.getLabelProvider();

		final ManchesterSyntaxParser parser = Parboiled.createParser(
				ManchesterSyntaxParser.class, new ManchesterActions(
						new Provider<IModel>() {
							public IModel get() {
								if (subject instanceof IObject) {
									return ((IObject) subject).getModel();
								}
								return null;
							}
						}));
		return new ProposalSupport() {
			@Override
			public IContentProposalProvider getProposalProvider() {
				return new ParboiledProposalProvider(parser.Description(),
						new ManchesterProposals(subject));
			}

			@Override
			public char[] getAutoActivationCharacters() {
				return null;
			}

			@Override
			public IItemLabelProvider getLabelProvider() {
				return new IItemLabelProvider() {
					@Override
					public String getText(Object object) {
						if (object instanceof ResourceProposal) {
							return resourceLabelProvider.getText(object);
						}
						return ((IContentProposal) object).getLabel();
					}

					@Override
					public Object getImage(Object object) {
						if (object instanceof ResourceProposal) {
							return resourceLabelProvider.getImage(object);
						}
						return null;
					}
				};
			}
		};
	}

	@Override
	public ICommand convertValueFromEditor(Object editorValue,
			final IEntity subject, IReference property, Object oldValue) {
		final List<Object[]> newStmts = new ArrayList<Object[]>();
		ParsingResult<Object> result = new BasicParseRunner<Object>(Parboiled
				.createParser(ManchesterSyntaxParser.class,
						new ManchesterActions(new Provider<IModel>() {
							@Override
							public IModel get() {
								return ((IObject) subject).getModel();
							}
						}) {
							@Override
							public boolean createStmt(Object subject,
									Object predicate, Object object) {
								newStmts.add(new Object[] { subject, predicate,
										object });
								return true;
							}
						}).Description()).run((String) editorValue);
		if (result.resultValue != null) {
			return addStatements(((IObject) subject).getModel(),
					result.resultValue, newStmts);
		}
		return null;
	}
}
