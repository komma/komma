/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.internal.model.extensions;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.ModelPlugin;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * A plugin extension reader that populates the
 * {@link IModel.Factory.Registry} model factory's
 * {@link IModel.Factory.Registry#getContentTypeToFactoryMap()
 * content type} map. Clients are not expected to use this class directly.
 */
public class ContentFactoriesRegistryReader extends KommaRegistryReader {
	static final String TAG_FACTORY = "factory";
	static final String ATT_CONTENT_TYPE_IDENTIFIER = "contentTypeIdentifier";
	static final String ATT_CLASS = "class";

	private IModel.Factory.Registry modelFactoryRegistry;

	public ContentFactoriesRegistryReader(
			IModel.Factory.Registry ontologyFactoryRegistry) {
		super(ModelPlugin.PLUGIN_ID, "contentFactories");
		this.modelFactoryRegistry = ontologyFactoryRegistry;
	}

	@Override
	protected boolean readElement(IConfigurationElement element, boolean add) {
		if (element.getName().equals(TAG_FACTORY)) {
			String contentTypeIdentifier = element
					.getAttribute(ATT_CONTENT_TYPE_IDENTIFIER);
			if (contentTypeIdentifier == null) {
				logMissingAttribute(element, ATT_CONTENT_TYPE_IDENTIFIER);
			} else if (element.getAttribute(ATT_CLASS) == null) {
				logMissingAttribute(element, ATT_CLASS);
			} else if (add) {
				Object previous = modelFactoryRegistry
						.getContentTypeToFactoryMap().put(
								contentTypeIdentifier,
								new ModelFactoryDescriptor(element, ATT_CLASS));
				if (previous instanceof ModelFactoryDescriptor) {
					ModelFactoryDescriptor descriptor = (ModelFactoryDescriptor) previous;
					ModelPlugin.logErrorMessage("Both '"
							+ descriptor.element.getContributor().getName()
							+ "' and '" + element.getContributor().getName()
							+ "' register a content factory for '"
							+ contentTypeIdentifier + "'");
				}
				return true;
			} else {
				modelFactoryRegistry.getContentTypeToFactoryMap().remove(
						contentTypeIdentifier);
				return true;
			}
		}

		return false;
	}
}
