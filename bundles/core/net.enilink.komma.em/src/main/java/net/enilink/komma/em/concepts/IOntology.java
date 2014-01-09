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
package net.enilink.komma.em.concepts;

import java.util.Collection;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.Ontology;

public interface IOntology extends Ontology {
	Collection<IClass> getRootClasses();

	IExtendedIterator<IProperty> getRootProperties();

	IExtendedIterator<IProperty> getRootObjectProperties();

	IExtendedIterator<IProperty> getRootDatatypeProperties();
}
