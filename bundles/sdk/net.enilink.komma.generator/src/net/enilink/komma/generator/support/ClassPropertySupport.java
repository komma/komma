/*
 * Copyright (c) 2008, 2010, Zepheira All rights reserved.
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

import java.math.BigInteger;

import javax.xml.namespace.QName;

import net.enilink.composition.traits.Behaviour;

import net.enilink.vocab.owl.FunctionalProperty;
import net.enilink.vocab.owl.Restriction;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.Class;
import net.enilink.komma.generator.concepts.CodeClass;
import net.enilink.komma.core.IQuery;

public abstract class ClassPropertySupport implements CodeClass,
		Behaviour<CodeClass> {
	private static final String OWL = "http://www.w3.org/2002/07/owl#";
	private static final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	private static final QName NOTHING = new QName(OWL, "Nothing");
	private static final String PREFIX = "PREFIX rdfs:<" + RDFS
			+ ">\nPREFIX owl:<" + OWL + ">\n";
	private static final String WHERE_PROP_DOMAIN_TYPE = PREFIX
			+ "SELECT DISTINCT ?prop WHERE { ?prop rdfs:domain ?type } ORDER BY ?prop";

	@SuppressWarnings("unchecked")
	public Iterable<Property> getDeclaredProperties() {
		IQuery query = getKommaManager().createQuery(
				WHERE_PROP_DOMAIN_TYPE);
		query.setParameter("type", this);
		return (Iterable<Property>) query.getResultList();
	}

	public CodeClass getRange(Property property) {
		CodeClass range = null;
		for (Class c : getRdfsSubClassOf()) {
			if (c instanceof Restriction) {
				Restriction r = (Restriction) c;
				if (property.equals(r.getOwlOnProperty())) {
					Class type = r.getOwlAllValuesFrom();
					if (type != null) {
						range = (CodeClass) type;
					}
				}
			}
		}
		if (range != null) {
			return range;
		}
		for (Class c : getRdfsSubClassOf()) {
			if (c instanceof Restriction || c.equals(getBehaviourDelegate())) {
				continue;
			}
			Class type = ((CodeClass) c).getRange(property);
			if (type != null) {
				range = (CodeClass) type;
			}
		}
		if (range != null) {
			return range;
		}
		for (Class r : property.getRdfsRanges()) {
			range = (CodeClass) r;
		}
		if (range != null) {
			return range;
		}
		for (Property p : property.getRdfsSubPropertyOf()) {
			CodeClass superRange = getRange(p);
			if (superRange != null) {
				range = superRange;
			}
		}
		return range;
	}

	public boolean isFunctional(Property property) {
		if (property instanceof FunctionalProperty)
			return true;
		boolean functional = false;
		BigInteger one = BigInteger.valueOf(1);
		for (Class c : getRdfsSubClassOf()) {
			if (c instanceof Restriction) {
				Restriction r = (Restriction) c;
				if (property.equals(r.getOwlOnProperty())) {
					if (one.equals(r.getOwlMaxCardinality())
							|| one.equals(r.getOwlCardinality())) {
						functional = true;
					}
				}
			}
		}
		if (functional) {
			return functional;
		}
		CodeClass range = getRange(property);
		if (range == null) {
			return false;
		}
		return NOTHING.equals(range.getURI());
	}

}
