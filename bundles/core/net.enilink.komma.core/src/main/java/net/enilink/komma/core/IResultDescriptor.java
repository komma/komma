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

public interface IResultDescriptor<R> extends IQueryBase<IResultDescriptor<R>> {
	<T> IResultDescriptor<T> bindResultType(Class<T> resultType,
			Class<?>... resultTypes);

	String getParameterVariable();

	String getProperty();

	String toQueryString();

	String getResultVariable();

	IResultDescriptor<R> setIncludeInferred(boolean includeInferred);

	boolean getIncludeInferred();

	/**
	 * Initializes a property of the result objects.
	 * 
	 * @param descriptor
	 *            An descriptor for the property's values.
	 * 
	 * @return this
	 */
	IResultDescriptor<R> prefetch(IResultDescriptor<?> descriptor);

	/**
	 * Initializes a property of the result objects.
	 * 
	 * @param property
	 *            A property name which overrides the default property name of
	 *            <code>propertyIterator</code>.
	 * @param descriptor
	 *            An descriptor for the property's values.
	 * 
	 * @return this
	 */
	IResultDescriptor<R> prefetch(String property,
			IResultDescriptor<?> descriptor);

	<T> IResultDescriptor<T> restrictResultType(Class<T> resultType,
			Class<?>... resultTypes);
}
