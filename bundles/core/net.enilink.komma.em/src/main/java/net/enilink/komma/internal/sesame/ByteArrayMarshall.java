/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.internal.sesame;

import org.apache.commons.codec.binary.Base64;
import net.enilink.composition.properties.sesame.converters.Marshall;
import org.openrdf.model.Literal;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.XMLSchema;

public class ByteArrayMarshall implements Marshall<byte[]> {
	private LiteralFactory lf;

	public ByteArrayMarshall(LiteralFactory lf) {
		this.lf = lf;
	}

	@Override
	public byte[] deserialize(Literal literal) {
		return Base64.decodeBase64(literal.stringValue().getBytes());
	}

	@Override
	public URI getDatatype() {
		return XMLSchema.BASE64BINARY;
	}

	@Override
	public String getJavaClassName() {
		return byte[].class.getName();
	}

	@Override
	public Literal serialize(byte[] data) {
		return lf.createLiteral(new String(Base64.encodeBase64(data)),
				getDatatype());
	}

	@Override
	public void setDatatype(URI datatype) {
		if (!datatype.equals(XMLSchema.BASE64BINARY)) {
			throw new IllegalArgumentException(datatype.toString());
		}
	}
}
