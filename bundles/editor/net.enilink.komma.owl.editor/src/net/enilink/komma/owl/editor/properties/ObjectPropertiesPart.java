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

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.core.URI;

public class ObjectPropertiesPart extends AbstractPropertiesPart {
	@Override
	protected String getName() {
		return "ObjectProperties";
	}

	@Override
	public void refresh() {
		super.refresh();
		if (model != null) {
			treeViewer.setInput(model.getOntology().getRootObjectProperties()
					.toList().toArray());
		}
	}

	@Override
	protected URI getPropertyType() {
		return OWL.TYPE_OBJECTPROPERTY;
	}
}
