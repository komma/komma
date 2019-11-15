/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.vocab.komma;

import net.enilink.komma.core.IEntity;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://www.w3.org/2000/01/rdf-schema#Resource")
public interface KommaResource extends Resource, IEntity {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#contains")
	Set<Object> getKommaContains();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaContains(Set<?> kommaContains);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#containsTransitive")
	Set<Object> getKommaContainsTransitive();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaContainsTransitive(Set<?> kommaContainsTransitive);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#isAbstract")
	Set<Object> getKommaIsAbstract();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaIsAbstract(Set<?> kommaIsAbstract);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#orderedContains")
	Set<Object> getKommaOrderedContains();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaOrderedContains(Set<?> kommaOrderedContains);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#precedes")
	Set<Object> getKommaPrecedes();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaPrecedes(Set<?> kommaPrecedes);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#precedesTransitive")
	Set<Object> getKommaPrecedesTransitive();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaPrecedesTransitive(Set<?> kommaPrecedesTransitive);

}
