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
package net.enilink.komma.owl.edit;

import java.util.Arrays;
import java.util.Collection;

import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.common.util.IResourceLocator;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.parser.manchester.ManchesterSyntaxGenerator;
import net.enilink.komma.rdfs.edit.RDFSClassItemProvider;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;

public class OWLClassItemProvider extends RDFSClassItemProvider {
	public OWLClassItemProvider(OWLItemProviderAdapterFactory adapterFactory,
			IResourceLocator resourceLocator, Collection<IClass> supportedTypes) {
		super(adapterFactory, resourceLocator, supportedTypes);
	}

	@Override
	protected void collectNewChildDescriptors(
			ICollector<Object> newChildDescriptors, Object object) {
		if (object instanceof IClass) {
			IEntityManager em = ((IEntity) object).getEntityManager();
			for (IReference type : Arrays.asList(OWL.TYPE_CLASS,
					RDFS.TYPE_CLASS)) {
				newChildDescriptors.add(createChildParameter(em
						.find(RDFS.PROPERTY_SUBCLASSOF), new ChildDescriptor(
						Arrays.asList(em.find(type, IClass.class)), true)));
			}
		}
	}

	@Override
	public String getText(Object object) {
		if (object instanceof IReference) {
			if (((IReference) object).getURI() == null) {
				return new ManchesterSyntaxGenerator() {
					protected String getPrefix(IReference reference) {
						if (reference instanceof IObject
								&& reference.getURI() != null
								&& reference
										.getURI()
										.namespace()
										.trimFragment()
										.equals(((IObject) reference)
												.getModel().getURI()
												.trimFragment())) {
							return "";
						}
						return super.getPrefix(reference);
					}
				}.generateText(object);
			}
			return ModelUtil.getLabel(object);
		}
		return String.valueOf(object);
	}
}
