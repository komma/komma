/**
 * <copyright>
 *
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 */
package net.enilink.komma.model.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.core.URI;

public class FileURIHandler extends URIHandler {
	@Override
	public boolean canHandle(URI uri) {
		return uri.isFile();
	}

	/**
	 * Creates an output stream for the file path and returns it.
	 * <p>
	 * This implementation allocates a {@link FileOutputStream} and creates
	 * subdirectories as necessary.
	 * </p>
	 * 
	 * @return an open output stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open output stream.
	 */
	@Override
	public OutputStream createOutputStream(URI uri, Map<?, ?> options)
			throws IOException {
		String filePath = uri.toFileString();
		final File file = new File(filePath);
		String parent = file.getParent();
		if (parent != null) {
			new File(parent).mkdirs();
		}
		final Map<Object, Object> response = getResponse(options);
		OutputStream outputStream = new FileOutputStream(file) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					if (response != null) {
						response.put(
								IURIConverter.RESPONSE_TIME_STAMP_PROPERTY,
								file.lastModified());
					}
				}
			}
		};
		return outputStream;
	}

	/**
	 * Creates an input stream for the file path and returns it.
	 * <p>
	 * This implementation allocates a {@link FileInputStream}.
	 * </p>
	 * 
	 * @return an open input stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open input stream.
	 */
	@Override
	public InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException {
		String filePath = uri.toFileString();
		File file = new File(filePath);
		InputStream inputStream = new FileInputStream(file);
		Map<Object, Object> response = getResponse(options);
		if (response != null) {
			response.put(IURIConverter.RESPONSE_TIME_STAMP_PROPERTY,
					file.lastModified());
		}
		return inputStream;
	}

	@Override
	public void delete(URI uri, Map<?, ?> options) throws IOException {
		String filePath = uri.toFileString();
		File file = new File(filePath);
		file.delete();
	}

	@Override
	public boolean exists(URI uri, Map<?, ?> options) {
		String filePath = uri.toFileString();
		File file = new File(filePath);
		return file.exists();
	}

	@Override
	public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
		Map<String, Object> result = new HashMap<String, Object>();
		String filePath = uri.toFileString();
		File file = new File(filePath);
		if (file.exists()) {
			Set<String> requestedAttributes = getRequestedAttributes(options);
			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_TIME_STAMP)) {
				result.put(IURIConverter.ATTRIBUTE_TIME_STAMP,
						file.lastModified());
			}
			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_LENGTH)) {
				result.put(IURIConverter.ATTRIBUTE_LENGTH, file.length());
			}
			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_READ_ONLY)) {
				result.put(IURIConverter.ATTRIBUTE_READ_ONLY, !file.canWrite());
			}
			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_HIDDEN)) {
				result.put(IURIConverter.ATTRIBUTE_HIDDEN, file.isHidden());
			}
			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_DIRECTORY)) {
				result.put(IURIConverter.ATTRIBUTE_DIRECTORY,
						file.isDirectory());
			}
		}
		return result;
	}

	@Override
	public void setAttributes(URI uri, Map<String, ?> attributes,
			Map<?, ?> options) throws IOException {
		String filePath = uri.toFileString();
		File file = new File(filePath);
		if (file.exists()) {
			Long timeStamp = (Long) attributes
					.get(IURIConverter.ATTRIBUTE_TIME_STAMP);
			if (timeStamp != null && !file.setLastModified(timeStamp)) {
				throw new IOException(
						"Could not set the timestamp for the file '" + file
								+ "'");
			}
			Boolean isReadOnly = (Boolean) attributes
					.get(IURIConverter.ATTRIBUTE_READ_ONLY);
			if (Boolean.TRUE.equals(isReadOnly) && !file.setReadOnly()) {
				throw new IOException("Could not set the file '" + file
						+ "' to be read only");
			}
		} else {
			throw new FileNotFoundException("The file '" + file
					+ "' does not exist");
		}
	}
}
