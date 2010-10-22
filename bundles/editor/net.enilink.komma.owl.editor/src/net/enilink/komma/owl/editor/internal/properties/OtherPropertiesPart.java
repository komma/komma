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
									+ "SELECT DISTINCT ?p WHERE {"
									+ "?p a [rdfs:subClassOf rdf:Property] ."
									+ "OPTIONAL {?p rdfs:subPropertyOf ?other ."
									+ "FILTER (?p != ?other && isIRI(?other))} "
									+ "OPTIONAL {"
									+ "		?p a ?type . ?type rdfs:subClassOf rdf:Property ."
									+ " 	FILTER (?type = owl:AnnotationProperty || ?type != rdf:Property && !regex(str(?type), 'http://www.w3.org/2002/07/owl#'))"
									+ "}"
									+ "FILTER (bound(?type) && !bound(?other) && isIRI(?p)) }");

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
