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
package net.enilink.komma.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.xml.sax.ContentHandler;

import net.enilink.komma.core.URI;

/**
 * A handler for determining information about URI.
 * <p>
 * A URI handler is used primarily by a {@link IURIConverter URI converter} which
 * provides support for accessing and querying the contents of a URI by virtue
 * of having a {@link IURIConverter#getURIHandlers() list} of URI handlers that
 * it consults to determine whether the handler {@link #canHandle(URI) can
 * handle} the given URI and if so that it uses as a delegate.
 * </p>
 * 
 * @see IURIConverter
 * @see ContentHandler
 * @since 2.4
 */
public interface IURIHandler {

	/**
	 * Returns whether this handler is appropriate for the given URI.
	 * 
	 * @param uri
	 *            the URI to consider.
	 * @return whether this handler is appropriate for the given URI.
	 */
	boolean canHandle(URI uri);

	/**
	 * Creates an input stream for the URI and returns it.
	 * 
	 * @param uri
	 *            the URI for which to create the input stream.
	 * @param options
	 *            a map of options to influence the kind of stream that is
	 *            returned; unrecognized options are ignored and
	 *            <code>null</code> is permitted.
	 * @return an open input stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open input stream.
	 * @see IURIConverter#createInputStream(URI, Map)
	 */
	InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException;

	/**
	 * Creates an output stream for the URI and returns it.
	 * 
	 * @param uri
	 *            the URI for which to create the output stream.
	 * @param options
	 *            a map of options to influence the kind of stream that is
	 *            returned; unrecognized options are ignored and
	 *            <code>null</code> is permitted.
	 * @return an open output stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open output stream.
	 * @see IURIConverter#createOutputStream(URI, Map)
	 */
	OutputStream createOutputStream(URI uri, Map<?, ?> options)
			throws IOException;

	/**
	 * Deletes the contents of the given URI.
	 * 
	 * @param uri
	 *            the URI to consider.
	 * @param options
	 *            options to influence how the contents are deleted, or
	 *            <code>null</code> if there are no options.
	 * @throws IOException
	 *             if there is a problem deleting the contents.
	 * @see IURIConverter#delete(URI, Map)
	 */
	void delete(URI uri, Map<?, ?> options) throws IOException;

	/**
	 * Returns a map from String properties to their corresponding values
	 * representing a description the given URI's contents. See the
	 * {@link ContentHandler#contentDescription(URI, InputStream, Map, Map)
	 * content handler} for more details.
	 * 
	 * @param uri
	 *            the URI to consider.
	 * @param options
	 *            options to influence how the content description is
	 *            determined, or <code>null</code> if there are no options.
	 * @return a map from String properties to their corresponding values
	 *         representing a description the given URI's contents.
	 * @throws IOException
	 *             if there is a problem accessing the contents.
	 * @see IURIConverter#contentDescription(URI, Map)
	 * @see ContentHandler#contentDescription(URI, InputStream, Map, Map)
	 */
	Map<String, ?> contentDescription(URI uri, Map<?, ?> options)
			throws IOException;

	/**
	 * Returns whether the given URI has contents. If the URI
	 * {@link #exists(URI, Map) exists} it will be possible to
	 * {@link #createOutputStream(URI, Map) create} an input stream.
	 * 
	 * @param uri
	 *            the URI to consider.
	 * @param options
	 *            options to influence how the existence determined, or
	 *            <code>null</code> if there are no options.
	 * @return whether the given URI has contents.
	 * @see IURIConverter#exists(URI, Map)
	 */
	boolean exists(URI uri, Map<?, ?> options);

	/**
	 * Returns a map from String attributes to their corresponding values
	 * representing information about various aspects of the URI's state. The
	 * {@link IURIConverter#OPTION_REQUESTED_ATTRIBUTES requested attributes
	 * option} can be used to specify which properties to fetch; without that
	 * option, all supported attributes will be fetched. If the URI doesn't not
	 * support any particular attribute, an entry for that attribute will not be
	 * appear in the result.
	 * 
	 * @param uri
	 *            the URI to consider.
	 * @param options
	 *            options to influence how the attributes are determined, or
	 *            <code>null</code> if there are no options.
	 * @return a map from String attributes to their corresponding values
	 *         representing information about various aspects of the URI's
	 *         state.
	 */
	Map<String, ?> getAttributes(URI uri, Map<?, ?> options);

	/**
	 * Updates the map from String attributes to their corresponding values
	 * representing information about various aspects of the URI's state.
	 * Unsupported or unchangeable attributes are ignored.
	 * 
	 * @param uri
	 *            the URI to consider.
	 * @param attributes
	 *            the new values for the attributes.
	 * @param options
	 *            options to influence how the attributes are updated, or
	 *            <code>null</code> if there are no options.
	 * @throws IOException
	 *             if there is a problem updating the attributes.
	 */
	void setAttributes(URI uri, Map<String, ?> attributes, Map<?, ?> options)
			throws IOException;
}
