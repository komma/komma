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

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import com.google.inject.Inject;

import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.core.ILiteralMapper;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;

/**
 * Converts {@link Duration} to and from {@link ILiteral}.
 * 
 */
public class DurationLiteralMapper implements ILiteralMapper<Duration> {
	@Inject
	private ILiteralFactory lf;

	private DatatypeFactory factory;

	private Class<? extends Duration> javaClass;

	private URI datatype = XMLSCHEMA.TYPE_DURATION;

	@Inject
	public DurationLiteralMapper(DatatypeFactory factory) {
		this.factory = factory;
		javaClass = factory.newDuration(0).getClass();
	}
	
	public String getJavaClassName() {
		return javaClass.getName();
	}

	public URI getDatatype() {
		return datatype;
	}

	public void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	public Duration deserialize(String label) {
		return factory.newDuration(label);
	}

	public ILiteral serialize(Duration object) {
		return lf.createLiteral(object.toString(), datatype, null);
	}
}
