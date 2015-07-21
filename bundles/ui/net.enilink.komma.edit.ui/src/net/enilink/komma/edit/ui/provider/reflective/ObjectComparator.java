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
package net.enilink.komma.edit.ui.provider.reflective;

import org.eclipse.jface.viewers.ViewerComparator;

import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.Resource;

public class ObjectComparator extends ViewerComparator {
	@Override
	public int category(Object element) {
		if (!(element instanceof Resource))
			return 0;

		if (element instanceof Property) {
			return 2;
		}
		return 1;
	}
}
