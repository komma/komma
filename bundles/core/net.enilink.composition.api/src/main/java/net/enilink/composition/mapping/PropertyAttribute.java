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

import java.util.Objects;

/**
 * Represents meta information for a mapped RDF property.
 */
public class PropertyAttribute {
	public static final String LOCALIZED = "localized";
	public static final String TYPE = "type";
	public static final String TRANSIENT = "transient";

	private final String name;
	private final String value;

	public PropertyAttribute(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PropertyAttribute that)) return false;
		return Objects.equals(name, that.name) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}
}
