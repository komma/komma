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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.enilink.komma.core.URI;
import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.IURIHandler;
import net.enilink.komma.model.ModelUtil;

/**
 * An implementation of a {@link IURIHandler URI handler}.
 *
 */
public class URIHandler implements IURIHandler {

	private final static int TIMEOUT_HARD_IN_MS = 5000;
	private final static int TIMEOUT_SOFT_IN_MS = TIMEOUT_HARD_IN_MS - 100;

	/**
	 * This implementation always returns true; clients are generally expected
	 * to override this.
	 */
	public boolean canHandle(URI uri) {
		return true;
	}

	/**
	 * Returns the value of the {@link URIConverter#OPTION_URI_CONVERTER URI
	 * converter option}.
	 * 
	 * @param options
	 *            the options in which to look for the URI converter.
	 * @return the value of the URI converter option.
	 */
	protected IURIConverter getURIConverter(Map<?, ?> options) {
		return (IURIConverter) options.get(IURIConverter.OPTION_URI_CONVERTER);
	}

	/**
	 * Returns the value of the {@link URIConverter#OPTION_RESPONSE response
	 * option}.
	 * 
	 * @param options
	 *            the options in which to look for the response option.
	 * @return the value of the response option.
	 */
	@SuppressWarnings("unchecked")
	protected Map<Object, Object> getResponse(Map<?, ?> options) {
		return (Map<Object, Object>) options.get(IURIConverter.OPTION_RESPONSE);
	}

	/**
	 * Returns the value of the {@link URIConverter#OPTION_REQUESTED_ATTRIBUTES
	 * requested attributes option}.
	 * 
	 * @param options
	 *            the options in which to look for the requested attributes
	 *            option.
	 * @return the value of the requested attributes option.
	 */
	@SuppressWarnings("unchecked")
	protected Set<String> getRequestedAttributes(Map<?, ?> options) {
		return (Set<String>) options
				.get(IURIConverter.OPTION_REQUESTED_ATTRIBUTES);
	}

	/**
	 * Creates an output stream for the URI, assuming it's a URL, and returns
	 * it. Specialized support is provided for HTTP URLs.
	 * 
	 * @return an open output stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open output stream.
	 */
	public OutputStream createOutputStream(URI uri, Map<?, ?> options)
			throws IOException {
		try {
			URL url = new URL(uri.toString());
			final URLConnection urlConnection = url.openConnection();
			urlConnection.setDoOutput(true);
			if (urlConnection instanceof HttpURLConnection) {
				final HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
				httpURLConnection.setRequestMethod("PUT");
				return new FilterOutputStream(urlConnection.getOutputStream()) {
					@Override
					public void close() throws IOException {
						super.close();
						int responseCode;
						try {
							responseCode = getResponseCode(httpURLConnection);
						} catch (InterruptedException | ExecutionException
								| TimeoutException e) {
							throw new IOException(e);
						}
						switch (responseCode) {
						case HttpURLConnection.HTTP_OK:
						case HttpURLConnection.HTTP_CREATED:
						case HttpURLConnection.HTTP_NO_CONTENT: {
							break;
						}
						default: {
							throw new IOException(
									"PUT failed with HTTP response code "
											+ responseCode);
						}
						}
					}
				};
			} else {
				OutputStream result = urlConnection.getOutputStream();
				final Map<Object, Object> response = getResponse(options);
				if (response != null) {
					result = new FilterOutputStream(result) {
						@Override
						public void close() throws IOException {
							try {
								super.close();
							} finally {
								response.put(
										IURIConverter.RESPONSE_TIME_STAMP_PROPERTY,
										urlConnection.getLastModified());
							}
						}
					};
				}
				return result;
			}
		} catch (RuntimeException exception) {
			throw new IModel.IOWrappedException(exception);
		}
	}

	protected String acceptHeader() {
		StringBuilder accept = new StringBuilder();
		for (Map.Entry<String, Double> mimeType : ModelUtil
				.getSupportedMimeTypes().entrySet()) {
			if (accept.length() > 0) {
				accept.append(", ");
			}
			accept.append(mimeType.getKey()).append("; q=")
					.append(String.format("%.2f", mimeType.getValue()));
		}
		return accept.toString();
	}

	/**
	 * Creates an input stream for the URI, assuming it's a URL, and returns it.
	 *
	 * @return an open input stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open input stream.
	 */

	public InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException {
		try {
			URL url = new URL(uri.toString());
			final URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Accept", acceptHeader());
			InputStream result = getInputStream(urlConnection);
			Map<Object, Object> response = getResponse(options);
			if (response != null) {
				response.put(IURIConverter.RESPONSE_TIME_STAMP_PROPERTY,
						urlConnection.getLastModified());
				if (urlConnection.getContentType() != null) {
					response.put(
							IURIConverter.RESPONSE_MIME_TYPE_PROPERTY,
							urlConnection.getContentType()
									.replaceAll(";.*$", "").trim());
				}
			}
			return result;
		} catch (RuntimeException | ExecutionException | InterruptedException
				| TimeoutException exception) {
			throw new IModel.IOWrappedException(exception);
		}
	}

	/**
	 * Only HTTP connections support delete.
	 */
	public void delete(URI uri, Map<?, ?> options) throws IOException {
		try {
			URL url = new URL(uri.toString());
			URLConnection urlConnection = url.openConnection();
			urlConnection.setDoOutput(true);
			if (urlConnection instanceof HttpURLConnection) {
				final HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
				httpURLConnection.setRequestMethod("DELETE");
				int responseCode = getResponseCode(httpURLConnection);
				switch (responseCode) {
				case HttpURLConnection.HTTP_OK:
				case HttpURLConnection.HTTP_ACCEPTED:
				case HttpURLConnection.HTTP_NO_CONTENT: {
					break;
				}
				default: {
					throw new IOException(
							"DELETE failed with HTTP response code "
									+ responseCode);
				}
				}
			} else {
				throw new IOException("Delete is not supported for " + uri);
			}
		} catch (RuntimeException | ExecutionException | InterruptedException
				| TimeoutException exception) {
			throw new IModel.IOWrappedException(exception);
		}
	}

	/**
	 * This implementation delegates to the {@link #getURIConverter(Map) URI
	 * converter}'s {@link URIConverter#getContentHandlers() content handlers}.
	 */
	public Map<String, ?> contentDescription(URI uri, Map<?, ?> options)
			throws IOException {
		IURIConverter uriConverter = (IURIConverter) options
				.get(IURIConverter.OPTION_URI_CONVERTER);
		InputStream inputStream = null;
		Map<String, ?> result = null;
		Map<Object, Object> context = new HashMap<Object, Object>();
		try {
			for (IContentHandler contentHandler : uriConverter
					.getContentHandlers()) {
				if (contentHandler.canHandle(uri)) {
					if (inputStream == null) {
						try {
							inputStream = createInputStream(uri, options);
						} catch (IOException exception) {
							inputStream = new ByteArrayInputStream(new byte[0]);
						}
						if (!inputStream.markSupported()) {
							inputStream = new BufferedInputStream(inputStream);
						}
						inputStream.mark(Integer.MAX_VALUE);
					} else {
						inputStream.reset();
					}
					context.put(IURIConverter.ATTRIBUTE_MIME_TYPE, options
							.get(IURIConverter.RESPONSE_MIME_TYPE_PROPERTY));
					Map<String, ?> contentDescription = contentHandler
							.contentDescription(uri, inputStream, options,
									context);
					switch ((IContentHandler.Validity) contentDescription
							.get(IContentHandler.VALIDITY_PROPERTY)) {
					case VALID: {
						return contentDescription;
					}
					case INDETERMINATE: {
						if (result == null) {
							result = contentDescription;
						}
						break;
					}
					case INVALID: {
						break;
					}
					}
				}
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		return result == null ? IContentHandler.INVALID_CONTENT_DESCRIPTION
				: result;
	}

	/**
	 * If a stream can be created the file exists. Specialized support is
	 * provided for HTTP connections to avoid fetching the whole stream in that
	 * case.
	 */
	public boolean exists(URI uri, Map<?, ?> options) {
		try {
			URL url = new URL(uri.toString());
			URLConnection urlConnection = url.openConnection();
			urlConnection.setRequestProperty("Accept", acceptHeader());
			if (urlConnection instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
				httpURLConnection.setRequestMethod("HEAD");
				int responseCode = getResponseCode(httpURLConnection);
				// TODO
				// I'm concerned that folders will often return 401 or even 403.
				// So should we consider something to exist even though access
				// if unauthorized or forbidden?
				//
				return responseCode == HttpURLConnection.HTTP_OK;
			} else {
				InputStream inputStream = urlConnection.getInputStream();
				inputStream.close();
				return true;
			}
		} catch (Throwable exception) {
			return false;
		}
	}

	public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
		Map<String, Object> result = new HashMap<String, Object>();
		Set<String> requestedAttributes = getRequestedAttributes(options);
		try {
			URL url = new URL(uri.toString());
			URLConnection urlConnection = null;
			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_READ_ONLY)) {
				urlConnection = url.openConnection();
				urlConnection.setRequestProperty("Accept", acceptHeader());
				if (urlConnection instanceof HttpURLConnection) {
					HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
					httpURLConnection.setRequestMethod("OPTIONS");
					int responseCode = getResponseCode(httpURLConnection);
					if (responseCode == HttpURLConnection.HTTP_OK) {
						String allow = httpURLConnection
								.getHeaderField("Allow");
						result.put(IURIConverter.ATTRIBUTE_READ_ONLY,
								allow == null || !allow.contains("PUT"));
					}
					urlConnection = null;
				} else {
					result.put(IURIConverter.ATTRIBUTE_READ_ONLY, true);
				}
			}

			if (requestedAttributes == null
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_TIME_STAMP)
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_LENGTH)
					|| requestedAttributes
							.contains(IURIConverter.ATTRIBUTE_MIME_TYPE)) {
				if (urlConnection == null) {
					urlConnection = url.openConnection();
					urlConnection.setRequestProperty("Accept", acceptHeader());
					if (urlConnection instanceof HttpURLConnection) {
						HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
						httpURLConnection.setRequestMethod("HEAD");
						getResponseCode(httpURLConnection);
					}
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_TIME_STAMP)) {
					if (urlConnection.getHeaderField("last-modified") != null) {
						result.put(IURIConverter.ATTRIBUTE_TIME_STAMP,
								urlConnection.getLastModified());
					}
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_LENGTH)) {
					if (urlConnection.getHeaderField("content-length") != null) {
						result.put(IURIConverter.ATTRIBUTE_LENGTH,
								urlConnection.getContentLength());
					}
				}
				if (requestedAttributes == null
						|| requestedAttributes
								.contains(IURIConverter.ATTRIBUTE_MIME_TYPE)) {
					String contentType = urlConnection.getContentType();
					if (contentType != null) {
						result.put(IURIConverter.ATTRIBUTE_MIME_TYPE,
								contentType.replaceAll(";.*$", "").trim());
					}
				}
			}
		} catch (IOException | ExecutionException | InterruptedException
				| TimeoutException exception) {
			// Ignore exceptions.
		}
		return result;
	}

	private int getResponseCode(final HttpURLConnection connection)
			throws InterruptedException, ExecutionException, TimeoutException {
		FutureTask<Integer> futureTask = new FutureTask<>(
				new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						setupTimeout(connection);
						return connection.getResponseCode();
					}
				});
		new Thread(futureTask).start();

		return futureTask.get(TIMEOUT_HARD_IN_MS, TimeUnit.MILLISECONDS);
	}

	private InputStream getInputStream(final URLConnection connection)
			throws InterruptedException, ExecutionException, TimeoutException {
		FutureTask<InputStream> futureTask = new FutureTask<>(
				new Callable<InputStream>() {
					@Override
					public InputStream call() throws Exception {
						setupTimeout(connection);
						return connection.getInputStream();
					}
				});
		new Thread(futureTask).start();

		return futureTask.get(TIMEOUT_HARD_IN_MS, TimeUnit.MILLISECONDS);
	}

	private void setupTimeout(URLConnection connection) {
		connection.setConnectTimeout(TIMEOUT_SOFT_IN_MS);
		connection.setReadTimeout(TIMEOUT_SOFT_IN_MS);
	}

	public void setAttributes(URI uri, Map<String, ?> attributes,
			Map<?, ?> options) throws IOException {
		// We can't update any properties via just a URL connection.
	}
}
