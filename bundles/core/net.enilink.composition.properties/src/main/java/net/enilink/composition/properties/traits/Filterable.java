package net.enilink.composition.properties.traits;

import java.util.Iterator;

/**
 * Interface for collections which may be filtered using a string pattern.
 */
public interface Filterable<E> {
	/**
	 * Returns a filtered view on the collection constrained by
	 * <code>pattern</code> and an upper <code>limit</code> on the number of
	 * results.
	 * 
	 * @param pattern
	 *            The pattern used for filtering.
	 * @param limit
	 *            The limit on the number of results.
	 * 
	 * @return Iterator with matching elements.
	 */
	Iterator<E> filter(String pattern, int limit);

	/**
	 * Returns a filtered view on the collection constrained by
	 * <code>pattern</code>.
	 * 
	 * @param pattern
	 *            The pattern used for filtering.
	 * 
	 * @return Iterator with matching elements.
	 */
	Iterator<E> filter(String pattern);
}
