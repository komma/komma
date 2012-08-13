package net.enilink.komma.dm;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IValue;

public interface IDataManagerQuery<R> {
	/**
	 * Evaluates the query and returns the result.
	 * 
	 * @return The result of the query.
	 */
	IExtendedIterator<R> evaluate();

	/**
	 * Skips to the <code>startPosition</code> of the results.
	 * 
	 * @param startPosition
	 * 
	 * @return This query object.
	 */
	IDataManagerQuery<R> setFirstResult(int startPosition);

	/**
	 * Terminates the result list after reading <code>maxResult</code>
	 * 
	 * @param maxResult
	 * 
	 * @return This query object.
	 */
	IDataManagerQuery<R> setMaxResults(int maxResult);

	/**
	 * Assigns an entity or literal to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param value
	 *            managed entity or literal.
	 */
	IDataManagerQuery<R> setParameter(String name, IValue value);

	/**
	 * Returns <code>true</code> if the methods
	 * {@link IDataManagerQuery#setFirstResult(int)} and
	 * {@link IDataManagerQuery#setMaxResults(int)} are supported.
	 * 
	 * @return <code>true</code> if setting limits is supported, else
	 *         <code>false</code>
	 */
	boolean supportsLimit();
}
