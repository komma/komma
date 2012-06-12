package net.enilink.komma.edit.ui.properties.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.common.StringUtils;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.IndexRange;
import org.parboiled.support.ParsingResult;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.common.ui.assist.ContentProposalExt;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.util.ISparqlConstants;

public class ResourceEditingSupport implements IPropertyEditingSupport {
	static class ConstructorParser extends BaseRdfParser {
		public Rule Constructor() {
			return Sequence(
					FirstOf(Sequence(FirstOf(IriRef(), PN_LOCAL()), Ch('a')),
							Ch('a')), WS_NO_COMMENT(),
					FirstOf(IriRef(), PN_LOCAL(), Sequence(EMPTY, push(""))),
					drop(), push(matchRange()));
		}
	}

	protected class ResourceProposal extends ContentProposalExt {
		IEntity resource;

		public ResourceProposal(String content, int cursorPosition,
				IEntity resource) {
			super(content, Type.REPLACE, ResourceEditingSupport.this
					.getLabel(resource), ResourceEditingSupport.this
					.getLabel(resource), cursorPosition);
			this.resource = resource;
		}

		public IEntity getResource() {
			return resource;
		}
	}

	class ResourceProposalProvider implements IContentProposalProvider {
		IEntity subject;
		IReference predicate;

		ResourceProposalProvider(IEntity subject, IReference predicate) {
			this.subject = subject;
			this.predicate = predicate;
		}

		@Override
		public IContentProposal[] getProposals(String contents, int position) {
			ParsingResult<Object> ctor = new BasicParseRunner<Object>(
					createConstructorParser().Constructor()).run(contents);
			String prefix = "";
			if (ctor.matched) {
				IndexRange range = (IndexRange) ctor.resultValue;
				prefix = contents.substring(0, range.start);
				contents = contents.substring(range.start, range.end);
				position = contents.length();
			}

			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			for (IEntity resource : getResourceProposals(subject,
					ctor.matched ? null : predicate,
					contents.substring(0, position), 20)) {
				String content = getLabel(resource);
				if (content.length() > 0) {
					content = prefix + content;
					proposals.add(new ResourceProposal(content, content
							.length(), resource));
				}
			}
			Collections.sort(proposals, new Comparator<IContentProposal>() {
				@Override
				public int compare(IContentProposal c1, IContentProposal c2) {
					return c1.getLabel().compareTo(c2.getLabel());
				}
			});
			return proposals.toArray(new IContentProposal[proposals.size()]);
		}
	}

	protected IAdapterFactory adapterFactory;
	private boolean editPredicate;

	public ResourceEditingSupport(IAdapterFactory adapterFactory) {
		this(adapterFactory, false);
	}

	public ResourceEditingSupport(IAdapterFactory adapterFactory,
			boolean editPredicate) {
		this.adapterFactory = adapterFactory;
		this.editPredicate = editPredicate;
	}

	protected String getLabel(Object value) {
		if (value == null) {
			return null;
		}
		IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
				.adapt(value, IItemLabelProvider.class);
		if (labelProvider != null) {
			return labelProvider.getText(value);
		}
		return value.toString();
	}

	protected Iterable<IResource> getResourceProposals(IEntity subject,
			IReference predicate, String template, int limit) {
		Set<IResource> resources = new HashSet<IResource>(20);
		template = template.trim();

		if (subject instanceof IObject && predicate != null && template != null
				&& !template.contains(":")) {
			// find properties within the default namespace first
			URI uri = ((IObject) subject).getModel().getURI();
			String uriPattern = uri.appendLocalPart(template).toString();
			String uriNamespace = uri.appendLocalPart("").toString();

			resources.addAll(retrieve(subject, predicate, template, uriPattern,
					uriNamespace, limit));
		}

		if (resources.size() < limit) {
			// additionally, if limit not exceeded, find properties from other namespaces
			String uriPattern = template;
			if (!template.matches("[#/]")) {
				uriPattern = "#" + template;

				int colonIndex = template.lastIndexOf(':');
				if (colonIndex == 0) {
					uriPattern = "#" + template.substring(1);
				} else if (colonIndex > 0) {
					String prefix = template.substring(0, colonIndex);
					URI namespaceUri = subject.getEntityManager().getNamespace(
							prefix);
					if (namespaceUri != null) {
						uriPattern = namespaceUri.appendFragment(
								template.substring(colonIndex + 1)).toString();
					}
				}
			}

			List<IResource> fallbackResources = retrieve(subject, predicate,
					template, uriPattern, null, limit);
			int count = 0;
			while (resources.size() < limit && count < fallbackResources.size()) {
				resources.add(fallbackResources.get(count));
				count++;
			}
		}

		return resources;
	}

	protected List<IResource> retrieve(IEntity subject, IReference predicate,
			String template, String uriPattern, String uriNamespace, int limit) {
		StringBuilder sparql = new StringBuilder(ISparqlConstants.PREFIX
				+ "SELECT DISTINCT ?s WHERE {");

		if (editPredicate) {
			sparql.append("?s a rdf:Property . ");
		} else {
			// TODO compute correct intersection of ranges
			if (predicate != null) {
				sparql.append("{?property rdfs:range ?sType FILTER (?sType != owl:Thing)}");
				sparql.append(" UNION {?subject a [rdfs:subClassOf ?r] . ?r owl:onProperty ?property {?r owl:allValuesFrom ?sType} UNION {?r owl:someValuesFrom ?sType}}");
				sparql.append(" ?s a ?sType . ");
			}
		}

		sparql.append("{?s ?p ?o . FILTER regex(str(?s), ?uriPattern)}");
		sparql.append(" UNION ");
		sparql.append("{");
		sparql.append("?s rdfs:label ?l . FILTER regex(str(?l), ?template)");
		if (uriNamespace != null) {
			sparql.append(". FILTER regex(str(?s), ?uriNamespace)");
		}
		sparql.append("}");
		sparql.append("} LIMIT " + limit);

		// TODO incorporate correct ranges
		IQuery<?> query = subject.getEntityManager()
				.createQuery(sparql.toString())
				.setParameter("uriPattern", uriPattern)
				.setParameter("template", "^" + template);
		if (uriNamespace != null) {
			query.setParameter("uriNamespace", uriNamespace);
		}

		if (!editPredicate) {
			query.setParameter("subject", subject);
			query.setParameter("property", predicate);
		}

		return query.evaluate(IResource.class).toList();
	}

	protected ConstructorParser createConstructorParser() {
		return Parboiled.createParser(ConstructorParser.class);
	}

	@Override
	public ProposalSupport getProposalSupport(final IEntity subject,
			final IReference property, Object value) {
		return new ProposalSupport() {
			@Override
			public IContentProposalProvider getProposalProvider() {
				return new ResourceProposalProvider(subject, property);
			}

			@Override
			public IItemLabelProvider getLabelProvider() {
				return new IItemLabelProvider() {
					@Override
					public String getText(Object object) {
						IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
								.adapt(((ResourceProposal) object).resource,
										IItemLabelProvider.class);
						if (labelProvider != null) {
							return labelProvider
									.getText(((ResourceProposal) object).resource);
						}
						return ModelUtil
								.getLabel(((ResourceProposal) object).resource);
					}

					@Override
					public Object getImage(Object object) {
						IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
								.adapt(((ResourceProposal) object).resource,
										IItemLabelProvider.class);
						if (labelProvider != null) {
							return labelProvider
									.getImage(((ResourceProposal) object).resource);
						}
						return null;
					}
				};
			}

			@Override
			public char[] getAutoActivationCharacters() {
				return null;
			}
		};
	}

	@Override
	public boolean canEdit(IEntity subject, IReference property, Object value) {
		return true;
	}

	@Override
	public Object getValueForEditor(IEntity subject, IReference property,
			Object value) {
		if (editPredicate) {
			value = property;
		}
		String text = value != null ? getLabel(value) : null;
		return text != null ? text : "";
	}

	protected URI toURI(IModel model, Object value) {
		if (value instanceof IriRef) {
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
				return ns.appendLocalPart(localPart);
			}
			throw new IllegalArgumentException("Unknown prefix");
		} else if (value != null) {
			return model.getURI().appendLocalPart(value.toString());
		}
		return null;
	}

	@Override
	public ICommand convertValueFromEditor(Object editorValue,
			final IEntity subject, IReference property, Object oldValue) {
		final URI[] newName = { null };
		boolean createNew = false;
		ParsingResult<Object> ctor = new BasicParseRunner<Object>(
				createConstructorParser().Constructor())
				.run((String) editorValue);
		if (ctor.matched) {
			createNew = true;
			IndexRange range = (IndexRange) ctor.resultValue;
			editorValue = ((String) editorValue).substring(range.start,
					range.end);
			// check if a name for the new resource is given
			if (ctor.valueStack.size() > 1) {
				if (subject instanceof IObject) {
					newName[0] = toURI(((IObject) subject).getModel(),
							ctor.valueStack.peek(1));
				}
			}
		}
		Iterator<IResource> resources = getResourceProposals(subject,
				createNew ? null : property, (String) editorValue + "$", 1)
				.iterator();
		if (resources.hasNext()) {
			final IEntity resource = resources.next();
			if (createNew
					&& getLabel(resource).equals(((String) editorValue).trim())) {
				// create a new object
				return new SimpleCommand() {
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						return CommandResult.newOKCommandResult(subject
								.getEntityManager().createNamed(newName[0],
										resource));
					}
				};
			} else if (!resource.equals(oldValue)
					&& getLabel(resource).equals(((String) editorValue).trim())) {
				// replace value with existing object
				return new IdentityCommand(resource);
			}
		}
		return null;
	}
}
