package net.enilink.komma.core;

import java.util.List;

import net.enilink.commons.iterator.IExtendedIterator;

/**
 * Represents the result of a SPARQL select query or a projected construct
 * query where the results are mapped to beans.
 * 
 * @param <T> The element type, either a bean interface, {@link IBindings} or <code>Object[]</code>
 */
public interface ITupleResult<T> extends IExtendedIterator<T> {
	/**
	 * The list of variable names that have associated values within 
	 * {@link IBindings} or an <code>Object[]</code> array. The 
	 * 
	 * @return The list of variable names with value bindings
	 */
	List<String> getBindingNames();
}