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
package net.enilink.komma.owl.edit;

import java.util.Collection;

import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.owl.edit.manchester.ManchesterSyntaxGenerator;
import net.enilink.komma.rdfs.edit.RDFSClassItemProvider;

public class OWLClassItemProvider extends RDFSClassItemProvider {
	public OWLClassItemProvider(OWLItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	@Override
	public String getText(Object object) {
		return ManchesterSyntaxGenerator.generateText(object);
	}
}
