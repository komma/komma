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
package net.enilink.composition;

import java.util.Collection;

/**
 * Interface for a factory that creates composite objects.
 * 
 * @param <T>
 *            The identifier type.
 */
public interface ObjectFactory<T> {
	/**
	 * Creates an object with no rdf:type.
	 */
	Object createObject();

	/**
	 * Creates an object with an assumed rdf:type.
	 */
	<C> C createObject(Class<C> type);

	/**
	 * Creates an object with an assumed rdf:type.
	 */
	<C> C createObject(Class<C> type, Class<?>... types);

	/**
	 * Creates an object with assumed rdf:types.
	 */
	Object createObject(Collection<T> types);

	/**
	 * Creates an object with assumed rdf:types.
	 */
	Object createObject(T... types);
}