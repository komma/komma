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
package net.enilink.komma.em.internal.behaviours;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.core.URI;
import net.enilink.vocab.komma.KOMMA;

@Iri(KOMMA.NAMESPACE + "LiteralKeyValueMap")
public abstract class LiteralKeyValueMap extends AbstractRDFMap {
	@Override
	protected URI getUri4Key() {
		return KOMMA.PROPERTY_KEYDATA;
	}

	@Override
	protected URI getUri4Value() {
		return KOMMA.PROPERTY_VALUEDATA;
	}
}
