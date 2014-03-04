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
package net.enilink.komma.em.internal;

import org.apache.commons.codec.binary.Base64;

import com.google.inject.Inject;

import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.literals.IConverter;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;

public class ByteArrayConverter implements IConverter<byte[]> {
	@Inject
	private ILiteralFactory lf;

	@Override
	public byte[] deserialize(String label) {
		return Base64.decodeBase64(label.getBytes());
	}

	@Override
	public URI getDatatype() {
		return XMLSCHEMA.TYPE_BASE64BINARY;
	}

	@Override
	public String getJavaClassName() {
		return byte[].class.getName();
	}

	@Override
	public ILiteral serialize(byte[] data) {
		return lf.createLiteral(new String(Base64.encodeBase64(data)),
				getDatatype(), null);
	}

	@Override
	public void setDatatype(URI datatype) {
		if (!datatype.equals(getDatatype())) {
			throw new IllegalArgumentException(datatype.toString());
		}
	}
}
