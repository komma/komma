package net.enilink.komma.edit.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.assist.IContentProposal;
import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.assist.ParboiledProposalProvider;
import net.enilink.komma.edit.assist.ReflectiveSemanticProposals;
import net.enilink.komma.edit.properties.ResourceFinder.Match;
import net.enilink.komma.edit.properties.ResourceFinder.Options;
import net.enilink.komma.edit.provider.IItemLabelProvider;
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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

public class ManchesterEditingSupport extends ResourceEditingSupport {
	class ManchesterActions implements IManchesterActions {
		@Override
		public boolean createStmt(Object subject, Object predicate,
				Object object) {
			return true;
		}
	}

	class ManchesterProposals extends ReflectiveSemanticProposals {
		IEntityManager em;

		public ManchesterProposals(IEntityManager em) {
			this.em = em;
		}

		public IContentProposal[] IriRef(ParsingResult<?> result, int index,
				String prefix) {
			StringBuilder text = new StringBuilder();
			for (int l = 1; l <= result.inputBuffer.getLineCount(); l++) {
				text.append(result.inputBuffer.extractLine(l));
			}

			int insertPos = index - prefix.length();

			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			Options options = Options.create(em, null, prefix, 20);
			for (Match match : new ResourceFinder().findAnyResources(options)) {
				String label = getLabel(match.resource);
				String origText = text.substring(insertPos, index);
				// insert proposal text
				text.replace(insertPos, index, label);
				// create proposal
				proposals.add(new ResourceProposal(text.toString(), insertPos
						+ label.length(), match.resource).setUseAsValue(text
						.length() == label.length()));
				// restore original text
				text.replace(insertPos, insertPos + label.length(), origText);
			}
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}
	};

	public ManchesterEditingSupport(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	protected CommandResult addStatements(final IEntityManager em,
			final Object subject, final List<Object[]> stmts) {
		Map<BNode, IReference> bNodes = new HashMap<BNode, IReference>();
		List<IStatement> realStmts = new ArrayList<IStatement>();
		for (Object[] stmt : stmts) {
			Object s = stmt[0], p = stmt[1], o = stmt[2];
			realStmts
					.add(new Statement((IReference) toValue(em, s, bNodes),
							(IReference) toValue(em, p, bNodes), toValue(em, o,
									bNodes)));

		}
		em.add(realStmts);
		return CommandResult.newOKCommandResult(toValue(em, subject, bNodes));
	}

	protected IValue toValue(final IEntityManager em, Object value,
			final Map<BNode, IReference> bNodes) {
		if (value instanceof BNode) {
			IReference reference = bNodes.get(value);
			if (reference == null) {
				bNodes.put((BNode) value, reference = em.create());
			}
			return reference;
		} else if (value instanceof IriRef || value instanceof QName) {
			return toURI(em, value);
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
							(URI) toValue(em, genericLiteral.getDatatype(),
									bNodes), genericLiteral.getLanguage());
					return false;
				}
			}, null);
			return (IValue) result[0];
		}
		return (IReference) value;
	}

	@Override
	public IProposalSupport getProposalSupport(Object element) {
		final IEntity subject = getSubject(element);
		if (subject == null) {
			return null;
		}
		final IItemLabelProvider resourceLabelProvider = super
				.getProposalSupport(element).getLabelProvider();
		final ManchesterSyntaxParser parser = Parboiled.createParser(
				ManchesterSyntaxParser.class, new ManchesterActions());
		return new IProposalSupport() {
			@Override
			public IContentProposalProvider getProposalProvider() {
				return new ParboiledProposalProvider(parser.Description(),
						new ManchesterProposals(subject.getEntityManager()));
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
	public ICommand convertEditorValue(final Object editorValue,
			final IEntityManager entityManager, Object element) {
		return new SimpleCommand() {
			@Override
			protected CommandResult doExecuteWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				final List<Object[]> newStmts = new ArrayList<Object[]>();
				ParsingResult<Object> result = new ReportingParseRunner<Object>(
						Parboiled.createParser(ManchesterSyntaxParser.class,
								new ManchesterActions() {
									@Override
									public boolean createStmt(Object subject,
											Object predicate, Object object) {
										newStmts.add(new Object[] { subject,
												predicate, object });
										return true;
									}
								}).Description()).run((String) editorValue);
				if (result.matched && result.resultValue != null) {
					return addStatements(entityManager, result.resultValue,
							newStmts);
				} else {
					return CommandResult.newErrorCommandResult(ErrorUtils
							.printParseErrors(result.parseErrors));
				}
			}
		};
	}
}
