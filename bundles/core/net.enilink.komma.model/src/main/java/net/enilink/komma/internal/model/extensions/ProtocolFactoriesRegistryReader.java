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
package net.enilink.komma.internal.model.extensions;

import org.eclipse.core.runtime.IConfigurationElement;

import net.enilink.komma.KommaCore;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.ModelCore;

/**
 * A plugin extension reader that populates the
 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#INSTANCE
 * global} resource factory's
 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#getProtocolToFactoryMap()
 * protocol} map. Clients are not expected to use this class directly.
 */
public class ProtocolFactoriesRegistryReader extends KommaRegistryReader {
	static final String TAG_FACTORY = "factory";
	static final String ATT_PROTOCOLNAME = "protocolName";
	static final String ATT_CLASS = "class";

	private IModel.Factory.Registry modelFactoryRegistry;

	public ProtocolFactoriesRegistryReader(
			IModel.Factory.Registry ontologyFactoryRegistry) {
		super(ModelCore.PLUGIN_ID, "protocolFactories");
		this.modelFactoryRegistry = ontologyFactoryRegistry;
	}

	@Override
	protected boolean readElement(IConfigurationElement element, boolean add) {
		if (element.getName().equals(TAG_FACTORY)) {
			String protocolName = element.getAttribute(ATT_PROTOCOLNAME);
			if (protocolName == null) {
				logMissingAttribute(element, ATT_PROTOCOLNAME);
			} else if (element.getAttribute(ATT_CLASS) == null) {
				logMissingAttribute(element, ATT_CLASS);
			} else if (add) {
				Object previous = modelFactoryRegistry
						.getProtocolToFactoryMap().put(protocolName,
								new ModelFactoryDescriptor(element, ATT_CLASS));
				if (previous instanceof ModelFactoryDescriptor) {
					ModelFactoryDescriptor descriptor = (ModelFactoryDescriptor) previous;
					KommaCore.logErrorMessage("Both '"
							+ descriptor.element.getContributor().getName()
							+ "' and '" + element.getContributor().getName()
							+ "' register a protocol parser for '"
							+ protocolName + "'");
				}
				return true;
			} else {
				modelFactoryRegistry.getProtocolToFactoryMap().remove(
						protocolName);
				return true;
			}
		}

		return false;
	}
}
