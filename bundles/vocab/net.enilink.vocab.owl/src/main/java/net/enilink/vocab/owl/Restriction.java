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
package net.enilink.vocab.owl;

import java.math.BigInteger;

import net.enilink.composition.annotations.Iri;

import net.enilink.vocab.rdf.List;
import net.enilink.vocab.rdfs.Class;
import net.enilink.komma.core.IValue;

@Iri("http://www.w3.org/2002/07/owl#Restriction")
public interface Restriction extends net.enilink.vocab.owl.Class {

	/** http://www.w3.org/2002/07/owl#allValuesFrom */
	@Iri("http://www.w3.org/2002/07/owl#allValuesFrom")
	public abstract Class getOwlAllValuesFrom();

	/** http://www.w3.org/2002/07/owl#allValuesFrom */
	public abstract void setOwlAllValuesFrom(Class value);

	/** http://www.w3.org/2002/07/owl#qualifiedCardinality */
	@Iri("http://www.w3.org/2002/07/owl#qualifiedCardinality")
	public abstract BigInteger getOwlQualifiedCardinality();

	/** http://www.w3.org/2002/07/owl#qualifiedCardinality */
	public abstract void setOwlQualifiedCardinality(BigInteger value);

	/** http://www.w3.org/2002/07/owl#cardinality */
	@Iri("http://www.w3.org/2002/07/owl#cardinality")
	public abstract BigInteger getOwlCardinality();

	/** http://www.w3.org/2002/07/owl#cardinality */
	public abstract void setOwlCardinality(BigInteger value);

	/** http://www.w3.org/2002/07/owl#hasSelf */
	@Iri("http://www.w3.org/2002/07/owl#hasSelf")
	public abstract Boolean getOwlHasSelf();

	/** http://www.w3.org/2002/07/owl#hasSelf */
	public abstract void setOwlHasSelf(Boolean value);

	/** http://www.w3.org/2002/07/owl#hasValue */
	@Iri("http://www.w3.org/2002/07/owl#hasValue")
	public abstract IValue getOwlHasValue();

	/** http://www.w3.org/2002/07/owl#hasValue */
	public abstract void setOwlHasValue(Object value);

	/** http://www.w3.org/2002/07/owl#maxCardinality */
	@Iri("http://www.w3.org/2002/07/owl#maxCardinality")
	public abstract BigInteger getOwlMaxCardinality();

	/** http://www.w3.org/2002/07/owl#maxCardinality */
	public abstract void setOwlMaxCardinality(BigInteger value);

	/** http://www.w3.org/2002/07/owl#minCardinality */
	@Iri("http://www.w3.org/2002/07/owl#minCardinality")
	public abstract BigInteger getOwlMinCardinality();

	/** http://www.w3.org/2002/07/owl#minCardinality */
	public abstract void setOwlMinCardinality(BigInteger value);

	/** http://www.w3.org/2002/07/owl#maxQualifiedCardinality */
	@Iri("http://www.w3.org/2002/07/owl#maxQualifiedCardinality")
	public abstract BigInteger getOwlMaxQualifiedCardinality();

	/** http://www.w3.org/2002/07/owl#maxQualifiedCardinality */
	public abstract void setOwlMaxQualifiedCardinality(BigInteger value);

	/** http://www.w3.org/2002/07/owl#minQualifiedCardinality */
	@Iri("http://www.w3.org/2002/07/owl#minQualifiedCardinality")
	public abstract BigInteger getOwlMinQualifiedCardinality();

	/** http://www.w3.org/2002/07/owl#minQualifiedCardinality */
	public abstract void setOwlMinQualifiedCardinality(BigInteger value);

	/** http://www.w3.org/2002/07/owl#onProperty */
	@Iri("http://www.w3.org/2002/07/owl#onProperty")
	public abstract OwlProperty getOwlOnProperty();

	/** http://www.w3.org/2002/07/owl#onProperty */
	public abstract void setOwlOnProperty(OwlProperty value);

	/** http://www.w3.org/2002/07/owl#onProperties */
	@Iri("http://www.w3.org/2002/07/owl#onProperties")
	public abstract List<DatatypeProperty> getOwlOnProperties();

	/** http://www.w3.org/2002/07/owl#onProperties */
	public abstract void setOwlOnProperties(
			java.util.List<? extends DatatypeProperty> properties);

	/** http://www.w3.org/2002/07/owl#onClass */
	@Iri("http://www.w3.org/2002/07/owl#onClass")
	public abstract net.enilink.vocab.owl.Class getOwlOnClass();

	/** http://www.w3.org/2002/07/owl#onClass */
	public abstract void setOwlOnClass(
			net.enilink.vocab.owl.Class onClass);

	/** http://www.w3.org/2002/07/owl#onDataRange */
	@Iri("http://www.w3.org/2002/07/owl#onDataRange")
	public abstract DataRange getOwlOnDataRange();

	/** http://www.w3.org/2002/07/owl#onDataRange */
	public abstract void setOwlOnDataRange(DataRange dataRange);

	/** http://www.w3.org/2002/07/owl#someValuesFrom */
	@Iri("http://www.w3.org/2002/07/owl#someValuesFrom")
	public abstract Class getOwlSomeValuesFrom();

	/** http://www.w3.org/2002/07/owl#someValuesFrom */
	public abstract void setOwlSomeValuesFrom(Class value);

}
