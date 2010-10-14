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
package net.enilink.komma.internal.sesame.behaviours;

import net.enilink.composition.annotations.Iri;
import org.openrdf.model.URI;

import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.concepts.CONCEPTS;

@Iri(CONCEPTS.NAMESPACE + "LiteralKeyValueMap")
public abstract class LiteralKeyValueMap extends AbstractRDFMap {
	@Override
	protected URI getUri4Key() {
		return URIUtil.toSesameUri(CONCEPTS.PROPERTY_KEYDATA);
	}

	@Override
	protected URI getUri4Value() {
		return URIUtil.toSesameUri(CONCEPTS.PROPERTY_VALUEDATA);
	}
}
