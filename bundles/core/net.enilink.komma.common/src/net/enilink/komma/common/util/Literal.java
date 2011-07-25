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
package net.enilink.komma.common.util;

import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.URI;

public class Literal implements ILiteral {
	protected URI datatype;
	protected String language;
	protected Object value;

	protected Literal() {
	}

	/**
	 * Creates a new plain literal with the supplied value.
	 * 
	 * @param value
	 *            The value for the literal, must not be <tt>null</tt>.
	 */
	public Literal(Object value) {
		this(value, null, null);
	}

	/**
	 * Creates a new plain literal with the supplied value and language tag.
	 * 
	 * @param value
	 *            The value for the literal, must not be <tt>null</tt>.
	 * @param language
	 *            The language tag for the literal.
	 */
	public Literal(Object value, String language) {
		this(value, null, language);
	}

	/**
	 * Creates a new datyped literal with the supplied value and datatype.
	 * 
	 * @param value
	 *            The value for the literal, must not be <tt>null</tt>.
	 * @param datatype
	 *            The datatype for the literal.
	 */
	public Literal(Object value, URI datatype) {
		this(value, datatype, null);
	}

	/**
	 * Creates a new Literal object, initializing the variables with the
	 * supplied parameters.
	 */
	private Literal(Object value, URI datatype, String language) {
		assert value != null;

		setValue(value);
		if (language != null) {
			setLanguage(language.toLowerCase());
		}
		if (datatype != null) {
			setDatatype(datatype);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Literal))
			return false;
		Literal other = (Literal) obj;
		if (datatype == null) {
			if (other.datatype != null)
				return false;
		} else if (!datatype.equals(other.datatype))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public URI getDatatype() {
		return datatype;
	}

	@Override
	public String getLabel() {
		return String.valueOf(value);
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public Object getInstanceValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((datatype == null) ? 0 : datatype.hashCode());
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	public void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Override
	public String toString() {
		String result = "\"" + getLabel() + "\"";
		String suffix = "";
		if (datatype != null) {
			suffix = "^^" + datatype.toString();
		} else if (language != null) {
			suffix = "@" + language;
		}
		return result + suffix;
	}
}
