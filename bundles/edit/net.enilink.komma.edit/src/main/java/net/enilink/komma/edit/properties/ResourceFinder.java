package net.enilink.komma.edit.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.enilink.commons.iterator.IMap;
import net.enilink.komma.core.Bindings;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.QueryFragment;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.model.IObject;
import net.enilink.vocab.rdf.RDF;

public class ResourceFinder {
	private static Pattern ESCAPE_CHARS = Pattern.compile("[\\[.{(*+?^$|]");

	public static class Options {
		final IEntityManager em;
		final IEntity subject;
		final IReference predicate;
		final IReference type;
		final String pattern;
		final int limit;

		Options(IEntityManager em, IEntity subject, IReference predicate,
				IReference type, String pattern, int limit) {
			this.em = em;
			this.subject = subject;
			this.predicate = predicate;
			this.type = type;
			this.pattern = pattern.trim();
			this.limit = limit;
		}

		public static Options create(IEntityManager em, IReference type,
				String pattern, int limit) {
			return new Options(em, null, null, type, pattern, limit);
		}

		public static Options create(IEntity subject, String pattern, int limit) {
			return new Options(subject.getEntityManager(), subject, null, null,
					pattern, limit);
		}

		public Options forPredicate(IReference predicate) {
			return new Options(em, subject, predicate, type, pattern, limit);
		}

		public Options ofType(IReference type) {
			return new Options(em, subject, predicate, type, pattern, limit);
		}

		public Options anyType() {
			return new Options(em, subject, null, null, pattern, limit);
		}
	}

	public static class Match {
		public final IEntity resource;
		public final boolean inGraph;
		public final boolean matchesRange;

		public Match(IEntity resource, boolean inGraph, boolean matchesRange) {
			this.resource = resource;
			this.inGraph = inGraph;
			this.matchesRange = matchesRange;
		}

		public int score() {
			return 0 + (matchesRange ? 1000 : 0) + (inGraph ? 1 : 0);
		}
	}

	public Iterable<Match> findAnyResources(Options options) {
		Map<IEntity, Match> matches = new LinkedHashMap<>(options.limit);
		Iterator<Match> restricted = findRestrictedResources(options)
				.iterator();
		while (restricted.hasNext() && matches.size() < options.limit) {
			Match match = restricted.next();
			matches.put(match.resource, match);
		}
		if (options.predicate != null && matches.size() < options.limit) {
			// find resource without consideration of ranges
			Iterator<Match> fallback = findRestrictedResources(
					options.anyType()).iterator();
			while (fallback.hasNext() && matches.size() < options.limit) {
				Match match = fallback.next();
				if (!matches.containsKey(match.resource)) {
					matches.put(match.resource, match);
				}
			}
		}
		return matches.values();
	}

	public Iterable<Match> findRestrictedResources(Options options) {
		Map<IReference, Match> matches = new LinkedHashMap<>(options.limit);
		IEntity subject = options.subject;
		String pattern = options.pattern;
		IReference predicate = options.predicate;
		IReference type = options.type;
		if (!pattern.contains(":") && subject != null) {
			// find resources within the current model first
			for (Match match : retrieve(options.em, subject, predicate, type,
					pattern, toUriRegex(pattern), null, ((IObject) subject)
							.getModel().getURI(), options.limit)) {
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
					URI namespaceUri = options.em.getNamespace(prefix);
					if (namespaceUri != null) {
						uriNamespace = namespaceUri.toString();
						pattern = pattern.substring(colonIndex + 1);
					}
				}
				uriPattern = toUriRegex(pattern);
			}
			Iterator<Match> fallback = retrieve(options.em, subject, predicate,
					type, pattern, uriPattern, uriNamespace, null,
					options.limit).iterator();
			while (fallback.hasNext() && matches.size() < options.limit) {
				Match match = fallback.next();
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

	protected List<Match> retrieve(IEntityManager em, IEntity subject,
			final IReference predicate, IReference type, String pattern,
			String uriPattern, String namespace, final URI graph, int limit) {
		// System.out.println("retrieve("
		// + Arrays.asList(predicate, type, pattern, uriPattern,
		// namespace, graph, limit) + ")");
		StringBuilder sparql = new StringBuilder(ISparqlConstants.PREFIX
				+ "SELECT DISTINCT ?s ");

		boolean findPredicate = predicate == null
				&& RDF.TYPE_PROPERTY.equals(type);
		if (findPredicate) {
			sparql.append("?prio WHERE { ");
			sparql.append("?s a [rdfs:subClassOf* rdf:Property] . ");
			sparql.append("optional { ");
			sparql.append("filter bound(?subject) . ");
			sparql.append("{ ?subject a [ rdfs:subClassOf [ owl:onProperty ?s ] ] } ");
			sparql.append("union { ?subject a ?subjectType . ?s rdfs:domain ?subjectType filter (?subjectType != owl:Thing && ?subjectType != rdfs:Resource) } ");
			sparql.append("bind ( 1 as ?prio) }");
		} else {
			sparql.append("WHERE { ");
			if (type != null || predicate != null) {
				sparql.append("?s a ?sType .");
			}
			if (predicate != null) {
				sparql.append("{ ?property rdfs:range ?sType FILTER (?sType != owl:Thing) }");
				sparql.append(" UNION ");
				sparql.append("{ ?subject a [rdfs:subClassOf ?r] . ?r owl:onProperty ?property { ?r owl:allValuesFrom ?sType } UNION { ?r owl:someValuesFrom ?sType }}");
			}
		}

		if (graph != null) {
			sparql.append(" graph ?graph {\n");
		}

		QueryFragment searchS = null, searchL = null;
		if (pattern.trim().isEmpty()) {
			sparql.append("?s ?p ?o . ");
		} else {
			IDialect dialect = em.getFactory().getDialect();
			searchS = dialect.fullTextSearch(Arrays.asList("s"), IDialect.ALL,
					split(pattern, "\\s*[#/]\\s*"));
			searchL = dialect.fullTextSearch(Arrays.asList("l"),
					IDialect.DEFAULT, pattern);

			sparql.append("{ ?s ?p ?o . " + searchS);
			sparql.append(" FILTER regex(str(?s), ?uriPattern)");
			sparql.append(" }");
			sparql.append(" UNION ");
			sparql.append("{ ");
			sparql.append("?s rdfs:label ?l . " + searchL);
			// required, since FTS on labels has wrong results using OWLIM
			sparql.append(" FILTER regex(str(?l), ?pattern)");
			sparql.append(" }");
		}
		if (namespace != null) {
			sparql.append(" FILTER regex(str(?s), ?namespace)");
		}
		if (graph != null) {
			sparql.append("\n}");
		}
		sparql.append(" } LIMIT " + limit);
		// System.out.println(sparql);

		// TODO incorporate correct ranges
		IQuery<?> query = em.createQuery(sparql.toString());
		if (searchS != null) {
			searchS.addParameters(query);
		}
		if (searchL != null) {
			searchL.addParameters(query);
		}
		query.setParameter("uriPattern", uriPattern);
		query.setParameter("pattern", "^" + toRegex(pattern));
		if (namespace != null) {
			query.setParameter("namespace", "^" + namespace);
		}
		if (graph != null) {
			query.setParameter("graph", graph);
		}
		query.setParameter("subject", subject);
		if (!findPredicate) {
			if (predicate != null) {
				query.setParameter("property", predicate);
			}
			if (type != null) {
				query.setParameter("sType", type);
			}
			return query.evaluate(IEntity.class)
					.mapWith(new IMap<IEntity, Match>() {
						@Override
						public Match map(IEntity value) {
							// System.out.println(" > " + value);
							return new Match(value, graph != null,
									predicate != null);
						}
					}).toList();
		} else {
			// includes prio of matched predicates
			query.bindResultType("s", IEntity.class);
			return query.evaluate(Bindings.typed())
					.mapWith(new IMap<IBindings<Object>, Match>() {
						@Override
						public Match map(IBindings<Object> bindings) {
							IEntity resource = (IEntity) bindings.get("s");
							Number prio = (Number) bindings.get("prio");
							return new Match(resource, graph != null,
									prio != null && prio.intValue() > 0);
						}
					}).toList();
		}
	}

	protected String toRegex(String pattern) {
		return ESCAPE_CHARS.matcher(pattern).replaceAll("\\\\$0")
				.replace("\\*", ".*").replace("\\?", ".");
	}

	protected String toUriRegex(String pattern) {
		return "[#/:]" + toRegex(pattern) + "[^#/]*$";
	}

}
