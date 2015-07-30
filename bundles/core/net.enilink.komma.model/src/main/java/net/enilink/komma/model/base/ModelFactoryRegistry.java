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
package net.enilink.komma.model.base;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.core.URI;

/**
 * An extensible implementation of a resource factory registry.
 */
public class ModelFactoryRegistry implements IModel.Factory.Registry {
	/**
	 * The protocol map.
	 */
	protected Map<String, Object> protocolToFactoryMap = new HashMap<String, Object>();

	/**
	 * The extension map.
	 */
	protected Map<String, Object> extensionToFactoryMap = new HashMap<String, Object>();

	/**
	 * The content type identifier map.
	 */
	protected Map<String, Object> contentTypeIdentifierToFactoryMap = new HashMap<String, Object>();

	/**
	 * Returns the model factory appropriate for the given URI.
	 * <p>
	 * This implementation does the
	 * {@link IModel.Factory.Registry#getFactory(URI)
	 * typical} thing. It will delegate to
	 * {@link #delegatedGetFactory(URI, String)} in the case that the typical
	 * behavior doesn't produce a result; clients are encouraged to override
	 * that method only.
	 * </p>
	 * 
	 * @param uri
	 *            the URI.
	 * @return the resource factory appropriate for the given URI.
	 * @see org.eclipse.emf.ecore.resource.ResourceSet#createModel(URI)
	 */
	public IModel.Factory getFactory(URI uri) {
		return convert(getFactory(uri, protocolToFactoryMap,
				extensionToFactoryMap, contentTypeIdentifierToFactoryMap,
				IContentHandler.UNSPECIFIED_CONTENT_TYPE, true));
	}

	/**
	 * Returns the resource factory appropriate for the given URI.
	 * <p>
	 * This implementation does the
	 * {@link IModel.Factory.Registry#getFactory(URI, String)
	 * typical} thing. It will delegate to
	 * {@link #delegatedGetFactory(URI, String)} in the case that the typical
	 * behavior doesn't produce a result; clients are encouraged to override
	 * that method only.
	 * </p>
	 * 
	 * @param uri
	 *            the URI.
	 * @return the resource factory appropriate for the given URI.
	 * @see IModelSet#createModel(URI)
	 */
	public IModel.Factory getFactory(URI uri, String contentType) {
		return convert(getFactory(uri, protocolToFactoryMap,
				extensionToFactoryMap, contentTypeIdentifierToFactoryMap,
				contentType, true));
	}

	public static IModel.Factory convert(Object ontModelFactory) {
		return ontModelFactory instanceof IModel.Factory.IDescriptor ? ((IModel.Factory.IDescriptor) ontModelFactory)
				.createFactory()
				: (IModel.Factory) ontModelFactory;
	}

	protected Object getFactory(URI uri,
			Map<String, Object> protocolToFactoryMap,
			Map<String, Object> extensionToFactoryMap,
			Map<String, Object> contentTypeIdentifierToFactoryMap,
			String contentTypeIdentifier, boolean delegate) {
		Object modelFactory = null;
		if (!protocolToFactoryMap.isEmpty()) {
			modelFactory = protocolToFactoryMap.get(uri.scheme());
		}
		if (modelFactory == null) {
			boolean extensionToFactoryMapIsEmpty = extensionToFactoryMap
					.isEmpty();
			if (!extensionToFactoryMapIsEmpty) {
				modelFactory = extensionToFactoryMap
						.get(uri.fileExtension());
			}
			if (modelFactory == null) {
				boolean contentTypeIdentifierToFactoryMapIsEmpty = contentTypeIdentifierToFactoryMap
						.isEmpty();
				if (!contentTypeIdentifierToFactoryMapIsEmpty) {
					if (IContentHandler.UNSPECIFIED_CONTENT_TYPE
							.equals(contentTypeIdentifier)) {
						contentTypeIdentifier = getContentTypeIdentifier(uri);
					}
					if (contentTypeIdentifier != null) {
						modelFactory = contentTypeIdentifierToFactoryMap
								.get(contentTypeIdentifier);
					}
				}
				if (modelFactory == null) {
					if (!extensionToFactoryMapIsEmpty) {
						modelFactory = extensionToFactoryMap
								.get(IModel.Factory.Registry.DEFAULT_EXTENSION);
					}
					if (modelFactory == null) {
						if (!contentTypeIdentifierToFactoryMapIsEmpty) {
							modelFactory = contentTypeIdentifierToFactoryMap
									.get(IModel.Factory.Registry.DEFAULT_CONTENT_TYPE_IDENTIFIER);
						}
						if (modelFactory == null && delegate) {
							modelFactory = delegatedGetFactory(uri,
									contentTypeIdentifier);
						}
					}
				}
			}
		}
		return modelFactory;
	}

	protected String getContentTypeIdentifier(URI uri) {
		try {
			Map<String, ?> contentDescription = getURIConverter()
					.contentDescription(uri, getContentDescriptionOptions());
			return (String) contentDescription
					.get(IContentHandler.CONTENT_TYPE_PROPERTY);
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns the URI converter that's used to
	 * {@link URIConverter#contentDescription(URI, Map) compute} the content
	 * type identifier.
	 * 
	 * @return the URI converter that's used to compute the content type
	 *         identifier.
	 */
	protected IURIConverter getURIConverter() {
		return null;
	}

	/**
	 * A constant read only map of
	 * {@link URIConverter#contentDescription(URI, Map) options} used to request
	 * just the {@link IContentHandler#CONTENT_TYPE_PROPERTY content type}.
	 */
	protected static final Map<?, ?> CONTENT_DESCRIPTION_OPTIONS;
	static {
		Map<Object, Object> contentDescriptionOptions = new HashMap<Object, Object>();
		Set<String> requestedProperties = new HashSet<String>();
		requestedProperties.add(IContentHandler.CONTENT_TYPE_PROPERTY);
		contentDescriptionOptions
				.put(IContentHandler.OPTION_REQUESTED_PROPERTIES,
						requestedProperties);
		CONTENT_DESCRIPTION_OPTIONS = Collections
				.unmodifiableMap(contentDescriptionOptions);
	}

	/**
	 * Returns the default options used to
	 * {@link URIConverter#contentDescription(URI, Map) compute} the content
	 * type identifier.
	 * 
	 * @return the default options used to compute the content type identifier.
	 */
	protected Map<?, ?> getContentDescriptionOptions() {
		return CONTENT_DESCRIPTION_OPTIONS;
	}

	/**
	 * Returns the resource factory appropriate for the given URI and
	 * {@link IContentHandler#CONTENT_TYPE_PROPERTY content type identifier},
	 * when standard alternatives fail.
	 * <p>
	 * This implementation calls {@link #delegatedGetFactory(URI)}; clients are
	 * encouraged to override it.
	 * </p>
	 * 
	 * @param uri
	 *            the URI.
	 * @param contentTypeIdentifier
	 *            the {@link IContentHandler#CONTENT_TYPE_PROPERTY content type
	 *            identifier}.
	 * @return the resource factory appropriate for the given URI and content
	 *         type identifier.
	 * @see #getFactory(URI)
	 */
	protected IModel.Factory delegatedGetFactory(URI uri,
			String contentTypeIdentifier) {
		return null;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public Map<String, Object> getExtensionToFactoryMap() {
		return extensionToFactoryMap;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public Map<String, Object> getProtocolToFactoryMap() {
		return protocolToFactoryMap;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public Map<String, Object> getContentTypeToFactoryMap() {
		return contentTypeIdentifierToFactoryMap;
	}
}
