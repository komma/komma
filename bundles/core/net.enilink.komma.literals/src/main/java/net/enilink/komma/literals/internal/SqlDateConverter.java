/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
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

import java.sql.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.inject.Inject;

import net.enilink.komma.core.IConverter;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

/**
 * Converts {@link Date} to and from {@link ILiteral}.
 * 
 */
public class SqlDateConverter implements IConverter<Date> {
	private static final URI DATATYPE = URIs.createURI("java:"
			+ Date.class.getName());

	@Inject
	private ILiteralFactory lf;

	@Inject
	private DatatypeFactory factory;

	private URI datatype = DATATYPE;

	public String getJavaClassName() {
		return Date.class.getName();
	}

	public URI getDatatype() {
		return datatype;
	}

	public void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	public Date deserialize(String label) {
		XMLGregorianCalendar gc = factory.newXMLGregorianCalendar(label);
		return new Date(gc.toGregorianCalendar().getTimeInMillis());
	}

	public ILiteral serialize(Date object) {
		GregorianCalendar gc = new GregorianCalendar(0, 0, 0);
		gc.setTime(object);
		String label = factory.newXMLGregorianCalendar(gc).toXMLFormat();
		return lf.createLiteral(label, datatype, null);
	}
}
