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
package net.enilink.komma.generator.support;

import java.util.List;

import net.enilink.vocab.rdf.Property;
import net.enilink.komma.generator.concepts.CodeProperty;
import net.enilink.komma.core.IQuery;

public abstract class CodePropertySupport implements CodeProperty {
	private static final String PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>";
	private static final String EQINV_WHERE_PROP = PREFIX
			+ "SELECT DISTINCT ?eqinv WHERE {\n"
			+ "	{ ?prop owl:inverseOf ?eqinv }\n"
			+ "	UNION { ?prop rdfs:subPropertyOf ?eq }\n"
			+ "	UNION { ?prop owl:equivalentProperty ?eq } {\n"
			+ "		{ ?eq owl:inverseOf ?inv } {\n"
			+ "			{ ?inv rdfs:subPropertyOf ?eqinv }\n"
			+ "			UNION { ?inv owl:equivalentProperty ?eqinv }\n"
			+ "			UNION { ?inv owl:symmetric ?eqinv } } }\n"
			+ "	UNION { ?eq owl:inverseOf ?eqinv }\n"
			+ "	FILTER ( ?prop != ?eqinv && isURI(?eqinv) ) }";

	@SuppressWarnings("unchecked")
	public List<Property> findAllInverseOfProperties() {
		IQuery query = getKommaManager().createQuery(EQINV_WHERE_PROP);
		query.setParameter("prop", this);
		return (List<Property>)query.getResultList();
	}

}
