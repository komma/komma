/*******************************************************************************
 * Copyright (c) 2021 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.mapping;

import java.lang.reflect.Method;

public class PropertyDescriptor {
	private final String name;
	private final Method readMethod;
	private final Method writeMethod;
	private final String predicate;
	private boolean enforceList = false;
	private final PropertyAttribute[] attributes;

	public PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod,
							  String predicate, PropertyAttribute... attributes) {
		if (propertyName == null || propertyName.isEmpty()) {
			throw new IllegalArgumentException("Property name may not be empty.");
		}
		if (predicate == null || predicate.isEmpty()) {
			throw new IllegalArgumentException("Predicate may not be empty.");
		}
		this.name = propertyName;
		this.readMethod = readMethod;
		this.writeMethod = writeMethod;
		this.predicate = predicate;
		this.attributes = attributes;
	}

	public PropertyAttribute[] getAttributes() {
		return attributes;
	}

	public Class<?> getPropertyType() {
		Class<?> result = null;
		if (readMethod != null) {
			result = readMethod.getReturnType();
		} else if (writeMethod != null) {
			Class<?>[] parameterTypes = writeMethod.getParameterTypes();
			result = parameterTypes[0];
		}
		return result;
	}

	public void setEnforceList(boolean enforceList) {
		this.enforceList = enforceList;
	}

	public boolean isEnforceList() {
		return enforceList;
	}

	public String getName() {
		return name;
	}

	public String getPredicate() {
		return predicate;
	}

	public Method getReadMethod() {
		return readMethod;
	}

	public Method getWriteMethod() {
		return writeMethod;
	}
}
