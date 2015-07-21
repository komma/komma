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
package net.enilink.komma.model.concepts;

import java.util.Set;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.IEntity;

/**
 * 
 * @generated
 */
@Iri("http://enilink.net/vocab/komma/models#ModelSetContainer")
public interface ModelSetContainer extends IEntity {
	/**
	 * Returns the contained model sets.
	 * 
	 * @generated
	 */
	@Iri("http://enilink.net/vocab/komma/models#containsModelSet")
	Set<ModelSet> getModelSets();

	/**
	 * Sets the contained model sets.
	 * 
	 * @generated
	 */
	void setModelSets(Set<ModelSet> modelSets);
}