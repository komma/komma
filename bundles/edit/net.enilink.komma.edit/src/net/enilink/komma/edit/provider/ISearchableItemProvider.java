package net.enilink.komma.edit.provider;

import net.enilink.commons.iterator.IExtendedIterator;

/**
 * This interface can be implemented if searching and filtering should be
 * supported by an item content provider.
 * 
 */
public interface ISearchableItemProvider {
	/**
	 * Find elements beneath <code>parent</code> according to
	 * <code>filter</code>.
	 * 
	 * @param Expression
	 *            the filter expression
	 * @param parent
	 *            Parent element
	 * @param limit
	 *            Maximum number of results that should be returned
	 * @return Collection of elements matching <code>expression</code>
	 */
	IExtendedIterator<?> find(Object expression, Object parent, int limit);
}
