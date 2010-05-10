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
package net.enilink.komma.model;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public interface MODELS {
	public static final String NAMESPACE = "http://enilink.net/vocab/komma/models#";
	public static final URI NAMESPACE_URI = URIImpl.createURI(NAMESPACE);

	public static final URI CLASS_DIAGNOSTIC = NAMESPACE_URI
			.appendFragment("Diagnostic");

	public static final URI PROPERTY_MODEL = NAMESPACE_URI
			.appendFragment("model");

	public static final URI PROPERTY_MODIFIED = NAMESPACE_URI
			.appendFragment("modified");

	public static final URI PROPERTY_LOADED = NAMESPACE_URI
			.appendFragment("loaded");

	public static final URI PROPERTY_ERROR = NAMESPACE_URI
			.appendFragment("error");

	public static final URI PROPERTY_WARNING = NAMESPACE_URI
			.appendFragment("warning");

	public static final URI PROPERTY_MODELSET = NAMESPACE_URI
			.appendFragment("modelSet");

	public static final URI CLASS_MODELSET = NAMESPACE_URI
			.appendFragment("ModelSet");

	public static final URI CLASS_MODEL = NAMESPACE_URI.appendFragment("Model");
}