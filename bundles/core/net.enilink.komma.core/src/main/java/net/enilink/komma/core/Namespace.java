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
package net.enilink.komma.core;

public class Namespace implements INamespace {
	protected String prefix;
	protected URI uri;
	protected boolean derived;

	public Namespace(String prefix, URI uri) {
		this(prefix, uri, false);
	}

	public Namespace(String prefix, URI uri, boolean derived) {
		this.prefix = prefix;
		this.uri = uri;
		this.derived = derived;
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public boolean isDerived() {
		return derived;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Namespace other = (Namespace) obj;
		if (prefix == null) {
			if (other.prefix != null)
				return false;
		} else if (!prefix.equals(other.prefix))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder("(").append(prefix).append(" -> ").append(uri)
				.toString();
	}
}
