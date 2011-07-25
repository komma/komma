package net.enilink.komma.edit.provider;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
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
		return "?parent komma:hasDescendant ?s";
	}

	protected void setQueryParameters(IQuery<?> query, Object parent) {
		query.setParameter("parent", parent);
	}

	@Override
	public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
		IEntityManager em = getEntityManager(parent);
		if (expression instanceof String && em != null) {
			String findPatterns = getQueryFindPatterns(parent);
			String queryStr = ISparqlConstants.PREFIX
					+ "SELECT DISTINCT ?s WHERE {{"
					+ findPatterns
					+ " FILTER regex(str(?s), ?uriPattern)}" + " UNION {"
					+ findPatterns 
					+ " { ?s rdfs:label ?l } FILTER regex(str(?l), ?template)}"
					+ "}";

			if (limit > 0) {
				queryStr += " LIMIT " + limit;
			}

			String pattern = (String) expression;
			String uriPattern = pattern;
			if (!pattern.matches("[#/]")) {
				uriPattern = "#" + pattern;

				int colonIndex = pattern.lastIndexOf(':');
				if (colonIndex == 0) {
					uriPattern = "#" + pattern.substring(1);
				} else if (colonIndex > 0) {
					String prefix = pattern.substring(0, colonIndex);
					URI namespaceUri = em.getNamespace(prefix);
					if (namespaceUri != null) {
						uriPattern = namespaceUri.appendFragment(
								pattern.substring(colonIndex + 1)).toString();
					}
				}
			}
			IQuery<?> query = em.createQuery(queryStr);
			setQueryParameters(query, parent);
			return query.setParameter("uriPattern", uriPattern) //
					.setParameter("template", "^" + pattern) //
					.evaluate();
		}
		return WrappedIterator.emptyIterator();
	}
}
