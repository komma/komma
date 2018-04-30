package net.enilink.komma.common.util;

/**
 * ICollector is a type that allows for the incremental update of a collection
 * of objects. This used for updating views incrementally.
 */
public interface ICollector<T> {
	/**
	 * Add the element to the ICollector.
	 * 
	 * @param element
	 *            The element being added
	 */
	void add(T element);

	/**
	 * Add the elements to the ICollector.
	 * 
	 * @param elements
	 *            The elements being added
	 */
	void add(Iterable<T> elements);

	/**
	 * Returns <code>true</code> if this collector is canceled.
	 * 
	 * @return <code>true</code> if collector is canceled, else
	 *         <code>false</code>.
	 */
	boolean cancelled();
}
