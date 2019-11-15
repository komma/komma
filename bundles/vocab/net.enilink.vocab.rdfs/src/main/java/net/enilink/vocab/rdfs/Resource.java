/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.vocab.rdfs;

import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Localized;

import net.enilink.komma.core.IEntity;

/** The class resource, everything. */
@Iri("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface Resource extends IEntity {

	/** A description of the subject resource. */
	@Localized
	@Iri("http://www.w3.org/2000/01/rdf-schema#comment")
	public abstract String getRdfsComment();

	/** A description of the subject resource. */
	public abstract void setRdfsComment(String value);

	/** The defininition of the subject resource. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#isDefinedBy")
	public abstract Set<Object> getRdfsIsDefinedBy();

	/** The defininition of the subject resource. */
	public abstract void setRdfsIsDefinedBy(Set<?> value);

	/** A human-readable name for the subject. */
	@Localized
	@Iri("http://www.w3.org/2000/01/rdf-schema#label")
	public abstract String getRdfsLabel();

	/** A human-readable name for the subject. */
	public abstract void setRdfsLabel(String value);

	/** A member of the subject resource. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#member")
	public abstract Set<Object> getRdfsMembers();

	/** A member of the subject resource. */
	public abstract void setRdfsMembers(Set<?> value);

	/** Further information about the subject resource. */
	@Iri("http://www.w3.org/2000/01/rdf-schema#seeAlso")
	public abstract Set<Object> getRdfsSeeAlso();

	/** Further information about the subject resource. */
	public abstract void setRdfsSeeAlso(Set<?> value);

	/** The subject is an instance of a class. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
	public abstract Set<Class> getRdfTypes();

	/** The subject is an instance of a class. */
	public abstract void setRdfTypes(Set<? extends Class> value);

	/** Idiomatic property used for structured values. */
	@Iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#value")
	public abstract Set<Object> getRdfValues();

	/** Idiomatic property used for structured values. */
	public abstract void setRdfValues(Set<?> value);

}
