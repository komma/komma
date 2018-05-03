package net.enilink.komma.dm;

import java.util.Map;
import java.util.Set;

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
	 * Get the properties and associated values that are in effect for the query
	 * instance.
	 * 
	 * @return query properties
	 */
	Map<String, Object> getProperties();

	/**
	 * Get the names of the properties that are supported for query objects.
	 * These include all standard query properties as well as vendor-specific
	 * properties supported by the provider. These properties may or may not
	 * currently be in effect.
	 * 
	 * @return properties
	 */
	Set<String> getSupportedProperties();

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
	 * Set a query property. If a vendor-specific property is not recognized, it
	 * is silently ignored. Depending on the database in use and the locking
	 * mechanisms used by the provider, the property may or may not be observed.
	 * 
	 * @param propertyName
	 * @param value
	 * @return the same query instance
	 * @throws IllegalArgumentException
	 *             if the second argument is not valid for the implementation
	 */
	IDataManagerQuery<R> setProperty(String propertyName, Object value);
}
