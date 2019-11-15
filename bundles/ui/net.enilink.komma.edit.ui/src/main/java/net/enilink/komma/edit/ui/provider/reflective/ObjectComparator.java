/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
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
