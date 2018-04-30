package net.enilink.komma.core;

import java.util.Collection;

/**
 * Represents a dialect of SPARQL functions which are different among triple
 * stores.
 */
public interface IDialect {
	final static int DEFAULT = 0;

	final static int ALL = 1;
	final static int CASE_SENSITIVE = ALL << 1;

	/**
	 * Creates a query fragment to execute a full-text search on binding
	 * <code>bindingNames</code> with the given <code>patterns</code>. Possible
	 * flags are {@link IDialect#ALL} and {@link IDialect#CASE_SENSITIVE}.
	 * 
	 * @param bindingNames
	 *            The names of the bindings which should be matched by full-text
	 *            search.
	 * @param flags
	 *            The flags to control the full-text search.
	 * @param patterns
	 *            The search patterns.
	 * 
	 * @return A query fragment for executing the search.
	 */
	QueryFragment fullTextSearch(Collection<? extends String> bindingNames,
			int flags, String... patterns);
}
