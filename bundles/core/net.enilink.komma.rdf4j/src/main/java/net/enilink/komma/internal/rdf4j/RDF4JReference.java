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
package net.enilink.komma.internal.rdf4j;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class RDF4JReference implements IReference {
	private Resource resource;

	private URI uri;

	public RDF4JReference(Resource resource) {
		this.resource = resource;
		if (resource instanceof IRI) {
			this.uri = URIs.createURI(((IRI) resource).stringValue());
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
		return obj instanceof RDF4JReference
				&& resource.equals(((RDF4JReference) obj).resource);
	}

	public Resource getRDF4JResource() {
		return resource;
	}

	@Override
	public URI getURI() {
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
