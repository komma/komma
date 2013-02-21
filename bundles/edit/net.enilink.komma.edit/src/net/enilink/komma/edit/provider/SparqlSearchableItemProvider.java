package net.enilink.komma.edit.provider;

import java.util.Arrays;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.QueryFragment;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.ISparqlConstants;

public class SparqlSearchableItemProvider implements ISearchableItemProvider {
	protected IEntityManager getEntityManager(Object parent) {
		if (parent instanceof IEntity) {
			return ((IEntity) parent).getEntityManager();
		}
		return null;
	}

	protected String getQueryFindPatterns(Object parent) {
		return "?parent komma:hasDescendant ?s . ";
	}

	protected void setQueryParameters(IQuery<?> query, Object parent) {
		if (parent != null) {
			query.setParameter("parent", parent);
		}
	}

	@Override
	public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
		IEntityManager em = getEntityManager(parent);
		if (expression instanceof String && em != null) {
			String findPatterns = getQueryFindPatterns(parent);

			String pattern = (String) expression;
			String uriPattern = pattern;
			if (!pattern.matches(".*[#/].*")) {
				int colonIndex = pattern.lastIndexOf(':');
				if (colonIndex == 0) {
					pattern = pattern.substring(1);
				}
				uriPattern = "[#/:]" + pattern + "[^#/]*$";
				if (colonIndex > 0) {
					String prefix = pattern.substring(0, colonIndex);
					pattern = pattern.substring(colonIndex + 1);
					URI namespaceUri = em.getNamespace(prefix);
					if (namespaceUri != null) {
						uriPattern = namespaceUri.appendFragment(pattern)
								.toString();
					}
				}
			}
			IDialect dialect = em.getFactory().getDialect();
			QueryFragment searchS = dialect.fullTextSearch(Arrays.asList("s"),
					IDialect.CASE_INSENSITIVE | IDialect.ALL, pattern);
			QueryFragment searchL = dialect.fullTextSearch(Arrays.asList("l"),
					IDialect.CASE_INSENSITIVE, pattern);

			String queryStr = ISparqlConstants.PREFIX
					+ "SELECT DISTINCT ?s WHERE {" //
					+ findPatterns //
					+ "{" //
					+ " ?s rdfs:label ?l . " //
					+ searchL //
					+ " FILTER regex(str(?l), ?labelPattern, \"i\")" //
					+ "} UNION {" //
					+ " ?s ?p ?o . " + searchS
					+ " FILTER regex(str(?s), ?uriPattern, \"i\")" //
					+ "}" //
					+ "}";

			if (limit > 0) {
				queryStr += " LIMIT " + limit;
			}

			IQuery<?> query = em.createQuery(queryStr);
			searchS.addParameters(query);
			searchL.addParameters(query);
			query.setParameter("uriPattern", uriPattern);
			query.setParameter("labelPattern", pattern);
			setQueryParameters(query, parent);
			return query.evaluate();
		}
		return WrappedIterator.emptyIterator();
	}
}
