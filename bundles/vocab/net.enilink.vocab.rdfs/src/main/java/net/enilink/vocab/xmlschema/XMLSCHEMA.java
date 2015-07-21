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
package net.enilink.vocab.xmlschema;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

@Iri("http://www.w3.org/2001/XMLSchema#")
public interface XMLSCHEMA {
	public static final String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";
	public static final URI NAMESPACE_URI = URIs.createURI(NAMESPACE);

	public static final URI TYPE_ANYTYPE = NAMESPACE_URI.appendFragment("anyType");

	public static final URI TYPE_ANYSIMPLETYPE = NAMESPACE_URI.appendFragment("anySimpleType");

	public static final URI TYPE_STRING = NAMESPACE_URI.appendFragment("string");

	public static final URI TYPE_NORMALIZEDSTRING = NAMESPACE_URI.appendFragment("normalizedString");

	public static final URI TYPE_TOKEN = NAMESPACE_URI.appendFragment("token");

	public static final URI TYPE_NAME = NAMESPACE_URI.appendFragment("Name");

	public static final URI TYPE_NCNAME = NAMESPACE_URI.appendFragment("NCName");

	public static final URI TYPE_DECIMAL = NAMESPACE_URI.appendFragment("decimal");

	public static final URI TYPE_INTEGER = NAMESPACE_URI.appendFragment("integer");

	public static final URI TYPE_NONPOSITIVEINTEGER = NAMESPACE_URI.appendFragment("nonPositiveInteger");

	public static final URI TYPE_LONG = NAMESPACE_URI.appendFragment("long");

	public static final URI TYPE_NONNEGATIVEINTEGER = NAMESPACE_URI.appendFragment("nonNegativeInteger");

	public static final URI TYPE_INT = NAMESPACE_URI.appendFragment("int");

	public static final URI TYPE_UNSIGNEDLONG = NAMESPACE_URI.appendFragment("unsignedLong");

	public static final URI TYPE_SHORT = NAMESPACE_URI.appendFragment("short");

	public static final URI TYPE_UNSIGNEDINT = NAMESPACE_URI.appendFragment("unsignedInt");

	public static final URI TYPE_UNSIGNEDSHORT = NAMESPACE_URI.appendFragment("unsignedShort");

	public static final URI TYPE_DURATION = NAMESPACE_URI.appendFragment("duration");

	public static final URI TYPE_DATETIME = NAMESPACE_URI.appendFragment("dateTime");

	public static final URI TYPE_TIME = NAMESPACE_URI.appendFragment("time");

	public static final URI TYPE_DATE = NAMESPACE_URI.appendFragment("date");

	public static final URI TYPE_GYEARMONTH = NAMESPACE_URI.appendFragment("gYearMonth");

	public static final URI TYPE_GYEAR = NAMESPACE_URI.appendFragment("gYear");

	public static final URI TYPE_GMONTHDAY = NAMESPACE_URI.appendFragment("gMonthDay");

	public static final URI TYPE_GDAY = NAMESPACE_URI.appendFragment("gDay");

	public static final URI TYPE_GMONTH = NAMESPACE_URI.appendFragment("gMonth");

	public static final URI TYPE_BOOLEAN = NAMESPACE_URI.appendFragment("boolean");

	public static final URI TYPE_BASE64BINARY = NAMESPACE_URI.appendFragment("base64Binary");

	public static final URI TYPE_HEXBINARY = NAMESPACE_URI.appendFragment("hexBinary");

	public static final URI TYPE_FLOAT = NAMESPACE_URI.appendFragment("float");

	public static final URI TYPE_DOUBLE = NAMESPACE_URI.appendFragment("double");

	public static final URI TYPE_ANYURI = NAMESPACE_URI.appendFragment("anyURI");

	public static final URI TYPE_QNAME = NAMESPACE_URI.appendFragment("QName");

	public static final URI TYPE_NOTATION = NAMESPACE_URI.appendFragment("NOTATION");

	public static final URI TYPE_LANGUAGE = NAMESPACE_URI.appendFragment("language");

	public static final URI TYPE_NMTOKEN = NAMESPACE_URI.appendFragment("NMTOKEN");

	public static final URI TYPE_ID = NAMESPACE_URI.appendFragment("ID");

	public static final URI TYPE_IDREF = NAMESPACE_URI.appendFragment("IDREF");

	public static final URI TYPE_ENTITY = NAMESPACE_URI.appendFragment("ENTITY");

	public static final URI TYPE_NEGATIVEINTEGER = NAMESPACE_URI.appendFragment("negativeInteger");

	public static final URI TYPE_POSITIVEINTEGER = NAMESPACE_URI.appendFragment("positiveInteger");

	public static final URI TYPE_BYTE = NAMESPACE_URI.appendFragment("byte");

	public static final URI TYPE_UNSIGNEDBYTE = NAMESPACE_URI.appendFragment("unsignedByte");

}
