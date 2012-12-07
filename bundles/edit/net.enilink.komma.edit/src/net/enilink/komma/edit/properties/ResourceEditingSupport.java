package net.enilink.komma.edit.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.IndexRange;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.assist.ContentProposal;
import net.enilink.komma.edit.assist.IContentProposal;
import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.QueryFragment;
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

	protected class ResourceProposal extends ContentProposal implements
			IResourceProposal {
		IEntity resource;
		boolean useAsValue;

		public ResourceProposal(String content, int cursorPosition,
				IEntity resource) {
			super(content, ResourceEditingSupport.this.getLabel(resource),
					ResourceEditingSupport.this.getLabel(resource),
					cursorPosition, false);
			this.resource = resource;
		}

		@Override
		public IEntity getResource() {
			return resource;
		}

		public ResourceProposal setUseAsValue(boolean useAsValue) {
			this.useAsValue = useAsValue;
			return this;
		}

		public boolean getUseAsValue() {
			return useAsValue;
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
							.length(), resource).setUseAsValue(!ctor.matched));
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

	protected String toUriRegex(String pattern) {
		return "[#/]" + pattern + "[^#/]*$";
	}

	protected Iterable<IResource> getResourceProposals(IEntity subject,
			IReference predicate, String pattern, int limit) {
		Set<IResource> resources = new HashSet<IResource>(20);
		pattern = pattern.trim();

		if (subject instanceof IObject && pattern != null
				&& !pattern.contains(":")) {
			// find resources within the default namespace first
			URI uri = ((IObject) subject).getModel().getURI();
			String uriNamespace = uri.appendLocalPart("").toString();
			resources.addAll(retrieve(subject, predicate, pattern,
					toUriRegex(pattern), uriNamespace, limit));
		}

		if (resources.size() < limit) {
			// additionally, if limit not exceeded, find resources from other
			// namespaces
			String uriPattern = pattern;
			String uriNamespace = null;
			if (!pattern.matches(".*[#/].*")) {
				uriPattern = toUriRegex(pattern);

				int colonIndex = pattern.lastIndexOf(':');
				if (colonIndex == 0) {
					uriPattern = toUriRegex(pattern.substring(1));
				} else if (colonIndex > 0) {
					String prefix = pattern.substring(0, colonIndex);
					URI namespaceUri = subject.getEntityManager().getNamespace(
							prefix);
					if (namespaceUri != null) {
						uriNamespace = namespaceUri.toString();
					}
				}
			}

			List<IResource> fallbackResources = retrieve(subject, predicate,
					pattern, uriPattern, uriNamespace, limit);
			int count = 0;
			while (resources.size() < limit && count < fallbackResources.size()) {
				resources.add(fallbackResources.get(count));
				count++;
			}
		}

		return resources;
	}

	private String[] split(String s, String pattern) {
		List<String> tokens = new ArrayList<String>(Arrays.asList(s
				.split(pattern)));
		while (tokens.remove(""))
			;
		return tokens.toArray(new String[tokens.size()]);
	}

	protected List<IResource> retrieve(IEntity subject, IReference predicate,
			String pattern, String uriPattern, String namespace, int limit) {
		StringBuilder sparql = new StringBuilder(ISparqlConstants.PREFIX
				+ "SELECT DISTINCT ?s WHERE { ");

		if (editPredicate) {
			sparql.append("?s a rdf:Property . ");
		} else {
			if (predicate != null) {
				sparql.append("{ ?property rdfs:range ?sType FILTER (?sType != owl:Thing) }");
				sparql.append(" UNION ");
				sparql.append("{ ?subject a [rdfs:subClassOf ?r] . ?r owl:onProperty ?property { ?r owl:allValuesFrom ?sType }}");
				sparql.append(" UNION ");
				sparql.append("{ ?r owl:someValuesFrom ?sType }");
				sparql.append(" ?s a ?sType ");
			}
		}

		IDialect dialect = subject.getEntityManager().getDialect();
		QueryFragment searchS = dialect.fullTextSearch("s",
				IDialect.CASE_INSENSITIVE | IDialect.ALL,
				split(pattern, "\\s*[#/]\\s*"));
		QueryFragment searchL = dialect.fullTextSearch("l",
				IDialect.CASE_INSENSITIVE, pattern);

		sparql.append("{ ?s ?p ?o . " + searchS);
		sparql.append(" FILTER regex(str(?s), ?uriPattern)");
		sparql.append(" }");
		sparql.append(" UNION ");
		sparql.append("{ ");
		sparql.append("?s rdfs:label ?l . " + searchL);
		sparql.append(" }");
		if (namespace != null) {
			sparql.append(" FILTER regex(str(?s), ?namespace)");
		}
		sparql.append(" } LIMIT " + limit);

		// TODO incorporate correct ranges
		IQuery<?> query = subject.getEntityManager().createQuery(
				sparql.toString());
		searchS.addParameters(query);
		searchL.addParameters(query);
		query.setParameter("uriPattern", uriPattern);
		if (namespace != null) {
			query.setParameter("namespace", "^" + namespace);
		}
		if (!editPredicate) {
			query.setParameter("subject", subject);
			query.setParameter("property", predicate);
		}

		List<IResource> list = query.evaluate(IResource.class).toList();
		if (list.isEmpty() && predicate != null) {
			return this.retrieve(subject, null, pattern, uriPattern, namespace,
					limit);
		} else {
			return list;
		}
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
		if (editorValue.toString().isEmpty()) {
			return new IdentityCommand("Remove statement.");
		}

		final URI[] name = { null };
		boolean createNew = false;
		ConstructorParser parser = createConstructorParser();
		ParsingResult<Object> ctor = new BasicParseRunner<Object>(
				parser.Constructor()).run((String) editorValue);
		if (ctor.matched) {
			createNew = true;
			IndexRange range = (IndexRange) ctor.resultValue;
			editorValue = ((String) editorValue).substring(range.start,
					range.end);
			// check if a name for the new resource is given
			if (ctor.valueStack.size() > 1) {
				if (subject instanceof IObject) {
					name[0] = toURI(((IObject) subject).getModel(),
							ctor.valueStack.peek(1));
				}
			}
		} else {
			// allow to specify resources as full IRIs in the form
			// <http://example.org#resource>
			ParsingResult<Object> Iri = new BasicParseRunner<Object>(
					parser.IRI_REF()).run((String) editorValue);
			if (Iri.matched) {
				name[0] = toURI(((IObject) subject).getModel(), Iri.resultValue);
				return new SimpleCommand() {
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						return CommandResult.newOKCommandResult(subject
								.getEntityManager().find(name[0]));
					}
				};
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
								.getEntityManager().createNamed(name[0],
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
