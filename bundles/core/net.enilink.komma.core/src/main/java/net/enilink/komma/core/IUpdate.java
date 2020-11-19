/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

import java.util.Map;
import java.util.Set;

/**
 * Interface used to execute an update.
 */
public interface IUpdate {
	/**
	 * Executes the update.
	 */
	void execute();

	/**
	 * Get the hints and associated values that are in effect for the update
	 * instance.
	 * 
	 * @return query hints
	 */
	Map<String, Object> getHints();

	/**
	 * Get the names of the hints that are supported for query objects. These
	 * hints correspond to hints that may be passed to the methods of the Query
	 * interface that take hints as arguments or used with the NamedQuery and
	 * NamedNativeQuery annotations. These include all standard query hints as
	 * well as vendor-specific hints supported by the provider. These hints may
	 * or may not currently be in effect.
	 * 
	 * @return hints
	 */
	Set<String> getSupportedHints();

	/**
	 * Set a query hint. If a vendor-specific hint is not recognized, it is
	 * silently ignored. Portable applications should not rely on the standard
	 * timeout hint. Depending on the database in use and the locking mechanisms
	 * used by the provider, the hint may or may not be observed.
	 * 
	 * @param hintName
	 * @param value
	 * @return the same query instance
	 * @throws IllegalArgumentException
	 *             if the second argument is not valid for the implementation
	 */
	IUpdate setHint(String hintName, Object value);

	/**
	 * Assigns an entity or literal to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param value
	 *            managed entity or literal.
	 */
	IUpdate setParameter(String name, Object value);

	/**
	 * Assigns a concept to the given name.
	 * 
	 * @param name
	 *            Name of the variable to bind to.
	 * @param concept
	 *            Registered concept.
	 */
	IUpdate setTypeParameter(String name, Class<?> concept);
}
