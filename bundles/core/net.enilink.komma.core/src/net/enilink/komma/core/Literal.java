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

public class Literal implements ILiteral {
	protected URI datatype;
	protected String language;
	protected String label;

	/**
	 * Creates a new plain literal with the supplied value.
	 * 
	 * @param value
	 *            The value for the literal, must not be <tt>null</tt>.
	 */
	public Literal(String label) {
		this(label, null, null);
	}

	/**
	 * Creates a new plain literal with the supplied value and language tag.
	 * 
	 * @param value
	 *            The value for the literal, must not be <tt>null</tt>.
	 * @param language
	 *            The language tag for the literal.
	 */
	public Literal(String label, String language) {
		this(label, null, language);
	}

	/**
	 * Creates a new datyped literal with the supplied value and datatype.
	 * 
	 * @param value
	 *            The value for the literal, must not be <tt>null</tt>.
	 * @param datatype
	 *            The datatype for the literal.
	 */
	public Literal(String label, URI datatype) {
		this(label, datatype, null);
	}

	/**
	 * Creates a new Literal object, initializing the variables with the
	 * supplied parameters.
	 */
	protected Literal(String label, URI datatype, String language) {
		assert label != null;

		this.label = label;
		if (language != null) {
			this.language = language.toLowerCase();
		}
		this.datatype = datatype;
	}

	@Override
	public URI getDatatype() {
		return datatype;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public boolean equals(Object obj) {
		return Literals.equals(this, obj);
	}

	@Override
	public int hashCode() {
		return Literals.hashCode(this);
	}

	@Override
	public String toString() {
		return Literals.toString(this);
	}
}
