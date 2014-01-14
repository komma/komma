package net.enilink.komma.edit.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import net.enilink.komma.edit.assist.ContentProposal;
import net.enilink.komma.edit.assist.IContentProposal;
import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.ISparqlConstants;
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
		boolean perfectMatch;

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

		public ResourceProposal setPerfectMatch(boolean perfectMatch) {
			this.perfectMatch = perfectMatch;
			return this;
		}

		public boolean isPerfectMatch() {
			return perfectMatch;
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

			List<IContentProposal> allProposals = new ArrayList<IContentProposal>();
			Comparator<IContentProposal> comparator = new Comparator<IContentProposal>() {
				@Override
				public int compare(IContentProposal c1, IContentProposal c2) {
					return c1.getLabel().compareTo(c2.getLabel());
				}
			};
			int limit = 20;
			List<IReference> predicates = new ArrayList<IReference>();
			if (!ctor.matched) {
				predicates.add(predicate);
			}
			predicates.add(null);
			Set<IResource> allResources = new HashSet<IResource>();
			ProposalOptions options = ProposalOptions.create(subject,
					contents.substring(0, position), limit);
			// ensures that resources which match the predicate's range are
			// added in front of the result list
			for (IReference p : predicates) {
				if (allProposals.size() >= limit) {
					break;
				}
				List<IResource> resources = new ArrayList<IResource>();
				for (IResource resource : getRestrictedResources(options
						.forPredicate(p))) {
					// globally filter duplicate proposals
					if (allResources.add(resource)) {
						resources.add(resource);
					}
				}
				List<IContentProposal> proposals = toProposals(resources,
						prefix, !ctor.matched, p != null);
				Collections.sort(proposals, comparator);
				allProposals.addAll(proposals);
			}
			return allProposals.toArray(new IContentProposal[allProposals
					.size()]);
		}

		protected List<IContentProposal> toProposals(
				Iterable<IResource> resources, String prefix,
				boolean useAsValue, boolean matchesRange) {
			List<IContentProposal> proposals = new ArrayList<IContentProposal>();
			for (IEntity resource : resources) {
				String content = getLabel(resource);
				if (content.length() > 0) {
					content = prefix + content;
					proposals.add(new ResourceProposal(content, content
							.length(), resource).setUseAsValue(useAsValue)
							.setPerfectMatch(matchesRange));
				}
			}
			return proposals;
		}
	}

	public static class ProposalOptions implements Cloneable {
		IEntity subject;
		IReference predicate;
		IReference type;
		String pattern;
		int limit;

		ProposalOptions(IEntity subject, String pattern, int limit) {
			this.subject = subject;
			this.pattern = pattern.trim();
			this.limit = limit;
		}

		public static ProposalOptions create(IEntity subject, String pattern,
				int limit) {
			return new ProposalOptions(subject, pattern, limit);
		}

		public ProposalOptions forPredicate(IReference predicate) {
			ProposalOptions result = clone();
			result.predicate = predicate;
			return result;
		}

		public ProposalOptions ofType(IReference type) {
			ProposalOptions result = clone();
			result.type = type;
			return result;
		}

		public ProposalOptions anyType() {
			return clone().ofType(null).forPredicate(null);
		}

		@Override
		public ProposalOptions clone() {
			try {
				return (ProposalOptions) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new AssertionError(e);
			}
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
			return "";
		}
		IItemLabelProvider labelProvider = (IItemLabelProvider) adapterFactory
				.adapt(value, IItemLabelProvider.class);
		if (labelProvider != null) {
			return labelProvider.getText(value);
		}
		return value.toString();
	}

	protected String toUriRegex(String pattern) {
		return "[#/:]" + pattern + "[^#/]*$";
	}

	protected Iterable<IResource> getAnyResources(ProposalOptions options) {
		Set<IResource> resources = new LinkedHashSet<IResource>(options.limit);
		Iterator<IResource> restricted = getRestrictedResources(options)
				.iterator();
		while (restricted.hasNext() && resources.size() < options.limit) {
			resources.add(restricted.next());
		}
		if (options.predicate != null && resources.size() < options.limit) {
			// find resource without consideration of ranges
			Iterator<IResource> fallback = getRestrictedResources(
					options.anyType()).iterator();
			while (fallback.hasNext() && resources.size() < options.limit) {
				resources.add(fallback.next());
			}
		}
		return resources;
	}

	protected Iterable<IResource> getRestrictedResources(ProposalOptions options) {
		Set<IResource> resources = new LinkedHashSet<IResource>(options.limit);
		IEntity subject = options.subject;
		String pattern = options.pattern;
		IReference predicate = options.predicate;
		IReference type = options.type;
		if (subject instanceof IObject && !pattern.contains(":")) {
			// find resources within the current model first
			URI graph = ((IObject) subject).getModel().getURI();
			String graphNamespace = graph.appendLocalPart("").toString();
			resources.addAll(retrieve(subject, predicate, type, pattern,
					toUriRegex(pattern), graphNamespace, ((IObject) subject)
							.getModel().getURI(), options.limit));
		}
		if (resources.size() < options.limit) {
			// additionally, if limit not exceeded, find resources from other
			// namespaces
			String uriPattern = pattern;
			String uriNamespace = null;
			if (!pattern.matches(".*[#/].*")) {
				int colonIndex = pattern.lastIndexOf(':');
				if (colonIndex == 0) {
					pattern = pattern.substring(1);
				} else if (colonIndex > 0) {
					String prefix = pattern.substring(0, colonIndex);
					URI namespaceUri = subject.getEntityManager().getNamespace(
							prefix);
					if (namespaceUri != null) {
						uriNamespace = namespaceUri.toString();
						pattern = pattern.substring(colonIndex + 1);
					}
				}
				uriPattern = toUriRegex(pattern);
			}
			Iterator<IResource> fallback = retrieve(subject, predicate, type,
					pattern, uriPattern, uriNamespace, null, options.limit)
					.iterator();
			while (fallback.hasNext() && resources.size() < options.limit) {
				resources.add(fallback.next());
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
			IReference type, String pattern, String uriPattern,
			String namespace, URI graph, int limit) {
		StringBuilder sparql = new StringBuilder(ISparqlConstants.PREFIX
				+ "SELECT DISTINCT ?s WHERE { ");

		if (editPredicate) {
			sparql.append("?s a rdf:Property . ");
		} else {
			if (predicate != null) {
				sparql.append("{");
				sparql.append("{ ?property rdfs:range ?sType FILTER (?sType != owl:Thing) }");
				sparql.append(" UNION ");
				sparql.append("{ ?subject a [rdfs:subClassOf ?r] . ?r owl:onProperty ?property { ?r owl:allValuesFrom ?sType } UNION { ?r owl:someValuesFrom ?sType }}");
				sparql.append(" ?s a ?sType ");
				// allow to edit properties without range information
				sparql.append("} UNION { FILTER (NOT EXISTS { ?property rdfs:range ?sType } && NOT EXISTS { ?subject a [rdfs:subClassOf ?r] . ?r owl:onProperty ?property }) } ");
			}
			if (type != null) {
				sparql.append("?s a ?sType .");
			}
		}

		IDialect dialect = subject.getEntityManager().getFactory().getDialect();
		QueryFragment searchS = dialect.fullTextSearch(Arrays.asList("s"),
				IDialect.ALL, split(pattern, "\\s*[#/]\\s*"));
		QueryFragment searchL = dialect.fullTextSearch(Arrays.asList("l"),
				IDialect.DEFAULT, pattern);

		if (graph != null) {
			sparql.append(" graph ?graph {\n");
		}

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
		if (graph != null) {
			sparql.append("\n}");
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
		if (graph != null) {
			query.setParameter("graph", graph);
		}
		if (!editPredicate) {
			query.setParameter("subject", subject);
			query.setParameter("property", predicate);
			if (type != null) {
				query.setParameter("sType", type);
			}
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
		return value != null ? getLabel(value) : "";
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
			throw new IllegalArgumentException("Unknown prefix for QName "
					+ value);
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
					parser.IRI_REF_WSPACE()).run((String) editorValue);
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
		ProposalOptions options = ProposalOptions.create(subject,
				(String) editorValue + "$", 1);
		Iterator<IResource> resources = getAnyResources(
				options.forPredicate(createNew ? null : property)).iterator();
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
