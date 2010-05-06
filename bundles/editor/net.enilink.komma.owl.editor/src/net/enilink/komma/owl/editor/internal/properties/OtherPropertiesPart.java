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
package net.enilink.komma.owl.editor.internal.properties;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.ISparqlConstants;

public class OtherPropertiesPart extends AbstractPropertiesPart {
	@Override
	protected String getName() {
		return "Properties";
	}

	@Override
	public void refresh() {
		super.refresh();
		
		if (model != null) {
			IQuery<?> query = model
					.getOntology()
					.getKommaManager()
					.createQuery(
							ISparqlConstants.PREFIX
									+ " SELECT DISTINCT ?r WHERE {"
									+ " ?r a rdf:Property ."
									+ " OPTIONAL { ?r rdfs:subPropertyOf ?other"
									+ " FILTER (?r != ?other && isIRI(?other))} "
									+ " OPTIONAL { ?p a owl:DatatypeProperty . "
									+ " FILTER (?r = ?p ) . }"
									+ " OPTIONAL { ?p a owl:ObjectProperty ."
									+ " FILTER (?r = ?p ) .	}"
									+ " FILTER (!bound(?p)) ."
									+ " FILTER (!bound(?other) && isIRI(?r)) }");

			treeViewer.setInput(query.evaluate(IProperty.class).toList()
					.toArray());
		}
	}

	@Override
	protected URI getPropertyType() {
		return OWL.TYPE_OWLPROPERTY;
		// TODO: Typauswahl im Wizard (siehe Topbraid)
	}
}
