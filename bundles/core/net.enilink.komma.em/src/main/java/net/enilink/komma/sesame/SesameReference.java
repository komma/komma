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
package net.enilink.komma.sesame;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URIImpl;

public class SesameReference implements IReference, ISesameResourceAware {
	private net.enilink.komma.core.URI uri;

	private Resource resource;

	public SesameReference(IReference entity) {
		this.uri = entity.getURI();
		if (entity instanceof ISesameResourceAware) {
			this.resource = ((ISesameResourceAware) entity).getSesameResource();
		} else {
			throw new IllegalArgumentException(
					"Parameter entity must be instance of "
							+ ISesameResourceAware.class);
		}
	}

	public SesameReference(Resource resource) {
		this.resource = resource;
		if (resource instanceof URI) {
			this.uri = URIImpl.createURI(((URI) resource).stringValue());
		}
	}

	@Override
	public net.enilink.komma.core.URI getURI() {
		return uri;
	}

	@Override
	public Resource getSesameResource() {
		return resource;
	}

	@Override
	public String toString() {
		return resource.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (uri != null) {
			return uri.equals(obj);
		}
		return obj instanceof ISesameResourceAware
				&& resource.equals(((ISesameResourceAware) obj)
						.getSesameResource());
	}

	@Override
	public int hashCode() {
		if (uri != null) {
			return uri.hashCode();
		}
		return resource.hashCode();
	}
}
