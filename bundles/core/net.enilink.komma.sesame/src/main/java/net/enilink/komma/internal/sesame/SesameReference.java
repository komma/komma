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
package net.enilink.komma.internal.sesame;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.URIs;

public class SesameReference implements IReference {
	private Resource resource;

	private net.enilink.komma.core.URI uri;

	public SesameReference(Resource resource) {
		this.resource = resource;
		if (resource instanceof URI) {
			this.uri = URIs.createURI(((URI) resource).stringValue());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (uri != null) {
			return uri.equals(obj);
		}
		if (obj instanceof IReferenceable) {
			obj = ((IReferenceable) obj).getReference();
		}
		return obj instanceof SesameReference
				&& resource.equals(((SesameReference) obj).resource);
	}

	public Resource getSesameResource() {
		return resource;
	}

	@Override
	public net.enilink.komma.core.URI getURI() {
		return uri;
	}

	@Override
	public int hashCode() {
		if (uri != null) {
			return uri.hashCode();
		}
		return resource.hashCode();
	}

	@Override
	public String toString() {
		return resource.toString();
	}
}
