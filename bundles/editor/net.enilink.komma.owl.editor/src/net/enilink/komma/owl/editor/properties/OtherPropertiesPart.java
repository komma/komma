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
package net.enilink.komma.owl.editor.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.vocab.komma.KOMMA;
import net.enilink.vocab.owl.AnnotationProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.OntologyProperty;
import net.enilink.vocab.rdf.RDF;

public class OtherPropertiesPart extends AbstractPropertiesPart {
	@Override
	protected String getName() {
		return "Properties";
	}

	@Override
	protected URI getPropertyType() {
		return RDF.TYPE_PROPERTY;
	}

	@Override
	protected URI getRootProperty() {
		return KOMMA.PROPERTY_ROOTPROPERTY;
	}

	@Override
	protected Object[] filterElements(Object parent, Object[] elements) {
		if (hideBuiltins && KOMMA.PROPERTY_ROOTPROPERTY.equals(parent)) {
			List<Object> list = new ArrayList<>(Arrays.asList(elements));
			for (Iterator<?> it = list.iterator(); it.hasNext();) {
				Object element = it.next();
				if (element instanceof IReference) {
					URI uri = ((IReference) element).getURI();
					if (uri != null
							&& RDF.NAMESPACE_URI.equals(uri.namespace())
							|| OWL.NAMESPACE_URI.equals(uri.namespace())
							&& !(element instanceof AnnotationProperty || element instanceof OntologyProperty)) {
						it.remove();
					}
				}
			}
			return list.toArray();
		}
		return super.filterElements(parent, elements);
	}
}
