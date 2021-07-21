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
package net.enilink.composition.properties.behaviours;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import net.enilink.composition.properties.PropertySet;
import net.enilink.composition.properties.PropertySetFactory;
import net.enilink.composition.properties.util.PropertySets;
import net.enilink.composition.properties.util.UnmodifiablePropertySet;

public abstract class Methods {
	public static final Method PROPERTYSET_GET_ALL;
	public static final Method PROPERTYSET_GET_SINGLE;
	public static final Method PROPERTYSET_SET_ALL;
	public static final Method PROPERTYSET_SET_SINGLE;
	public static final Method PROPERTYSET_ADD_ALL;
	public static final Method PROPERTYSET_ADD_SINGLE;
	public static final Method PROPERTYSETFACTORY_CREATEPROPERTYSET;
	public static final Method PROPERTYSETS_UNMODIFIABLE;
	public static final Method UNMODIFIABLEPROPERTYSET_GET_DELEGATE;

	static {
		PROPERTYSET_GET_ALL = getMethod(PropertySet.class, "getAll");
		PROPERTYSET_GET_SINGLE = getMethod(PropertySet.class, "getSingle");
		PROPERTYSET_SET_ALL = getMethod(PropertySet.class, "setAll", Collection.class);
		PROPERTYSET_SET_SINGLE = getMethod(PropertySet.class, "setSingle",
				Object.class);
		PROPERTYSET_ADD_ALL = getMethod(PropertySet.class, "addAll",
				Collection.class);
		PROPERTYSET_ADD_SINGLE = getMethod(PropertySet.class, "add",
				Object.class);
		PROPERTYSETFACTORY_CREATEPROPERTYSET = getMethod(
				PropertySetFactory.class, "createPropertySet", Object.class,
				String.class, Class.class, Annotation[].class);
		PROPERTYSETS_UNMODIFIABLE = getMethod(PropertySets.class,
				"unmodifiable", PropertySet.class);
		UNMODIFIABLEPROPERTYSET_GET_DELEGATE = getMethod(
				UnmodifiablePropertySet.class, "getDelegate");
	}

	private static Method getMethod(Class<?> owner, String name,
			Class<?>... parameterTypes) {
		try {
			return owner.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException nse) {
			throw new RuntimeException("Required method was not found", nse);
		}
	}
}
