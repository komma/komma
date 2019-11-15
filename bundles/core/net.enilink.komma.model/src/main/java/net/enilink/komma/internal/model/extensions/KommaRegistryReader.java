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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;

import net.enilink.commons.util.extensions.RegistryReader;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.KommaException;

public abstract class KommaRegistryReader extends RegistryReader {
	protected KommaRegistryReader(IExtensionRegistry registry, String pluginId,
			String anExtensionPoint) {
		super(registry, pluginId, anExtensionPoint);
	}

	protected KommaRegistryReader(String pluginId, String extensionPoint) {
		super(pluginId, extensionPoint);
	}

	public static class PluginClassDescriptor {
		protected IConfigurationElement element;
		protected String attributeName;

		public PluginClassDescriptor(IConfigurationElement element,
				String attributeName) {
			this.element = element;
			this.attributeName = attributeName;
		}

		public Object createInstance() {
			try {
				return element.createExecutableExtension(attributeName);
			} catch (CoreException e) {
				throw new KommaException(e);
			}
		}
	}

	static class ModelFactoryDescriptor extends PluginClassDescriptor implements
			IModel.Factory.IDescriptor {
		protected IModel.Factory factoryInstance;

		public ModelFactoryDescriptor(IConfigurationElement e, String attrName) {
			super(e, attrName);
		}

		public IModel.Factory createFactory() {
			if (factoryInstance == null) {
				factoryInstance = (IModel.Factory) createInstance();
			}
			return factoryInstance;
		}
	}

}
