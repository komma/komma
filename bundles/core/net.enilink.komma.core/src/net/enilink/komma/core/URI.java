/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

import java.util.List;

public interface URI extends IReference {

	/**
	 * When specified as the last argument to
	 * {@link #createURI(String, boolean, int) createURI}, indicates that there
	 * is no fragment, so any <code>#</code> characters should be encoded.
	 * 
	 * @see #createURI(String, boolean, int)
	 */
	public static final int FRAGMENT_NONE = 0;
	/**
	 * When specified as the last argument to
	 * {@link #createURI(String, boolean, int) createURI}, indicates that the
	 * first <code>#</code> character should be taken as the fragment separator,
	 * and any others should be encoded.
	 * 
	 * @see #createURI(String, boolean, int)
	 */
	public static final int FRAGMENT_FIRST_SEPARATOR = 1;
	/**
	 * When specified as the last argument to
	 * {@link #createURI(String, boolean, int) createURI}, indicates that the
	 * last <code>#</code> character should be taken as the fragment separator,
	 * and any others should be encoded.
	 * 
	 * @see #createURI(String, boolean, int)
	 */
	public static final int FRAGMENT_LAST_SEPARATOR = 2;

	/**
	 * Returns <code>true</code> if this is a relative URI, or
	 * <code>false</code> if it is an absolute URI.
	 */
	boolean isRelative();

	/**
	 * Returns <code>true</code> if this a a hierarchical URI, or
	 * <code>false</code> if it is of the generic form.
	 */
	boolean isHierarchical();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with an authority
	 * component; <code>false</code> otherwise.
	 */
	boolean hasAuthority();

	/**
	 * Returns <code>true</code> if this is a non-hierarchical URI with an
	 * opaque part component; <code>false</code> otherwise.
	 */
	boolean hasOpaquePart();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with a device
	 * component; <code>false</code> otherwise.
	 */
	boolean hasDevice();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with an absolute
	 * or relative path; <code>false</code> otherwise.
	 */
	boolean hasPath();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with an absolute
	 * path, or <code>false</code> if it is non-hierarchical, has no path, or
	 * has a relative path.
	 */
	boolean hasAbsolutePath();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with a relative
	 * path, or <code>false</code> if it is non-hierarchical, has no path, or
	 * has an absolute path.
	 */
	boolean hasRelativePath();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with an empty
	 * relative path; <code>false</code> otherwise.
	 * 
	 * <p>
	 * Note that <code>!hasEmpty()</code> does <em>not</em> imply that this URI
	 * has any path segments; however, <code>hasRelativePath &&
	 * !hasEmptyPath()</code> does.
	 */
	boolean hasEmptyPath();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI with a query
	 * component; <code>false</code> otherwise.
	 */
	boolean hasQuery();

	/**
	 * Returns <code>true</code> if this URI has a fragment component;
	 * <code>false</code> otherwise.
	 */
	boolean hasFragment();

	/**
	 * Returns <code>true</code> if this is a current document reference; that
	 * is, if it is a relative hierarchical URI with no authority, device or
	 * query components, and no path segments; <code>false</code> is returned
	 * otherwise.
	 */
	boolean isCurrentDocumentReference();

	/**
	 * Returns <code>true</code> if this is a
	 * {@link #isCurrentDocumentReference() current document reference} with no
	 * fragment component; <code>false</code> otherwise.
	 * 
	 * @see #isCurrentDocumentReference()
	 */
	boolean isEmpty();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI that may refer
	 * directly to a locally accessible file. This is considered to be the case
	 * for a file-scheme absolute URI, or for a relative URI with no query;
	 * <code>false</code> is returned otherwise.
	 */
	boolean isFile();

	/**
	 * Returns <code>true</code> if this is a platform URI, that is, an
	 * absolute, hierarchical URI, with "platform" scheme, no authority, and at
	 * least two segments; <code>false</code> is returned otherwise.
	 * 
	 * @since org.eclipse.emf.common 2.3
	 */
	boolean isPlatform();

	/**
	 * Returns <code>true</code> if this is a platform resource URI, that is, a
	 * {@link #isPlatform platform URI} whose first segment is "resource";
	 * <code>false</code> is returned otherwise.
	 * 
	 * @see #isPlatform
	 * @since org.eclipse.emf.common 2.3
	 */
	boolean isPlatformResource();

	/**
	 * Returns <code>true</code> if this is a platform plug-in URI, that is, a
	 * {@link #isPlatform platform URI} whose first segment is "plugin";
	 * <code>false</code> is returned otherwise.
	 * 
	 * @see #isPlatform
	 * @since org.eclipse.emf.common 2.3
	 */
	boolean isPlatformPlugin();

	/**
	 * Returns <code>true</code> if this is an archive URI. If so, it is also
	 * hierarchical, with an authority (consisting of an absolute URI followed
	 * by "!"), no device, and an absolute path.
	 */
	boolean isArchive();

	/**
	 * Returns <code>true</code> if <code>object</code> is an instance of
	 * <code>URI</code> equal to this one; <code>false</code> otherwise.
	 * 
	 * <p>
	 * Equality is determined strictly by comparing components, not by
	 * attempting to interpret what resource is being identified. The comparison
	 * of schemes is case-insensitive.
	 */
	boolean equals(Object object);

	/**
	 * If this is an absolute URI, returns the scheme component;
	 * <code>null</code> otherwise.
	 */
	String scheme();

	/**
	 * If this is a non-hierarchical URI, returns the opaque part component;
	 * <code>null</code> otherwise.
	 */
	String opaquePart();

	/**
	 * If this is a hierarchical URI with an authority component, returns it;
	 * <code>null</code> otherwise.
	 */
	String authority();

	/**
	 * If this is a hierarchical URI with an authority component that has a user
	 * info portion, returns it; <code>null</code> otherwise.
	 */
	String userInfo();

	/**
	 * If this is a hierarchical URI with an authority component that has a host
	 * portion, returns it; <code>null</code> otherwise.
	 */
	String host();

	/**
	 * If this is a hierarchical URI with an authority component that has a port
	 * portion, returns it; <code>null</code> otherwise.
	 */
	String port();

	/**
	 * If this is a hierarchical URI with a device component, returns it;
	 * <code>null</code> otherwise.
	 */
	String device();

	/**
	 * If this is a hierarchical URI with a path, returns an array containing
	 * the segments of the path; an empty array otherwise. The leading separator
	 * in an absolute path is not represented in this array, but a trailing
	 * separator is represented by an empty-string segment as the final element.
	 */
	String[] segments();

	/**
	 * Returns an unmodifiable list containing the same segments as the array
	 * returned by {@link #segments segments}.
	 */
	List<String> segmentsList();

	/**
	 * Returns the number of elements in the segment array that would be
	 * returned by {@link #segments segments}.
	 */
	int segmentCount();

	/**
	 * Provides fast, indexed access to individual segments in the path segment
	 * array.
	 * 
	 * @exception java.lang.IndexOutOfBoundsException
	 *                if <code>i < 0</code> or <code>i >= segmentCount()</code>.
	 */
	String segment(int i);

	/**
	 * Returns the last segment in the segment array, or <code>null</code>.
	 */
	String lastSegment();

	/**
	 * If this is a hierarchical URI with a path, returns a string
	 * representation of the path; <code>null</code> otherwise. The path
	 * consists of a leading segment separator character (a slash), if the path
	 * is absolute, followed by the slash-separated path segments. If this URI
	 * has a separate <a href="#device_explanation">device component</a>, it is
	 * <em>not</em> included in the path.
	 */
	String path();

	/**
	 * If this is a hierarchical URI with a path, returns a string
	 * representation of the path, including the authority and the <a
	 * href="#device_explanation">device component</a>; <code>null</code>
	 * otherwise.
	 * 
	 * <p>
	 * If there is no authority, the format of this string is:
	 * 
	 * <pre>
	 *   device/pathSegment1/pathSegment2...
	 * </pre>
	 * 
	 * <p>
	 * If there is an authority, it is:
	 * 
	 * <pre>
	 * // authority/device/pathSegment1/pathSegment2...
	 * </pre>
	 * 
	 * <p>
	 * For an <a href="#archive_explanation">archive URI</a>, it's just:
	 * 
	 * <pre>
	 *   authority/pathSegment1/pathSegment2...
	 * </pre>
	 */
	String devicePath();

	/**
	 * If this is a hierarchical URI with a query component, returns it;
	 * <code>null</code> otherwise.
	 */
	String query();

	/**
	 * Returns the URI formed from this URI and the given query.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>query</code> is not a valid query (portion)
	 *                according to {@link #validQuery validQuery}.
	 */
	URI appendQuery(String query);

	/**
	 * If this URI has a non-null {@link #query query}, returns the URI formed
	 * by removing it; this URI unchanged, otherwise.
	 */
	URI trimQuery();

	/**
	 * If this URI has a fragment component, returns it; <code>null</code>
	 * otherwise.
	 */
	String fragment();

	/**
	 * Returns the URI formed from this URI and the given fragment.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>fragment</code> is not a valid fragment (portion)
	 *                according to {@link #validFragment validFragment}.
	 */
	URI appendFragment(String fragment);

	/**
	 * If this URI has a non-null {@link #fragment fragment}, returns the URI
	 * formed by removing it; this URI unchanged, otherwise.
	 */
	URI trimFragment();

	/**
	 * If this URI has a non-null {@link #fragment fragment}, returns the URI
	 * formed by removing it while keeping the '#' symbol; If this URI has no
	 * fragment {@link #fragment fragment}, returns the URI without its last
	 * segment; this URI unchanged, otherwise.
	 */
	URI namespace();

	/**
	 * Returns the URI formed from this URI and the given local part.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>localPart</code> is not a valid segment according
	 *                to {@link #validSegment} or a valid fragment (portion)
	 *                according to {@link #validFragment}.
	 */
	URI appendLocalPart(String localPart);

	/**
	 * Returns the local part of this URI.
	 * 
	 * <ul>
	 * <li>If the URI has a fragment component then return it, else
	 * <li>If the URI has a path then return the last path segment,
	 * <li>If this fails, split after the last occurrence of the ':' character.
	 * </ul>
	 */
	String localPart();

	/**
	 * Resolves this URI reference against a <code>base</code> absolute
	 * hierarchical URI, returning the resulting absolute URI. If already
	 * absolute, the URI itself is returned. URI resolution is described in
	 * detail in section 5.2 of <a
	 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>,
	 * "Resolving Relative References to Absolute Form."
	 * 
	 * <p>
	 * During resolution, empty segments, self references ("."), and parent
	 * references ("..") are interpreted, so that they can be removed from the
	 * path. Step 6(g) gives a choice of how to handle the case where parent
	 * references point to a path above the root: the offending segments can be
	 * preserved or discarded. This method preserves them. To have them
	 * discarded, please use the two-parameter form of
	 * {@link #resolve(URI, boolean) resolve}.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>base</code> is non-hierarchical or is relative.
	 */
	URI resolve(URI base);

	/**
	 * Resolves this URI reference against a <code>base</code> absolute
	 * hierarchical URI, returning the resulting absolute URI. If already
	 * absolute, the URI itself is returned. URI resolution is described in
	 * detail in section 5.2 of <a
	 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>,
	 * "Resolving Relative References to Absolute Form."
	 * 
	 * <p>
	 * During resolution, empty segments, self references ("."), and parent
	 * references ("..") are interpreted, so that they can be removed from the
	 * path. Step 6(g) gives a choice of how to handle the case where parent
	 * references point to a path above the root: the offending segments can be
	 * preserved or discarded. This method can do either.
	 * 
	 * @param preserveRootParents
	 *            <code>true</code> if segments referring to the parent of the
	 *            root path are to be preserved; <code>false</code> if they are
	 *            to be discarded.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>base</code> is non-hierarchical or is relative.
	 */
	URI resolve(URI base, boolean preserveRootParents);

	/**
	 * Finds the shortest relative or, if necessary, the absolute URI that, when
	 * resolved against the given <code>base</code> absolute hierarchical URI
	 * using {@link #resolve(URI) resolve}, will yield this absolute URI.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>base</code> is non-hierarchical or is relative.
	 * @exception java.lang.IllegalStateException
	 *                if <code>this</code> is relative.
	 */
	URI deresolve(URI base);

	/**
	 * Finds an absolute URI that, when resolved against the given
	 * <code>base</code> absolute hierarchical URI using
	 * {@link #resolve(URI, boolean) resolve}, will yield this absolute URI.
	 * 
	 * @param preserveRootParents
	 *            the boolean argument to <code>resolve(URI,
	 * boolean)</code> for which the returned URI should resolve to this URI.
	 * @param anyRelPath
	 *            if <code>true</code>, the returned URI's path (if any) will be
	 *            relative, if possible. If <code>false</code>, the form of the
	 *            result's path will depend upon the next parameter.
	 * @param shorterRelPath
	 *            if <code>anyRelPath</code> is <code>false</code> and this
	 *            parameter is <code>true</code>, the returned URI's path (if
	 *            any) will be relative, if one can be found that is no longer
	 *            (by number of segments) than the absolute path. If both
	 *            <code>anyRelPath</code> and this parameter are
	 *            <code>false</code>, it will be absolute.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>base</code> is non-hierarchical or is relative.
	 * @exception java.lang.IllegalStateException
	 *                if <code>this</code> is relative.
	 */
	URI deresolve(URI base, boolean preserveRootParents, boolean anyRelPath,
			boolean shorterRelPath);

	/**
	 * If this URI may refer directly to a locally accessible file, as
	 * determined by {@link #isFile isFile}, {@link #decode decodes} and formats
	 * the URI as a pathname to that file; returns null otherwise.
	 * 
	 * <p>
	 * If there is no authority, the format of this string is:
	 * 
	 * <pre>
	 *   device/pathSegment1/pathSegment2...
	 * </pre>
	 * 
	 * <p>
	 * If there is an authority, it is:
	 * 
	 * <pre>
	 * // authority/device/pathSegment1/pathSegment2...
	 * </pre>
	 * 
	 * <p>
	 * However, the character used as a separator is system-dependent and
	 * obtained from {@link java.io.File#separatorChar}.
	 */
	String toFileString();

	/**
	 * If this is a platform URI, as determined by {@link #isPlatform}, returns
	 * the workspace-relative or plug-in-based path to the resource, optionally
	 * {@link #decode decoding} the segments in the process.
	 * 
	 * @see #createPlatformResourceURI(String, boolean)
	 * @see #createPlatformPluginURI
	 * @since org.eclipse.emf.common 2.3
	 */
	String toPlatformString(boolean decode);

	/**
	 * Returns the URI formed by appending the specified segment on to the end
	 * of the path of this URI, if hierarchical; this URI unchanged, otherwise.
	 * If this URI has an authority and/or device, but no path, the segment
	 * becomes the first under the root in an absolute path.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>segment</code> is not a valid segment according
	 *                to {@link #validSegment}.
	 */
	URI appendSegment(String segment);

	/**
	 * Returns the URI formed by appending the specified segments on to the end
	 * of the path of this URI, if hierarchical; this URI unchanged, otherwise.
	 * If this URI has an authority and/or device, but no path, the segments are
	 * made to form an absolute path.
	 * 
	 * @param segments
	 *            an array of non-null strings, each representing one segment of
	 *            the path. If desired, a trailing separator should be
	 *            represented by an empty-string segment as the last element of
	 *            the array.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>segments</code> is not a valid segment array
	 *                according to {@link #validSegments}.
	 */
	URI appendSegments(String[] segments);

	/**
	 * Returns the URI formed by trimming the specified number of segments
	 * (including empty segments, such as one representing a trailing separator)
	 * from the end of the path of this URI, if hierarchical; otherwise, this
	 * URI is returned unchanged.
	 * 
	 * <p>
	 * Note that if all segments are trimmed from an absolute path, the root
	 * absolute path remains.
	 * 
	 * @param i
	 *            the number of segments to be trimmed in the returned URI. If
	 *            less than 1, this URI is returned unchanged; if equal to or
	 *            greater than the number of segments in this URI's path, all
	 *            segments are trimmed.
	 */
	URI trimSegments(int i);

	/**
	 * Returns <code>true</code> if this is a hierarchical URI that has a path
	 * that ends with a trailing separator; <code>false</code> otherwise.
	 * 
	 * <p>
	 * A trailing separator is represented as an empty segment as the last
	 * segment in the path; note that this definition does <em>not</em> include
	 * the lone separator in the root absolute path.
	 */
	boolean hasTrailingPathSeparator();

	/**
	 * If this is a hierarchical URI whose path includes a file extension, that
	 * file extension is returned; null otherwise. We define a file extension as
	 * any string following the last period (".") in the final path segment. If
	 * there is no path, the path ends in a trailing separator, or the final
	 * segment contains no period, then we consider there to be no file
	 * extension. If the final segment ends in a period, then the file extension
	 * is an empty string.
	 */
	String fileExtension();

	/**
	 * Returns the URI formed by appending a period (".") followed by the
	 * specified file extension to the last path segment of this URI, if it is
	 * hierarchical with a non-empty path ending in a non-empty segment;
	 * otherwise, this URI is returned unchanged.
	 * 
	 * <p>
	 * The extension is appended regardless of whether the segment already
	 * contains an extension.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>fileExtension</code> is not a valid segment
	 *                (portion) according to {@link #validSegment}.
	 */
	URI appendFileExtension(String fileExtension);

	/**
	 * If this URI has a non-null {@link #fileExtension fileExtension}, returns
	 * the URI formed by removing it; this URI unchanged, otherwise.
	 */
	URI trimFileExtension();

	/**
	 * Returns <code>true</code> if this is a hierarchical URI that ends in a
	 * slash; that is, it has a trailing path separator or is the root absolute
	 * path, and has no query and no fragment; <code>false</code> is returned
	 * otherwise.
	 */
	boolean isPrefix();

	/**
	 * If this is a hierarchical URI reference and <code>oldPrefix</code> is a
	 * prefix of it, this returns the URI formed by replacing it by
	 * <code>newPrefix</code>; <code>null</code> otherwise.
	 * 
	 * <p>
	 * In order to be a prefix, the <code>oldPrefix</code>'s {@link #isPrefix
	 * isPrefix} must return <code>true</code>, and it must match this URI's
	 * scheme, authority, and device. Also, the paths must match, up to prefix's
	 * end.
	 * 
	 * @exception java.lang.IllegalArgumentException
	 *                if either <code>oldPrefix</code> or <code>newPrefix</code>
	 *                is not a prefix URI according to {@link #isPrefix}.
	 */
	URI replacePrefix(URI oldPrefix, URI newPrefix);
}