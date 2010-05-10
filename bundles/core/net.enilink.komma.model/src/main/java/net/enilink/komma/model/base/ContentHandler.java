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
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.ITextContentDescriber;

import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

/**
 * An implementation of a content handler.
 */
public class ContentHandler implements IContentHandler {
	/**
	 * Creates a map with a single entry from
	 * {@link IContentHandler#VALIDITY_PROPERTY} to the given validity value.
	 * 
	 * @param validity
	 *            the value of the validity property.
	 * @return a map with a single entry from
	 *         {@link IContentHandler#VALIDITY_PROPERTY} to the given validity
	 *         value.
	 */
	public static Map<String, Object> createContentDescription(Validity validity) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put(VALIDITY_PROPERTY, validity);
		return result;
	}

	/**
	 * Returns the value of {@link IContentHandler#OPTION_REQUESTED_PROPERTIES}
	 * in the options map.
	 * 
	 * @param options
	 *            the options in which to look up the property.
	 * @return value of {@link IContentHandler#OPTION_REQUESTED_PROPERTIES} in
	 *         the options map.
	 */
	@SuppressWarnings("unchecked")
	protected Set<String> getRequestedProperties(Map<?, ?> options) {
		return (Set<String>) options.get(OPTION_REQUESTED_PROPERTIES);
	}

	/**
	 * Returns whether the named property is one requested in the options.
	 * 
	 * @param property
	 *            the property in question.
	 * @param options
	 *            the options in which to look for the requested property.
	 * @return whether the named property is one requested in the options.
	 * @see #getRequestedProperties(Map)
	 */
	protected boolean isRequestedProperty(String property, Map<?, ?> options) {
		if (IContentHandler.VALIDITY_PROPERTY.equals(property)
				|| IContentHandler.CONTENT_TYPE_PROPERTY.equals(property)) {
			return true;
		} else {
			Set<String> requestedProperties = getRequestedProperties(options);
			if (requestedProperties == null) {
				return true;
			} else {
				return requestedProperties.contains(property);
			}
		}
	}

	/**
	 * This implementations always return true; clients are generally expected
	 * to override this.
	 * 
	 * @param uri
	 *            the URI in questions.
	 * @return true;
	 */
	public boolean canHandle(URI uri) {
		return true;
	}

	/**
	 * This base implementation handles looking up the
	 * {@link IContentHandler#BYTE_ORDER_MARK_PROPERTY} if that's a
	 * {@link #isRequestedProperty(String, Map) requested property}.
	 */
	public Map<String, Object> contentDescription(URI uri,
			InputStream inputStream, Map<?, ?> options,
			Map<Object, Object> context) throws IOException {
		Map<String, Object> result = createContentDescription(IContentHandler.Validity.INDETERMINATE);
		if (isRequestedProperty(IContentHandler.BYTE_ORDER_MARK_PROPERTY,
				options)) {
			result.put(IContentHandler.BYTE_ORDER_MARK_PROPERTY,
					getByteOrderMark(uri, inputStream, options, context));
		}
		return result;
	}

	/**
	 * Returns the byte order marker at the start of the input stream.
	 * 
	 * @param uri
	 *            the URI of the input stream.
	 * @param inputStream
	 *            the input stream to scan.
	 * @param options
	 *            any options to influence the behavior; this base
	 *            implementation ignores this.
	 * @param context
	 *            the cache for fetching and storing a previous computation of
	 *            the byte order marker; this base implementation caches
	 *            {@link IContentHandler#BYTE_ORDER_MARK_PROPERTY}.
	 * @return the byte order marker at the start of the input stream.
	 * @throws IOException
	 */
	protected ByteOrderMark getByteOrderMark(URI uri, InputStream inputStream,
			Map<?, ?> options, Map<Object, Object> context) throws IOException {
		ByteOrderMark result = (ByteOrderMark) context
				.get(IContentHandler.BYTE_ORDER_MARK_PROPERTY);
		if (result == null) {
			result = ByteOrderMark.read(inputStream);
			inputStream.reset();
			context.put(IContentHandler.BYTE_ORDER_MARK_PROPERTY, result);
		}
		return result;
	}

	/**
	 * An implementation of a describer that delegates to a
	 * {@link IContentHandler}.
	 */
	public static class Describer implements IContentDescriber,
			ITextContentDescriber, IExecutableExtension {
		/**
		 * The content handler delegate.
		 */
		protected IContentHandler contentHandler;

		/**
		 * Returns the qualified names of the supported options. This base
		 * implementation supports only {@link IContentDescription#CHARSET} and
		 * {@link IContentDescription#BYTE_ORDER_MARK}.
		 * 
		 * @return the qualified names of the supported options.
		 */
		public QualifiedName[] getSupportedOptions() {
			return SUPPORTED_OPTIONS;
		}

		/**
		 * This base implementation supports only
		 * {@link IContentDescription#CHARSET} and
		 * {@link IContentDescription#BYTE_ORDER_MARK}.
		 */
		private static final QualifiedName[] SUPPORTED_OPTIONS = {
				IContentDescription.CHARSET,
				IContentDescription.BYTE_ORDER_MARK };

		/**
		 * Returns the qualified name converted to the corresponding property
		 * string.
		 * 
		 * @param qualifiedName
		 *            the qualified name to convert.
		 * @return the qualified name converted to the corresponding property
		 *         string.
		 */
		protected String getProperty(QualifiedName qualifiedName) {
			return qualifiedName.toString();
		}

		/**
		 * Returns the given property's basic EMF value converted to the
		 * corresponding Eclipse value.
		 * 
		 * @param qualifiedName
		 *            the name of the property for which this value applies.
		 * @param value
		 *            the value to convert.
		 * @return the given property's basic EMF value converted to the
		 *         corresponding Eclipse value.
		 */
		protected Object getDescriptionValue(QualifiedName qualifiedName,
				Object value) {
			if (value == null) {
				return null;
			} else if (IContentDescription.BYTE_ORDER_MARK
					.equals(qualifiedName)) {
				return ((IContentHandler.ByteOrderMark) value).bytes();
			} else {
				return value;
			}
		}

		public int describe(InputStream inputStream,
				IContentDescription description) throws IOException {
			Map<Object, Object> options = new HashMap<Object, Object>();
			Map<String, ?> result;
			if (description != null) {
				Map<String, QualifiedName> requestedPropertyToQualifiedNameMap = new HashMap<String, QualifiedName>();
				Set<String> requestedProperties = new HashSet<String>();
				for (QualifiedName qualifiedName : getSupportedOptions()) {
					if (description.isRequested(qualifiedName)) {
						String property = getProperty(qualifiedName);
						if (property != null) {
							requestedPropertyToQualifiedNameMap.put(property,
									qualifiedName);
							requestedProperties.add(property);
						}
					}
				}
				options.put(IContentHandler.OPTION_REQUESTED_PROPERTIES,
						requestedProperties);
				result = contentHandler.contentDescription(URIImpl.createURI("*"),
						inputStream, options, new HashMap<Object, Object>());
				for (Map.Entry<String, ?> property : result.entrySet()) {
					QualifiedName qualifiedName = requestedPropertyToQualifiedNameMap
							.get(property.getKey());
					if (qualifiedName != null) {
						description.setProperty(qualifiedName,
								getDescriptionValue(qualifiedName, property
										.getValue()));
					}
				}
			} else {
				options.put(IContentHandler.OPTION_REQUESTED_PROPERTIES,
						Collections.emptySet());
				result = contentHandler.contentDescription(URIImpl.createURI("*"),
						inputStream, options, new HashMap<Object, Object>());
			}
			return ((IContentHandler.Validity) result
					.get(IContentHandler.VALIDITY_PROPERTY)).ordinal();
		}

		public int describe(Reader reader, IContentDescription description)
				throws IOException {
			return describe(new ReaderInputStream(reader), description);
		}

		public void setInitializationData(
				IConfigurationElement configurationElement,
				String propertyName, Object data) throws CoreException {
			Map<String, String> parameters = getParameters(
					configurationElement, propertyName, data);
			contentHandler = createContentHandler(parameters);
		}

		/**
		 * Returns the new content handler for the given parameters that were
		 * supplied by the registration of the Eclipse content type.
		 * 
		 * @param parameters
		 *            the parameter for configuring the content handler.
		 * @return the next content handler.
		 */
		protected IContentHandler createContentHandler(
				Map<String, String> parameters) {
			return null;
		}

		/**
		 * The key in the
		 * {@link #getParameters(IConfigurationElement, String, Object)
		 * parameters map} representing the content type identifier String.
		 */
		protected static final String CONTENT_TYPE_ID = "contentTypeID";

		/**
		 * The key in the
		 * {@link #getParameters(IConfigurationElement, String, Object)
		 * parameters map} representing the extensions, which are encoded as a
		 * space separate list of suffixes.
		 */
		protected static final String EXTENSIONS = "extensions";

		/**
		 * Returns the map of parameters as fetched from the given configuration
		 * element's information. This implementation populates the
		 * {@link #CONTENT_TYPE_ID content type identifier} and the
		 * {@link #EXTENSIONS extensions}.
		 * 
		 * @param configurationElement
		 *            the configuration element of the content type.
		 * @param propertyName
		 *            the property for this particular for this instance.
		 * @param data
		 *            the data associated with this instance.
		 * @return the map of parameters as fetched from the given configuration
		 *         element's information.
		 */
		protected Map<String, String> getParameters(
				IConfigurationElement configurationElement,
				String propertyName, Object data) {
			Map<String, String> parameters = new HashMap<String, String>();
			if (data != null) {
				@SuppressWarnings("unchecked")
				Map<String, String> dataMap = (Map<String, String>) data;
				parameters.putAll(dataMap);
				parameters.put(CONTENT_TYPE_ID, configurationElement
						.getAttribute("id"));
				String fileExtensions = configurationElement
						.getAttribute("file-extensions");
				if (fileExtensions != null) {
					parameters
							.put(EXTENSIONS, fileExtensions.replace(',', ' '));
				}
			}
			return parameters;
		}
	}
}
