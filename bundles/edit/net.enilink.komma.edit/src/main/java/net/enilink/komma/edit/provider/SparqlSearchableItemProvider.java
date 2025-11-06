package net.enilink.komma.edit.provider;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.QueryFragment;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.util.ISparqlConstants;

public class SparqlSearchableItemProvider implements ISearchableItemProvider {
	private static final Pattern ESCAPE_CHARS = Pattern.compile("[\\[.{(*+?^$|]");

	protected IEntityManager getEntityManager(Object parent) {
		if (parent instanceof IEntity) {
			return ((IEntity) parent).getEntityManager();
		}
		return null;
	}

	protected String getQueryFindPatterns(Object parent) {
		return "?parent komma:child* ?s . ";
	}

	protected void setQueryParameters(IQuery<?> query, Object parent) {
		if (parent != null) {
			query.setParameter("parent", parent);
		}
	}

	protected String patternToRegex(String pattern) {
		pattern = ESCAPE_CHARS.matcher(pattern).replaceAll("\\\\$0");
		return pattern.replace("\\*", ".*").replace("\\?", ".");
	}

	@Override
	public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
		IEntityManager em = getEntityManager(parent);
		if (expression instanceof String pattern && em != null) {
			String findPatterns = getQueryFindPatterns(parent);

			String uriPattern = pattern;
			if (!pattern.matches(".*[#/].*")) {
				int colonIndex = pattern.lastIndexOf(':');
				if (colonIndex == 0) {
					pattern = pattern.substring(1);
				}
				uriPattern = "[#/:]" + patternToRegex(pattern) + "[^#/]*$";
				if (colonIndex > 0) {
					String prefix = pattern.substring(0, colonIndex);
					pattern = pattern.substring(colonIndex + 1);
					URI namespaceUri = em.getNamespace(prefix);
					if (namespaceUri != null) {
						uriPattern = patternToRegex(namespaceUri
								.appendFragment("") + pattern);
					}
				}
			}
			IDialect dialect = em.getFactory().getDialect();
			QueryFragment searchS = dialect.fullTextSearch(List.of("s"),
					IDialect.ALL, pattern);
			QueryFragment searchL = dialect.fullTextSearch(List.of("l"),
					IDialect.DEFAULT, pattern);

			boolean isFilter = Pattern
					.compile("^\\s*filter", Pattern.CASE_INSENSITIVE)
					.matcher(searchS.toString()).find();
			String queryStr = ISparqlConstants.PREFIX
					+ "SELECT DISTINCT ?s WHERE {" //
					// if FTS is implemented with regex filter then
					// add patterns first
					+ (isFilter ? findPatterns : "") //
					+ "{" //
					+ " ?s rdfs:label ?l . "
					+ searchL //
					+ " FILTER regex(str(?l), ?labelPattern, \"i\")" //
					+ "} UNION {" //
					+ (isFilter ? " ?s ?p ?o . " : "") + searchS
					+ " FILTER regex(str(?s), ?uriPattern, \"i\")" //
					+ "} " //
					// if FTS is natively implemented then add
					// patterns last
					+ (isFilter ? "" : findPatterns) //
					+ "}";

			if (limit > 0) {
				queryStr += " LIMIT " + limit;
			}
			
			IQuery<?> query = em.createQuery(queryStr);
			searchS.addParameters(query);
			searchL.addParameters(query);
			query.setParameter("uriPattern", uriPattern);
			query.setParameter("labelPattern", patternToRegex(pattern));
			setQueryParameters(query, parent);
			return query.evaluate();
		}
		return WrappedIterator.emptyIterator();
	}
}
