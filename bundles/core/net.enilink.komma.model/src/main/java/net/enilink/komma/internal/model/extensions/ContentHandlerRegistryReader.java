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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.ModelPlugin;

/**
 * A plugin extension reader that populates the
 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#INSTANCE
 * global} resource factory's
 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Registry#getContentTypeToFactoryMap()
 * content type} map. Clients are not expected to use this class directly.
 */
public class ContentHandlerRegistryReader extends KommaRegistryReader {
	static final String TAG_HANDLER = "contentHandler";
	static final String ATT_CLASS = "class";
	static final String ATT_PRIORITY = "priority";

	protected IContentHandler.Registry contentHandlerRegistry;

	public ContentHandlerRegistryReader(
			IContentHandler.Registry contentHandlerRegistry) {
		super(ModelPlugin.PLUGIN_ID, "contentHandlers");
		this.contentHandlerRegistry = contentHandlerRegistry;
	}

	private final Map<String, List<IContentHandler>> CONTRIBUTION_MAP = new HashMap<String, List<IContentHandler>>();

	@Override
	protected boolean readElement(IConfigurationElement element, boolean add) {
		if (element.getName().equals(TAG_HANDLER)) {
			int priority = 0;
			if (element.getAttribute(ATT_PRIORITY) != null) {
				priority = Integer.parseInt(element.getAttribute(ATT_PRIORITY));
			}
			String contributorClassName = element.getAttribute(ATT_CLASS);
			if (contributorClassName == null) {
				logMissingAttribute(element, ATT_CLASS);
			} else {
				String contributorName = element.getContributor().getName();
				if (add) {
					try {
						Optional<Bundle> contributorBundle = Optional.empty();
						if (ModelPlugin.IS_OSGI_RUNNING) {
							contributorBundle = Stream
									.of(FrameworkUtil.getBundle(ModelPlugin.class).getBundleContext().getBundles())
									.filter(b -> b.getSymbolicName().equals(element.getNamespaceIdentifier()))
									.findFirst();
						}
						@SuppressWarnings("unchecked")
						Class<IContentHandler> contributorHandlerClass = (Class<IContentHandler>) (contributorBundle
								.isPresent() ? contributorBundle.get().loadClass(contributorClassName)
										: Class.forName(contributorClassName));
						Map<String, String> parameters = new HashMap<String, String>();
						for (IConfigurationElement parameter : element
								.getChildren("parameter")) {
							parameters.put(parameter.getAttribute("name"),
									parameter.getAttribute("value"));
						}
						IContentHandler contentHandler = parameters.isEmpty() ? contributorHandlerClass
								.newInstance() : contributorHandlerClass
								.getConstructor(Map.class).newInstance(
										parameters);
						contentHandlerRegistry.put(priority, contentHandler);
						List<IContentHandler> contributions = CONTRIBUTION_MAP
								.get(contributorName);
						if (contributions == null) {
							CONTRIBUTION_MAP
									.put(contributorName,
											contributions = new ArrayList<IContentHandler>());
						}
						contributions.add(contentHandler);
					} catch (Exception exception) {
						ModelPlugin.log(exception);
					}
					return true;
				} else {
					List<IContentHandler> contributions = CONTRIBUTION_MAP
							.get(contributorName);
					if (contributions != null) {
						for (List<IContentHandler> values : contentHandlerRegistry
								.values()) {
							values.removeAll(contributions);
						}
					}
					CONTRIBUTION_MAP.remove(contributorName);
					return true;
				}
			}
		} else if (element.getName().equals("parameter")) {
			return true;
		}

		return false;
	}
}
