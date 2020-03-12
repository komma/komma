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
package net.enilink.komma.workbench.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;

import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.base.ContentHandler;
import net.enilink.komma.core.URI;

public class PlatformContentHandler extends ContentHandler {
	@Override
	public boolean canHandle(URI uri) {
		return uri.isPlatform();
	}
	
	/**
	 * Returns the property name converted to a qualified name.
	 * 
	 * @param property
	 *            the property to convert.
	 * @return the property name converted to a qualified name.
	 */
	protected QualifiedName getQualifiedName(String property) {
		int index = property.lastIndexOf(":");
		if (index == -1) {
			return new QualifiedName(null, property);
		} else {
			return new QualifiedName(property.substring(0, index),
					property.substring(index + 1));
		}
	}

	/**
	 * Returns the given property's Eclipse value converted to EMF's
	 * corresponding basic value.
	 * 
	 * @param qualifiedName
	 *            the name of the property for which this value applies.
	 * @param value
	 *            the value to convert.
	 * @return the given property's Eclipse value converted to EMF's
	 *         corresponding basic value.
	 */
	protected Object getDescriptionValue(QualifiedName qualifiedName,
			Object value) {
		if (value == null) {
			return null;
		} else if (IContentDescription.BYTE_ORDER_MARK.equals(qualifiedName)) {
			for (ByteOrderMark byteOrderMarker : ContentHandler.ByteOrderMark
					.values()) {
				if (value == byteOrderMarker.bytes()) {
					return byteOrderMarker;
				}
			}
			return null;
		} else {
			return value;
		}
	}

	/**
	 * This implementation delegates to the platform's content description
	 * support,
	 */
	@Override
	public Map<String, Object> contentDescription(URI uri,
			InputStream inputStream, Map<?, ?> options,
			Map<Object, Object> context) throws IOException {
		IContentDescription contentDescription;
		if (uri.isPlatformResource()
				&& PlatformResourceURIHandler.WorkbenchHelper
						.getWorkspaceRoot() != null) {
			contentDescription = PlatformResourceURIHandler.WorkbenchHelper
					.getContentDescription(uri.toPlatformString(true), options);
		} else {
			contentDescription = Platform.getContentTypeManager()
					.getDescriptionFor(inputStream, uri.lastSegment(),
							IContentDescription.ALL);
		}
		if (contentDescription == null) {
			return INVALID_CONTENT_DESCRIPTION;
		} else {
			Map<String, Object> result = createContentDescription(ContentHandler.Validity.VALID);
			result.put(IContentHandler.CONTENT_TYPE_PROPERTY,
					contentDescription.getContentType().getId());
			Set<String> requestedProperties = getRequestedProperties(options);
			if (requestedProperties != null) {
				for (String property : requestedProperties) {
					QualifiedName qualifiedName = getQualifiedName(property);
					if (qualifiedName != null) {
						Object value = getDescriptionValue(qualifiedName,
								contentDescription.getProperty(qualifiedName));
						if (value != null) {
							result.put(property, value);
						}
					}
				}
			}
			return result;
		}
	}
}
