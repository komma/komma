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

import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IEntity;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/komma/models#Model")
public interface Model extends IEntity {
	/** 
	 * A set of the errors in a model.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#error")
	Set<IModel.IDiagnostic> getModelErrors();
	/** 
	 * A set of the errors in a model.
	 * @generated 
	 */
	void setModelErrors(Set<? extends Diagnostic> errors);

	/**
	 * Indicates whether the model has finished loading.
	 * 
	 * @generated
	 */
	@Iri("http://enilink.net/vocab/komma/models#loaded")
	boolean isModelLoaded();
	/** 
	 * Indicates whether the model has finished loading.
	 * @generated 
	 */
	void setModelLoaded(boolean loaded);

	/** 
	 * Indicates whether the model is currently being loaded.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#loading")
	boolean isModelLoading();
	/** 
	 * Indicates whether the model is currently being loaded.
	 * @generated 
	 */
	void setModelLoading(boolean loading);

	/** 
	 * Returns whether a model has been modified.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#modified")
	boolean isModelModified();
	/** 
	 * Returns whether a model has been modified.
	 * @generated 
	 */
	void setModelModified(boolean modified);

	/** 
	 * Returns whether a model or model set is persistent.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#persistent")
	boolean isModelPersistent();
	/** 
	 * Returns whether a model or model set is persistent.
	 * @generated 
	 */
	void setModelPersistent(boolean persistent);

	/** 
	 * A set of the warnings in a model.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#warning")
	Set<IModel.IDiagnostic> getModelWarnings();
	/** 
	 * A set of the warnings in a model.
	 * @generated 
	 */
	void setModelWarnings(Set<? extends Diagnostic> warnings);
	
	/**
	 * Namespace mappings for this model.
	 */
	@Iri("http://enilink.net/vocab/komma/models#namespace")
	Set<Namespace> getModelNamespaces();
	
	/**
	 * Namespace mappings for this model.
	 */
	void setModelNamespaces(Set<Namespace> namespaces);
}
