/*
 * Copyright (c) 2008, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.literals.internal;

import static javax.xml.XMLConstants.NULL_NS_URI;

import javax.xml.namespace.QName;

import com.google.inject.Inject;

import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.core.ILiteralMapper;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;

/**
 * Converts {@link QName} to and from {@link ILiteral}.
 * 
 * @author James Leigh
 * 
 */
public class QNameLiteralMapper implements ILiteralMapper<QName> {
	@Inject
	private ILiteralFactory lf;

	public URI getDatatype() {
		return XMLSCHEMA.TYPE_QNAME;
	}

	public void setDatatype(URI datatype) {
		if (!datatype.equals(getDatatype()))
			throw new IllegalArgumentException(datatype.toString());
	}

	public QName deserialize(ILiteral literal) {
		String label = literal.getLabel();
		int idx = label.indexOf(':');
		if (label.charAt(0) == '{' || idx < 0)
			return QName.valueOf(label);
		String prefix = label.substring(0, idx);
		return new QName(NULL_NS_URI, label.substring(idx + 1), prefix);
	}

	public ILiteral serialize(QName object) {
		if (object.getPrefix().length() == 0)
			return lf.createLiteral(object.toString(), getDatatype(),
					null);
		StringBuilder label = new StringBuilder();
		label.append(object.getPrefix());
		label.append(":");
		label.append(object.getLocalPart());
		return lf.createLiteral(label.toString(), getDatatype(), null);
	}

}
