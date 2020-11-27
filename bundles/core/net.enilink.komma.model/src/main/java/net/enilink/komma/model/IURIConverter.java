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
package net.enilink.komma.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.core.URI;
import net.enilink.komma.model.base.IURIMapRuleSet;
import net.enilink.komma.model.base.URIHandler;

import org.xml.sax.ContentHandler;

/**
 * A converter to normalize a URI or to produce an input or output stream for a
 * URI.
 * <p>
 * A model set provides {@link IModelSet#getURIConverter() one} of these for use
 * by it's {@link IModelSet#getModels models} when they are
 * {@link IModel#save(java.util.Map) serialized} and
 * {@link IModel#load(java.util.Map) deserialized}. A model set also uses this
 * directly when it {@link IModelSet#getModel looks up} a model: a model is
 * considered a match if {@link IModel#getURI it's URI}, and the URI being
 * looked up, {@link #normalize normalize} to {@link URI#equals(Object) equal}
 * URIs. Clients must extend the default implementation}, since methods can and
 * will be added to this API.
 * </p>
 * 
 * @see IModelSet#getURIConverter()
 * @see IURIHandler
 * @see IContentHandler
 */
public interface IURIConverter {
	/**
	 * An option used to pass the calling URIConverter to the {@link URIHandler}s.
	 */
	String OPTION_URI_CONVERTER = "URI_CONVERTER";

	/**
	 * An option to pass a {@link Map Map&lt;Object, Object>} to any of the URI
	 * converter's methods in order to yield results in addition to the returned
	 * value of the method.
	 */
	String OPTION_RESPONSE = "RESPONSE";

	/**
	 * A property of the {@link #OPTION_RESPONSE response option} used to yield
	 * the {@link #ATTRIBUTE_TIME_STAMP time stamp} associated with the creation
	 * of an {@link #createInputStream(URI, Map) input} or an
	 * {@link #createOutputStream(URI, Map) output} stream. This is typically
	 * used by resource {@link IModel#load(Map) load} and
	 * {@link IModel#save(Map) save} in order to set the
	 * {@link IModel#getTimeStamp()}.
	 */
	String RESPONSE_TIME_STAMP_PROPERTY = "TIME_STAMP";

	/**
	 * A property of the {@link #OPTION_RESPONSE response option} used to yield
	 * the {@link #ATTRIBUTE_MIME_TYPE MIME-type} associated with the creation
	 * of an {@link #createInputStream(URI, Map) input} or an
	 * {@link #createOutputStream(URI, Map) output} stream.
	 */
	String RESPONSE_MIME_TYPE_PROPERTY = "MIME_TYPE";

	/**
	 * Returns the normalized form of the URI.
	 * <p>
	 * This may, in theory, do absolutely anything. Default behaviour includes
	 * applying URI {@link IURIConverter#getURIMap mapping}, assuming
	 * <code>"file:"</code> protocol for a {@link URI#isRelative relative} URI
	 * with a {@link URI#hasRelativePath relative path}:
	 * 
	 * <pre>
	 *  ./WhateverDirectory/Whatever.file 
	 *    -&gt; 
	 *  file:./WhateverDirectory/Whatever.file
	 * </pre>
	 * 
	 * and assuming <code>"platform:/resource"</code> protocol for a relative
	 * URI with an {@link URI#hasAbsolutePath absolute path}:
	 * 
	 * <pre>
	 *  /WhateverRelocatableProject/Whatever.file 
	 *    -&gt; 
	 *  platform:/resource/WhateverRelocatableProject/Whatever.file
	 * </pre>
	 * 
	 * </p>
	 * <p>
	 * It is important to emphasize that normalization can result it loss of
	 * information. The normalized URI should generally be used only for
	 * comparison and for access to input or output streams.
	 * </p>
	 * 
	 * @param uri
	 *            the URI to normalize.
	 * @return the normalized form.
	 * @see org.eclipse.emf.ecore.plugin.EcorePlugin#getPlatformResourceMap
	 */
	URI normalize(URI uri);

	/**
	 * Returns the map used for remapping a logical URI to a physical URI when
	 * {@link #normalize normalizing}.
	 * <p>
	 * An implementation will typically also delegate to the
	 * {@link IURIConverter#URI_MAP global} map, so registrations made in this
	 * map are <em>local</em> to this URI converter, i.e., they augment or
	 * override those of the global map.
	 * </p>
	 * <p>
	 * The map generally specifies instance to instance mapping, except for the
	 * case that both the key URI and the value URI end with "/", which
	 * specifies a folder to folder mapping. A folder mapping will remap any URI
	 * that has the key as its {@link URI#replacePrefix prefix}, e.g., if the
	 * map contains:
	 * 
	 * <pre>
	 *  http://www.example.com/ -&gt; platform:/resource/example/
	 * </pre>
	 * 
	 * then the URI
	 * 
	 * <pre>
	 *  http://www.example.com/a/b/c.d
	 * </pre>
	 * 
	 * will map to
	 * 
	 * <pre>
	 *  platform:/resource/example/a/b/c.d
	 * </pre>
	 * 
	 * A matching instance mapping is considered first. If there isn't one, the
	 * folder mappings are considered starting with the
	 * {@link URI#segmentCount() longest} prefix.
	 * </p>
	 * 
	 * @see #normalize(URI)
	 * @see #URI_MAP
	 * @return the map used for remapping a logical URI to a physical URI.
	 */
	IURIMapRuleSet getURIMapRules();

	void setURIMapRules(IURIMapRuleSet uriMapRules);

	/**
	 * Returns the list of {@link IURIHandler}s.
	 * 
	 * @return the list of {@link IURIHandler}s.
	 */
	Collection<IURIHandler> getURIHandlers();

	/**
	 * Returns the first URI handler in the {@link #getURIHandler(URI) list} of
	 * URI handlers which {@link IURIHandler#canHandle(URI) can handle} the
	 * given URI.
	 * 
	 * @param uri
	 *            the URI for which to find a handler.
	 * @return the first URI handler in the list of URI handlers which can
	 *         handle the given URI.
	 * @throws RuntimeException
	 *             if no matching handler is found.
	 */
	IURIHandler getURIHandler(URI uri);

	/**
	 * Returns the list of {@link IContentHandler}s.
	 * 
	 * @return the list of {@link IContentHandler}s.
	 */
	Collection<IContentHandler> getContentHandlers();

	/**
	 * Creates an input stream for the URI and returns it; it has the same
	 * effect as calling {@link #createInputStream(URI, Map)
	 * createInputStream(uri, null)}.
	 * 
	 * @param uri
	 *            the URI for which to create the input stream.
	 * @return an open input stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open input stream.
	 * @see #createInputStream(URI, Map)
	 */
	InputStream createInputStream(URI uri) throws IOException;

	/**
	 * Creates an input stream for the URI and returns it.
	 * <p>
	 * It {@link #normalize normalizes} the URI and uses that as the basis for
	 * further processing. Special requirements, such as an Eclipse file
	 * refresh, are handled by the
	 * {@link net.enilink.komma.test.model.base.ExtensibleURIConverter.ecore.resource.impl.ExtensibleURIConverterImpl
	 * default implementation}.
	 * </p>
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
	 */
	InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException;

	/**
	 * Creates an output stream for the URI and returns it; it has the same
	 * effect as calling {@link #createOutputStream(URI, Map)
	 * createOutputStream(uri, null)}.
	 * 
	 * @return an open output stream.
	 * @exception IOException
	 *                if there is a problem obtaining an open output stream.
	 * @see #createOutputStream(URI, Map)
	 */
	OutputStream createOutputStream(URI uri) throws IOException;

	/**
	 * Creates an output stream for the URI and returns it.
	 * <p>
	 * It {@link #normalize normalizes} the URI and uses that as the basis for
	 * further processing. Special requirements, such as an Eclipse file
	 * refresh, are handled by the
	 * {@link net.enilink.komma.test.model.base.ExtensibleURIConverter.ecore.resource.impl.ExtensibleURIConverterImpl
	 * default implementation}.
	 * </p>
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
	 */
	boolean exists(URI uri, Map<?, ?> options);

	/**
	 * The MIME-type {@link #getAttributes(URI, Map) attribute} of the contents
	 * of a URI as String value.
	 */
	String ATTRIBUTE_MIME_TYPE = "mimeType";

	/**
	 * The time stamp {@link #getAttributes(URI, Map) attribute} representing
	 * the last time the contents of a URI were modified. The value is
	 * represented as Long that encodes the number of milliseconds since the
	 * epoch 00:00:00 GMT, January 1, 1970.
	 */
	String ATTRIBUTE_TIME_STAMP = "timeStamp";

	/**
	 * A {@link #ATTRIBUTE_TIME_STAMP} value that indicates no time stamp is
	 * available.
	 */
	long NULL_TIME_STAMP = -1;

	/**
	 * The length {@link #getAttributes(URI, Map) attribute} representing the
	 * number of bytes in the contents of a URI. It is represented as a Long
	 * value.
	 */
	String ATTRIBUTE_LENGTH = "length";

	/**
	 * The read only {@link #getAttributes(URI, Map) attribute} representing
	 * whether the contents of a URI can be modified. It is represented as a
	 * Boolean value. If the URI's contents {@link #exists(URI, Map) exist} and
	 * it is read only, it will not be possible to
	 * {@link #createOutputStream(URI, Map) create} an output stream.
	 */
	String ATTRIBUTE_READ_ONLY = "readOnly";

	/**
	 * The execute {@link #getAttributes(URI, Map) attribute} representing
	 * whether the contents of a URI can be executed. It is represented as a
	 * Boolean value.
	 */
	String ATTRIBUTE_EXECUTABLE = "executable";

	/**
	 * The archive {@link #getAttributes(URI, Map) attribute} representing
	 * whether the contents of a URI are archived. It is represented as a
	 * Boolean value.
	 */
	String ATTRIBUTE_ARCHIVE = "archive";

	/**
	 * The hidden {@link #getAttributes(URI, Map) attribute} representing
	 * whether the URI is visible. It is represented as a Boolean value.
	 */
	String ATTRIBUTE_HIDDEN = "hidden";

	/**
	 * The directory {@link #getAttributes(URI, Map) attribute} representing
	 * whether the URI represents a directory rather than a file. It is
	 * represented as a Boolean value.
	 */
	String ATTRIBUTE_DIRECTORY = "directory";

	/**
	 * An option passed as a {@link Set Set<String>} to
	 * {@link #getAttributes(URI, Map)} to indicate the specific attributes to
	 * be fetched.
	 */
	String OPTION_REQUESTED_ATTRIBUTES = "requestedAttributes";

	/**
	 * Returns a map from String attributes to their corresponding values
	 * representing information about various aspects of the URI's state. The
	 * {@link #OPTION_REQUESTED_ATTRIBUTES requested attributes option} can be
	 * used to specify which properties to fetch; without that option, all
	 * supported attributes will be fetched. If the URI doesn't not support any
	 * particular attribute, an entry for that attribute will not be appear in
	 * the result.
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
