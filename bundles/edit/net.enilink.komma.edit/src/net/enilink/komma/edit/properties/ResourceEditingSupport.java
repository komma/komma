package net.enilink.komma.edit.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.Bindings;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.QueryFragment;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.assist.ContentProposal;
import net.enilink.komma.edit.assist.IContentProposal;
import net.enilink.komma.edit.assist.IContentProposalProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.parser.BaseRdfParser;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.QName;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.IndexRange;
import org.parboiled.support.ParsingResult;

public class ResourceEditingSupport implements IPropertyEditingSupport {
	private static Pattern ESCAPE_CHARS = Pattern.compile("[\\[.{(*+?^$|]");

	static class ConstructorParser extends BaseRdfParser {
		public Rule Constructor() {
			return Sequence(
					FirstOf(Sequence(FirstOf(IriRef(), PN_LOCAL()), Ch('a')),
							Ch('a')), WS_NO_COMMENT(),
					FirstOf(IriRef(), PN_LOCAL(), Sequence(EMPTY, push(""))),
					drop(), push(matchRange()));
		}
	}

	public static class ResourceMatch {
		public final IEntity resource;
		public final boolean perfectMatch;

		public ResourceMatch(IEntity resource) {
			this(resource, false);
		}

		public ResourceMatch(IEntity resource, boolean perfectMatch) {
			this.resource = resource;
			this.perfectMatch = perfectMatch;
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

			int limit = 20;
			Set<IReference> predicates = new LinkedHashSet<>();
			if (!ctor.matched) {
				predicates.add(predicate);
			}
			predicates.add(null);
			Map<IEntity, ResourceMatch> allMatches = new LinkedHashMap<>();
			ProposalOptions options = ProposalOptions.create(subject,
					contents.substring(0, position), limit);
			// ensures that resources which match the predicate's range are
			// added in front of the result list
			for (IReference p : predicates) {
				if (allMatches.size() >= limit) {
					break;
				}
				for (ResourceMatch match : getRestrictedResources(options
						.forPredicate(p))) {
					// globally filter duplicate proposals
					ResourceMatch existing = allMatches.get(match.resource);
					if (existing == null || !existing.perfectMatch
							&& match.perfectMatch) {
						allMatches.put(match.resource, match);
					}
				}
			}
			List<ResourceProposal> resourceProposals = toProposals(
					allMatches.values(), prefix, !ctor.matched);
			Comparator<ResourceProposal> comparator = new Comparator<ResourceProposal>() {
				@Override
				public int compare(ResourceProposal p1, ResourceProposal p2) {
					if (p1.perfectMatch) {
						if (!p2.perfectMatch) {
							return -1;
						}
					} else if (p2.perfectMatch) {
						if (!p1.perfectMatch) {
							return 1;
						}
					}
					return p1.getLabel().compareTo(p2.getLabel());
				}
			};
			Collections.sort(resourceProposals, comparator);
			Collection<? extends IContentProposal> results = resourceProposals;
			if (resourceProposals.size() < limit && !contents.contains(":")) {
				final String contentsFinal = contents;
				List<IContentProposal> mixedProposals = new ArrayList<IContentProposal>(
						resourceProposals);
				try (IExtendedIterator<INamespace> it = subject
						.getEntityManager().getNamespaces()
						.filterKeep(new Filter<INamespace>() {
							public boolean accept(INamespace ns) {
								return ns.getPrefix().startsWith(contentsFinal);
							}
						})) {
					while (it.hasNext() && mixedProposals.size() < limit) {
						INamespace ns = it.next();
						mixedProposals.add(new ContentProposal(ns.getPrefix()
								+ ":", ns.getPrefix() + ":", ns.getURI()
								.toString()));
					}
				}
				results = mixedProposals;
			}
			return results.toArray(new IContentProposal[results.size()]);
		}

		protected List<ResourceProposal> toProposals(
				Iterable<ResourceMatch> matches, String prefix,
				boolean useAsValue) {
			List<ResourceProposal> proposals = new ArrayList<>();
			for (ResourceMatch match : matches) {
				String content = getLabel(match.resource);
				if (content.length() > 0) {
					content = prefix + content;
					proposals.add(new ResourceProposal(content, content
							.length(), match.resource)
							.setUseAsValue(useAsValue).setPerfectMatch(
									match.perfectMatch));
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
		pattern = ESCAPE_CHARS.matcher(pattern).replaceAll("\\\\$0");
		pattern = pattern.replace("\\*", ".*").replace("\\?", ".");
		return "[#/:]" + pattern + "[^#/]*$";
	}

	protected Iterable<ResourceMatch> getAnyResources(ProposalOptions options) {
		Map<IEntity, ResourceMatch> matches = new LinkedHashMap<>(options.limit);
		Iterator<ResourceMatch> restricted = getRestrictedResources(options)
				.iterator();
		while (restricted.hasNext() && matches.size() < options.limit) {
			ResourceMatch match = restricted.next();
			matches.put(match.resource, match);
		}
		if (options.predicate != null && matches.size() < options.limit) {
			// find resource without consideration of ranges
			Iterator<ResourceMatch> fallback = getRestrictedResources(
					options.anyType()).iterator();
			while (fallback.hasNext() && matches.size() < options.limit) {
				ResourceMatch match = fallback.next();
				if (!matches.containsKey(match.resource)) {
					matches.put(match.resource, match);
				}
			}
		}
		return matches.values();
	}

	protected Iterable<ResourceMatch> getRestrictedResources(
			ProposalOptions options) {
		Map<IReference, ResourceMatch> matches = new LinkedHashMap<>(
				options.limit);
		IEntity subject = options.subject;
		String pattern = options.pattern;
		IReference predicate = options.predicate;
		IReference type = options.type;
		if (!pattern.contains(":")) {
			// find resources within the current model first
			String graphNamespace = subject.getEntityManager().getNamespace("")
					.toString();
			for (ResourceMatch match : retrieve(subject, predicate, type,
					pattern, toUriRegex(pattern), graphNamespace,
					((IObject) subject).getModel().getURI(), options.limit)) {
				matches.put(match.resource, match);
			}
		}
		if (matches.size() < options.limit) {
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
			Iterator<ResourceMatch> fallback = retrieve(subject, predicate,
					type, pattern, uriPattern, uriNamespace, null,
					options.limit).iterator();
			while (fallback.hasNext() && matches.size() < options.limit) {
				ResourceMatch match = fallback.next();
				if (!matches.containsKey(match.resource)) {
					matches.put(match.resource, match);
				}
			}
		}

		return matches.values();
	}

	private String[] split(String s, String pattern) {
		List<String> tokens = new ArrayList<String>(Arrays.asList(s
				.split(pattern)));
		while (tokens.remove(""))
			;
		return tokens.toArray(new String[tokens.size()]);
	}

	protected List<ResourceMatch> retrieve(IEntity subject,
			final IReference predicate, IReference type, String pattern,
			String uriPattern, String namespace, URI graph, int limit) {
		StringBuilder sparql = new StringBuilder(ISparqlConstants.PREFIX
				+ "SELECT DISTINCT ?s ");

		if (editPredicate) {
			sparql.append("?prio WHERE { ");
			sparql.append("?s a [rdfs:subClassOf* rdf:Property] . ");
			sparql.append("optional { ");
			sparql.append("filter bound(?subject) . ");
			sparql.append("{ ?subject a [ rdfs:subClassOf [ owl:onProperty ?s ] ] } ");
			sparql.append("union { ?subject a ?subjectType . ?s rdfs:domain ?subjectType filter (?subjectType != owl:Thing && ?subjectType != rdfs:Resource) } ");
			sparql.append("bind ( 1 as ?prio) }");
		} else {
			sparql.append("WHERE { ");
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
		query.setParameter("subject", subject);
		if (!editPredicate) {
			query.setParameter("property", predicate);
			if (type != null) {
				query.setParameter("sType", type);
			}
			return query.evaluate(IEntity.class)
					.mapWith(new IMap<IEntity, ResourceMatch>() {
						@Override
						public ResourceMatch map(IEntity value) {
							return new ResourceMatch(value, predicate != null);
						}
					}).toList();
		} else {
			// includes prio of matched predicates
			query.bindResultType("s", IEntity.class);
			return query.evaluate(Bindings.typed())
					.mapWith(new IMap<IBindings<Object>, ResourceMatch>() {
						@Override
						public ResourceMatch map(IBindings<Object> bindings) {
							IEntity resource = (IEntity) bindings.get("s");
							Number prio = (Number) bindings.get("prio");
							return new ResourceMatch(resource, prio != null
									&& prio.intValue() > 0);
						}
					}).toList();
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
		return value != null ? getLabel(value) : "";
	}

	protected URI toURI(IModel model, Object value) {
		if (value instanceof IriRef) {
			URI uri = URIs.createURI(((IriRef) value).getIri());
			if (uri.isRelative()) {
				URI ns = model.getManager().getNamespace("");
				if (ns != null) {
					if (ns.fragment() != null) {
						uri = ns.appendLocalPart(uri.toString());
					} else {
						uri = uri.resolve(ns);
					}
				} else {
					throw new IllegalArgumentException(
							"Relative IRIs are not supported.");
				}
			}
			return uri;
		} else if (value instanceof QName) {
			String prefix = ((QName) value).getPrefix();
			String localPart = ((QName) value).getLocalPart();
			URI ns;
			if (prefix == null || prefix.trim().length() == 0) {
				prefix = "";
			}
			ns = model.getManager().getNamespace(prefix);
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
				(String) editorValue, 1);
		Iterator<ResourceMatch> matches = getAnyResources(
				options.forPredicate(createNew ? null : property)).iterator();
		if (matches.hasNext()) {
			final IEntity resource = matches.next().resource;
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
