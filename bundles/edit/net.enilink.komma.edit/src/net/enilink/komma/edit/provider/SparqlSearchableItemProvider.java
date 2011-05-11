package net.enilink.komma.edit.provider;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.ISparqlConstants;

public class SparqlSearchableItemProvider implements ISearchableItemProvider {
	protected String getSparqlFindPatterns(Object parent) {
		return "?parent komma:hasDescendant ?s";
	}

	@Override
	public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
		if (expression instanceof String && parent instanceof IEntity) {
			String query = ISparqlConstants.PREFIX
					+ "SELECT DISTINCT ?s WHERE {{"
					+ getSparqlFindPatterns(parent)
					+ " FILTER regex(str(?s), ?uriPattern)}" + " UNION "
					+ "{?s rdfs:label ?l . FILTER regex(str(?l), ?template)}"
					+ "}";

			if (limit > 0) {
				query += " LIMIT " + limit;
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
					URI namespaceUri = ((IEntity) parent).getEntityManager()
							.getNamespace(prefix);
					if (namespaceUri != null) {
						uriPattern = namespaceUri.appendFragment(
								pattern.substring(colonIndex + 1)).toString();
					}
				}
			}
			return ((IEntity) parent).getEntityManager().createQuery(query)
					.setParameter("uriPattern", uriPattern) //
					.setParameter("template", "^" + pattern) //
					.evaluate(IResource.class);
		}
		return WrappedIterator.emptyIterator();
	}
}
