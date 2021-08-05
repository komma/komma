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

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.Inject;

import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.core.ILiteralMapper;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;

/**
 * Converts {@link Locale} to and from {@link ILiteral}.
 * 
 * @author James Leigh
 * 
 */
public class LocaleLiteralMapper implements ILiteralMapper<Locale> {
	@Inject
	private ILiteralFactory lf;

	private URI datatype = XMLSCHEMA.TYPE_LANGUAGE;

	private ConcurrentMap<String, Locale> locales = new ConcurrentHashMap<String, Locale>();

	public URI getDatatype() {
		return datatype;
	}

	public void setDatatype(URI datatype) {
		this.datatype = datatype;
	}

	public Locale deserialize(ILiteral literal) {
		String lang = literal.getLabel();
		Locale locale = locales.get(lang);
		if (locale == null) {
			String[] l = lang.split("-", 3);
			String language = l.length < 1 ? "" : l[0];
			String country = l.length < 2 ? "" : l[1];
			String variant = l.length < 3 ? "" : l[2];
			locale = new Locale(language, country.toUpperCase(), variant);
			Locale o = locales.putIfAbsent(lang, locale);
			if (o != null) {
				locale = o;
			}
		}
		return locale;
	}

	public ILiteral serialize(Locale object) {
		String label = object.toString().toLowerCase().replace('_', '-');
		return lf.createLiteral(label, datatype, null);
	}
}