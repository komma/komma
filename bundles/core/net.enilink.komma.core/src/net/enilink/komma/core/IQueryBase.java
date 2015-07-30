/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

public interface IQueryBase<Q extends IQueryBase<Q>> {
	/**
	 * Asserts the given <code>resultTypes</code> for the result denoted by
	 * <code>name</code>. This ensures that the result values for
	 * <code>name</code> have at least the <code>resultTypes</code>.
	 * 
	 * @param name
	 *            The name of the result variable
	 * @param resultTypes
	 *            The types for the result variable
	 * @return The query object
	 */
	Q bindResultType(String name, Class<?>... resultTypes);

	/**
	 * Restricts the types of the result values for <code>name</code> to the
	 * given <code>resultTypes</code>. This ensures that no additional queries
	 * are executed to retrieve the actual types of the result values for
	 * <code>name</code>.
	 * 
	 * @param name
	 *            The name of the result variable
	 * @param resultTypes
	 *            The types for the result variable
	 * @return The query object
	 */
	Q restrictResultType(String name, Class<?>... resultTypes);

	/**
	 * Terminates the result list after reading <code>maxResult</code>
	 * 
	 * @param maxResult
	 */
	Q setMaxResults(int maxResult);

	/**
	 * The position of the first result the query object was set to retrieve.
	 * Returns 0 if setFirstResult was not applied to the query object.
	 * 
	 * @return position of first result
	 */
	int getFirstResult();

	/**
	 * The maximum number of results the query object was set to retrieve.
	 * Returns Integer.MAX_VALUE if setMaxResults was not applied to the query
	 * object.
	 * 
	 * @return maximum number of results
	 */
	int getMaxResults();

	/**
	 * Skips to the <code>startPosition</code> of the results.
	 * 
	 * @param startPosition
	 */
	Q setFirstResult(int startPosition);

	/**
	 * Assigns an entity or literal to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param value
	 *            managed entity or literal.
	 */
	Q setParameter(String name, Object value);
}