/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.properties.internal.parts;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.enilink.commons.ui.editor.IEditorPart;
import net.enilink.commons.ui.editor.IEditorPartProvider;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;

public abstract class ObjectDetailsPartProvider implements IEditorPartProvider {
	class Key {
		IResource resource;
		Collection<IClass> classes;

		Key(IResource resource, Collection<IClass> classes) {
			this.resource = resource;
			this.classes = classes;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((classes == null) ? 0 : classes.hashCode());
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
			final Key other = (Key) obj;
			if (classes == null) {
				if (other.classes != null)
					return false;
			} else if (!classes.equals(other.classes))
				return false;
			return true;
		}
	}

	public IEditorPart getPart(Object key) {
		Collection<IClass> classes = ((Key) key).classes;

		if (classes == null) {
			return null;
		} else {
			Collection<IProperty> properties = ((Key) key).resource
					.getRelevantProperties().toList();

			return createEditorPart(classes, properties);
		}
	}

	abstract protected IEditorPart createEditorPart(Collection<IClass> classes,
			Collection<IProperty> properties);

	public Object getPartKey(Object object) {
		if (object instanceof IResource) {
			IResource resource = (IResource) object;

			Set<IClass> classes = new HashSet<IClass>();
			Set<net.enilink.vocab.rdfs.Class> rdfTypes = resource
					.getRdfTypes();
			if (rdfTypes != null) {
				for (net.enilink.vocab.rdfs.Class rdfType : rdfTypes) {
					classes.add((IClass) rdfType);
				}
			}

			return new Key(resource, classes);
		}
		return null;
	}
}