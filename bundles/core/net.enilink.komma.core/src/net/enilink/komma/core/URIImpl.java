/**
 * <copyright> 
 *
 * Copyright (c) 2002-2006 IBM Corporation and others.
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
package net.enilink.komma.core;

import static net.enilink.komma.core.URIs.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of the {@link URI} interface.
 */
final class URIImpl implements URI {
	// Common to all URI types.
	private final int hashCode;
	private final boolean hierarchical;
	private final String scheme; // null -> relative URI reference
	private final String authority;
	private final String fragment;
	private URIImpl cachedNamespace;
	private URIImpl cachedTrimFragment;
	private String cachedToString;
	// private final boolean Iri;
	// private URI cachedASCIIURI;

	// Applicable only to a hierarchical URI.
	private final String device;
	private final boolean absolutePath;
	private final String[] segments; // empty last segment -> trailing separator
	private final String query;

	// Package protected constructor for use of static factory methods.
	URIImpl(boolean hierarchical, String scheme, String authority,
			String device, boolean absolutePath, String[] segments,
			String query, String fragment) {
		int hashCode = 0;
		// boolean Iri = false;

		if (hierarchical) {
			++hashCode;
		}
		if (absolutePath) {
			hashCode += 2;
		}
		if (scheme != null) {
			hashCode ^= scheme.toLowerCase().hashCode();
		}
		if (authority != null) {
			hashCode ^= authority.hashCode();
			// Iri = Iri || containsNonASCII(authority);
		}
		if (device != null) {
			hashCode ^= device.hashCode();
			// Iri = Iri || containsNonASCII(device);
		}
		if (query != null) {
			hashCode ^= query.hashCode();
			// Iri = Iri || containsNonASCII(query);
		}
		if (fragment != null) {
			hashCode ^= fragment.hashCode();
			// Iri = Iri || containsNonASCII(fragment);
		}

		for (int i = 0, len = segments.length; i < len; i++) {
			hashCode ^= segments[i].hashCode();
			// Iri = Iri || containsNonASCII(segments[i]);
		}

		this.hashCode = hashCode;
		// this.Iri = Iri;
		this.hierarchical = hierarchical;
		this.scheme = scheme == null ? null : scheme.intern();
		this.authority = authority;
		this.device = device;
		this.absolutePath = absolutePath;
		this.segments = segments;
		this.query = query;
		this.fragment = fragment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isRelative()
	 */
	public boolean isRelative() {
		return scheme == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isHierarchical()
	 */
	public boolean isHierarchical() {
		return hierarchical;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasAuthority()
	 */
	public boolean hasAuthority() {
		return hierarchical && authority != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasOpaquePart()
	 */
	public boolean hasOpaquePart() {
		// note: hierarchical -> authority != null
		return !hierarchical;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasDevice()
	 */
	public boolean hasDevice() {
		// note: device != null -> hierarchical
		return device != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasPath()
	 */
	public boolean hasPath() {
		// note: (absolutePath || authority == null) -> hierarchical
		// (authority == null && device == null && !absolutePath) -> scheme ==
		// null
		return absolutePath || (authority == null && device == null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasAbsolutePath()
	 */
	public boolean hasAbsolutePath() {
		// note: absolutePath -> hierarchical
		return absolutePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasRelativePath()
	 */
	public boolean hasRelativePath() {
		// note: authority == null -> hierarchical
		// (authority == null && device == null && !absolutePath) -> scheme ==
		// null
		return authority == null && device == null && !absolutePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasEmptyPath()
	 */
	public boolean hasEmptyPath() {
		// note: authority == null -> hierarchical
		// (authority == null && device == null && !absolutePath) -> scheme ==
		// null
		return authority == null && device == null && !absolutePath
				&& segments.length == 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasQuery()
	 */
	public boolean hasQuery() {
		// note: query != null -> hierarchical
		return query != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasFragment()
	 */
	public boolean hasFragment() {
		return fragment != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isCurrentDocumentReference()
	 */
	public boolean isCurrentDocumentReference() {
		// note: authority == null -> hierarchical
		// (authority == null && device == null && !absolutePath) -> scheme ==
		// null
		return authority == null && device == null && !absolutePath
				&& segments.length == 0 && query == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isEmpty()
	 */
	public boolean isEmpty() {
		// note: authority == null -> hierarchical
		// (authority == null && device == null && !absolutePath) -> scheme ==
		// null
		return authority == null && device == null && !absolutePath
				&& segments.length == 0 && query == null && fragment == null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isFile()
	 */
	public boolean isFile() {
		return isHierarchical()
				&& ((isRelative() && !hasQuery()) || SCHEME_FILE
						.equalsIgnoreCase(scheme));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isPlatform()
	 */
	public boolean isPlatform() {
		return isHierarchical() && !hasAuthority() && segmentCount() >= 2
				&& SCHEME_PLATFORM.equalsIgnoreCase(scheme);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isPlatformResource()
	 */
	public boolean isPlatformResource() {
		return isPlatform() && "resource".equals(segments[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isPlatformPlugin()
	 */
	public boolean isPlatformPlugin() {
		return isPlatform() && "plugin".equals(segments[0]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isArchive()
	 */
	public boolean isArchive() {
		return isArchiveScheme(scheme);
	}

	/**
	 * Returns the hash code.
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object instanceof IReference) {
			object = ((IReference) object).getURI();
		}
		if (!(object instanceof URIImpl))
			return false;
		URIImpl uri = (URIImpl) object;

		return hashCode == uri.hashCode()
				&& hierarchical == uri.isHierarchical()
				&& absolutePath == uri.hasAbsolutePath()
				&& equals(scheme, uri.scheme(), true)
				&& equals(authority,
						hierarchical ? uri.authority() : uri.opaquePart())
				&& equals(device, uri.device()) && equals(query, uri.query())
				&& equals(fragment, uri.fragment()) && segmentsEqual(uri);
	}

	// Tests whether this URI's path segment array is equal to that of the
	// given uri.
	private boolean segmentsEqual(URI uri) {
		if (segments.length != uri.segmentCount())
			return false;
		for (int i = 0, len = segments.length; i < len; i++) {
			if (!segments[i].equals(uri.segment(i)))
				return false;
		}
		return true;
	}

	// Tests two objects for equality, tolerating nulls; null is considered
	// to be a valid value that is only equal to itself.
	private static boolean equals(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}

	// Tests two strings for equality, tolerating nulls and optionally
	// ignoring case.
	private static boolean equals(String s1, String s2, boolean ignoreCase) {
		return s1 == null ? s2 == null : ignoreCase ? s1.equalsIgnoreCase(s2)
				: s1.equals(s2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#scheme()
	 */
	public String scheme() {
		return scheme;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#opaquePart()
	 */
	public String opaquePart() {
		return isHierarchical() ? null : authority;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#authority()
	 */
	public String authority() {
		return isHierarchical() ? authority : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#userInfo()
	 */
	public String userInfo() {
		if (!hasAuthority())
			return null;

		int i = authority.indexOf(USER_INFO_SEPARATOR);
		return i < 0 ? null : authority.substring(0, i);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#host()
	 */
	public String host() {
		if (!hasAuthority())
			return null;

		int i = authority.indexOf(USER_INFO_SEPARATOR);
		int j = authority.indexOf(PORT_SEPARATOR);
		return j < 0 ? authority.substring(i + 1) : authority.substring(i + 1,
				j);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#port()
	 */
	public String port() {
		if (!hasAuthority())
			return null;

		int i = authority.indexOf(PORT_SEPARATOR);
		return i < 0 ? null : authority.substring(i + 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#device()
	 */
	public String device() {
		return device;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#segments()
	 */
	public String[] segments() {
		return segments.clone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#segmentsList()
	 */
	public List<String> segmentsList() {
		return Collections.unmodifiableList(Arrays.asList(segments));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#segmentCount()
	 */
	public int segmentCount() {
		return segments.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#segment(int)
	 */
	public String segment(int i) {
		return segments[i];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#lastSegment()
	 */
	public String lastSegment() {
		int len = segments.length;
		if (len == 0)
			return null;
		return segments[len - 1];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#path()
	 */
	public String path() {
		if (!hasPath())
			return null;

		StringBuffer result = new StringBuffer();
		if (hasAbsolutePath())
			result.append(SEGMENT_SEPARATOR);

		for (int i = 0, len = segments.length; i < len; i++) {
			if (i != 0)
				result.append(SEGMENT_SEPARATOR);
			result.append(segments[i]);
		}
		return result.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#devicePath()
	 */
	public String devicePath() {
		if (!hasPath())
			return null;

		StringBuffer result = new StringBuffer();

		if (hasAuthority()) {
			if (!isArchive())
				result.append(AUTHORITY_SEPARATOR);
			result.append(authority);

			if (hasDevice())
				result.append(SEGMENT_SEPARATOR);
		}

		if (hasDevice())
			result.append(device);
		if (hasAbsolutePath())
			result.append(SEGMENT_SEPARATOR);

		for (int i = 0, len = segments.length; i < len; i++) {
			if (i != 0)
				result.append(SEGMENT_SEPARATOR);
			result.append(segments[i]);
		}
		return result.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#query()
	 */
	public String query() {
		return query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#appendQuery(java.lang.String)
	 */
	public URI appendQuery(String query) {
		if (!validQuery(query)) {
			throw new IllegalArgumentException("invalid query portion: "
					+ query);
		}
		return new URIImpl(hierarchical, scheme, authority, device,
				absolutePath, segments, query, fragment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#trimQuery()
	 */
	public URI trimQuery() {
		if (query == null) {
			return this;
		} else {
			return new URIImpl(hierarchical, scheme, authority, device,
					absolutePath, segments, null, fragment);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#fragment()
	 */
	public String fragment() {
		return fragment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#appendFragment(java.lang.String)
	 */
	public URIImpl appendFragment(String fragment) {
		if (!validFragment(fragment)) {
			throw new IllegalArgumentException("invalid fragment portion: "
					+ fragment);
		}
		URIImpl result = new URIImpl(hierarchical, scheme, authority, device,
				absolutePath, segments, query, fragment);

		if (!hasFragment()) {
			result.cachedTrimFragment = this;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#trimFragment()
	 */
	public URIImpl trimFragment() {
		if (fragment == null) {
			return this;
		} else if (cachedTrimFragment == null) {
			cachedTrimFragment = new URIImpl(hierarchical, scheme, authority,
					device, absolutePath, segments, query, null);
		}

		return cachedTrimFragment;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#namespace()
	 */
	public URIImpl namespace() {
		if (cachedNamespace != null) {
			return cachedNamespace;
		}

		if (fragment != null) {
			if (fragment.length() == 0) {
				cachedNamespace = this;
			} else {
				cachedNamespace = new URIImpl(hierarchical, scheme, authority,
						device, absolutePath, segments, query, "");
			}
		} else if (opaquePart() != null) {
			String opaquePart = opaquePart();
			int colonIndex = opaquePart.lastIndexOf(':');
			if (colonIndex >= 0) {
				cachedNamespace = new URIImpl(false, scheme,
						opaquePart.substring(0, colonIndex + 1), null,
						absolutePath, NO_SEGMENTS, null, null);
			} else {
				cachedNamespace = this;
			}
		} else {
			cachedNamespace = trimSegments(1).appendSegment("");
		}

		return cachedNamespace;
	}

	public URI appendLocalPart(String localPart) {
		if (!isHierarchical() && toString().endsWith(":")) {
			return createURI(toString() + localPart);
		}
		String last = lastSegment();
		if (last == null || last != null && last.length() > 0) {
			return appendFragment(localPart);
		}
		return trimSegments(1).appendSegment(localPart);
	}

	@Override
	public String localPart() {
		String localName = fragment();
		if (localName != null) {
			return localName;
		}
		localName = lastSegment();
		if (localName != null) {
			return localName;
		}
		localName = toString();
		int separatorIndex = localName.lastIndexOf(':');
		if (separatorIndex >= 0) {
			localName = localName.substring(separatorIndex + 1);
		}
		return localName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#resolve(net.enilink.komma
	 * .common.util.URI)
	 */
	public URIImpl resolve(URI base) {
		return resolve(base, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#resolve(net.enilink.komma
	 * .common.util.URI, boolean)
	 */
	public URIImpl resolve(URI base, boolean preserveRootParents) {
		if (!base.isHierarchical() || base.isRelative()) {
			throw new IllegalArgumentException(
					"resolve against non-hierarchical or relative base");
		}

		// an absolute URI needs no resolving
		if (!isRelative())
			return this;

		// note: isRelative() -> hierarchical

		String newAuthority = authority;
		String newDevice = device;
		boolean newAbsolutePath = absolutePath;
		String[] newSegments = segments;
		String newQuery = query;
		// note: it's okay for two URIs to share a segments array, since
		// neither will ever modify it

		if (authority == null) {
			// no authority: use base's
			newAuthority = base.authority();

			if (device == null) {
				// no device: use base's
				newDevice = base.device();

				if (hasEmptyPath() && query == null) {
					// current document reference: use base path and query
					newAbsolutePath = base.hasAbsolutePath();
					newSegments = base.segments();
					newQuery = base.query();
				} else if (hasRelativePath()) {
					// relative path: merge with base and keep query (note: if
					// the
					// base has no path and this a non-empty relative path,
					// there is
					// an implied root in the resulting path)
					newAbsolutePath = base.hasAbsolutePath() || !hasEmptyPath();
					newSegments = newAbsolutePath ? mergePath(base,
							preserveRootParents) : NO_SEGMENTS;
				}
				// else absolute path: keep it and query
			}
			// else keep device, path, and query
		}
		// else keep authority, device, path, and query

		// always keep fragment, even if null, and use scheme from base;
		// no validation needed since all components are from existing URIs
		return new URIImpl(true, base.scheme(), newAuthority, newDevice,
				newAbsolutePath, newSegments, newQuery, fragment);
	}

	// Merges this URI's relative path with the base non-relative path. If
	// base has no path, treat it as the root absolute path, unless this has
	// no path either.
	private String[] mergePath(URI base, boolean preserveRootParents) {
		if (base.hasRelativePath()) {
			throw new IllegalArgumentException("merge against relative path");
		}
		if (!hasRelativePath()) {
			throw new IllegalStateException("merge non-relative path");
		}

		int baseSegmentCount = base.segmentCount();
		int segmentCount = segments.length;
		String[] stack = new String[baseSegmentCount + segmentCount];
		int sp = 0;

		// use a stack to accumulate segments of base, except for the last
		// (i.e. skip trailing separator and anything following it), and of
		// relative path
		for (int i = 0; i < baseSegmentCount - 1; i++) {
			sp = accumulate(stack, sp, base.segment(i), preserveRootParents);
		}

		for (int i = 0; i < segmentCount; i++) {
			sp = accumulate(stack, sp, segments[i], preserveRootParents);
		}

		// if the relative path is empty or ends in an empty segment, a parent
		// reference, or a self reference, add a trailing separator to a
		// non-empty path
		if (sp > 0
				&& (segmentCount == 0
						|| SEGMENT_EMPTY.equals(segments[segmentCount - 1])
						|| SEGMENT_PARENT.equals(segments[segmentCount - 1]) || SEGMENT_SELF
							.equals(segments[segmentCount - 1]))) {
			stack[sp++] = SEGMENT_EMPTY;
		}

		// return a correctly sized result
		String[] result = new String[sp];
		System.arraycopy(stack, 0, result, 0, sp);
		return result;
	}

	// Adds a segment to a stack, skipping empty segments and self references,
	// and interpreting parent references.
	private static int accumulate(String[] stack, int sp, String segment,
			boolean preserveRootParents) {
		if (SEGMENT_PARENT.equals(segment)) {
			if (sp == 0) {
				// special care must be taken for a root's parent reference: it
				// is
				// either ignored or the symbolic reference itself is pushed
				if (preserveRootParents)
					stack[sp++] = segment;
			} else {
				// unless we're already accumulating root parent references,
				// parent references simply pop the last segment descended
				if (SEGMENT_PARENT.equals(stack[sp - 1]))
					stack[sp++] = segment;
				else
					sp--;
			}
		} else if (!SEGMENT_EMPTY.equals(segment)
				&& !SEGMENT_SELF.equals(segment)) {
			// skip empty segments and self references; push everything else
			stack[sp++] = segment;
		}
		return sp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#deresolve(net.enilink.komma
	 * .common.util.URIImpl)
	 */
	public URI deresolve(URI base) {
		return deresolve(base, true, false, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#deresolve(net.enilink.komma
	 * .common.util.URIImpl, boolean, boolean, boolean)
	 */
	public URI deresolve(URI base, boolean preserveRootParents,
			boolean anyRelPath, boolean shorterRelPath) {
		if (!base.isHierarchical() || base.isRelative()) {
			throw new IllegalArgumentException(
					"deresolve against non-hierarchical or relative base");
		}
		if (isRelative()) {
			throw new IllegalStateException("deresolve relative URI");
		}

		// note: these assertions imply that neither this nor the base URI has a
		// relative path; thus, both have either an absolute path or no path

		// different scheme: need complete, absolute URI
		if (!scheme.equalsIgnoreCase(base.scheme()))
			return this;

		// since base must be hierarchical, and since a non-hierarchical URI
		// must have both scheme and opaque part, the complete absolute URI is
		// needed to resolve to a non-hierarchical URI
		if (!isHierarchical())
			return this;

		String newAuthority = authority;
		String newDevice = device;
		boolean newAbsolutePath = absolutePath;
		String[] newSegments = segments;
		String newQuery = query;

		if (equals(authority, base.authority())
				&& (hasDevice() || hasPath() || (!base.hasDevice() && !base
						.hasPath()))) {
			// matching authorities and no device or path removal
			newAuthority = null;

			if (equals(device, base.device()) && (hasPath() || !base.hasPath())) {
				// matching devices and no path removal
				newDevice = null;

				// exception if (!hasPath() && base.hasPath())

				if (!anyRelPath && !shorterRelPath) {
					// user rejects a relative path: keep absolute or no path
				} else if (hasPath() == base.hasPath() && segmentsEqual(base)
						&& equals(query, base.query())) {
					// current document reference: keep no path or query
					newAbsolutePath = false;
					newSegments = NO_SEGMENTS;
					newQuery = null;
				} else if (!hasPath() && !base.hasPath()) {
					// no paths: keep query only
					newAbsolutePath = false;
					newSegments = NO_SEGMENTS;
				}
				// exception if (!hasAbsolutePath())
				else if (hasCollapsableSegments(preserveRootParents)) {
					// path form demands an absolute path: keep it and query
				} else {
					// keep query and select relative or absolute path based on
					// length
					String[] rel = findRelativePath(base, preserveRootParents);
					if (anyRelPath || segments.length > rel.length) {
						// user demands a relative path or the absolute path is
						// longer
						newAbsolutePath = false;
						newSegments = rel;
					}
					// else keep shorter absolute path
				}
			}
			// else keep device, path, and query
		}
		// else keep authority, device, path, and query

		// always include fragment, even if null;
		// no validation needed since all components are from existing URIs
		return new URIImpl(true, null, newAuthority, newDevice,
				newAbsolutePath, newSegments, newQuery, fragment);
	}

	// Returns true if the non-relative path includes segments that would be
	// collapsed when resolving; false otherwise. If preserveRootParents is
	// true, collapsible segments include any empty segments, except for the
	// last segment, as well as and parent and self references. If
	// preserveRootsParents is false, parent references are not collapsible if
	// they are the first segment or preceded only by other parent
	// references.
	private boolean hasCollapsableSegments(boolean preserveRootParents) {
		if (hasRelativePath()) {
			throw new IllegalStateException(
					"test collapsability of relative path");
		}

		for (int i = 0, len = segments.length; i < len; i++) {
			String segment = segments[i];
			if ((i < len - 1 && SEGMENT_EMPTY.equals(segment))
					|| SEGMENT_SELF.equals(segment)
					|| SEGMENT_PARENT.equals(segment)
					&& (!preserveRootParents || (i != 0 && !SEGMENT_PARENT
							.equals(segments[i - 1])))) {
				return true;
			}
		}
		return false;
	}

	// Returns the shortest relative path between the the non-relative path of
	// the given base and this absolute path. If the base has no path, it is
	// treated as the root absolute path.
	private String[] findRelativePath(URI base, boolean preserveRootParents) {
		if (base.hasRelativePath()) {
			throw new IllegalArgumentException(
					"find relative path against base with relative path");
		}
		if (!hasAbsolutePath()) {
			throw new IllegalArgumentException(
					"find relative path of non-absolute path");
		}

		// treat an empty base path as the root absolute path
		String[] startPath = ((URIImpl) base)
				.collapseSegments(preserveRootParents);
		String[] endPath = segments;

		// drop last segment from base, as in resolving
		int startCount = startPath.length > 0 ? startPath.length - 1 : 0;
		int endCount = endPath.length;

		// index of first segment that is different between endPath and
		// startPath
		int diff = 0;

		// if endPath is shorter than startPath, the last segment of endPath may
		// not be compared: because startPath has been collapsed and had its
		// last segment removed, all preceding segments can be considered non-
		// empty and followed by a separator, while the last segment of endPath
		// will either be non-empty and not followed by a separator, or just
		// empty
		for (int count = startCount < endCount ? startCount : endCount - 1; diff < count
				&& startPath[diff].equals(endPath[diff]); diff++) {
			// Empty statement.
		}

		int upCount = startCount - diff;
		int downCount = endCount - diff;

		// a single separator, possibly preceded by some parent reference
		// segments, is redundant
		if (downCount == 1 && SEGMENT_EMPTY.equals(endPath[endCount - 1])) {
			downCount = 0;
		}

		// an empty path needs to be replaced by a single "." if there is no
		// query, to distinguish it from a current document reference
		if (upCount + downCount == 0) {
			if (query == null)
				return new String[] { SEGMENT_SELF };
			return NO_SEGMENTS;
		}

		// return a correctly sized result
		String[] result = new String[upCount + downCount];
		Arrays.fill(result, 0, upCount, SEGMENT_PARENT);
		System.arraycopy(endPath, diff, result, upCount, downCount);
		return result;
	}

	// Collapses non-ending empty segments, parent references, and self
	// references in a non-relative path, returning the same path that would
	// be produced from the base hierarchical URI as part of a resolve.
	String[] collapseSegments(boolean preserveRootParents) {
		if (hasRelativePath()) {
			throw new IllegalStateException("collapse relative path");
		}

		if (!hasCollapsableSegments(preserveRootParents))
			return segments();

		// use a stack to accumulate segments
		int segmentCount = segments.length;
		String[] stack = new String[segmentCount];
		int sp = 0;

		for (int i = 0; i < segmentCount; i++) {
			sp = accumulate(stack, sp, segments[i], preserveRootParents);
		}

		// if the path is non-empty and originally ended in an empty segment, a
		// parent reference, or a self reference, add a trailing separator
		if (sp > 0
				&& (SEGMENT_EMPTY.equals(segments[segmentCount - 1])
						|| SEGMENT_PARENT.equals(segments[segmentCount - 1]) || SEGMENT_SELF
							.equals(segments[segmentCount - 1]))) {
			stack[sp++] = SEGMENT_EMPTY;
		}

		// return a correctly sized result
		String[] result = new String[sp];
		System.arraycopy(stack, 0, result, 0, sp);
		return result;
	}

	/**
	 * Returns the string representation of this URI. For a generic,
	 * non-hierarchical URI, this looks like:
	 * 
	 * <pre>
	 *   scheme:opaquePart#fragment
	 * </pre>
	 * 
	 * <p>
	 * For a hierarchical URI, it looks like:
	 * 
	 * <pre>
	 *   scheme://authority/device/pathSegment1/pathSegment2...?query#fragment
	 * </pre>
	 * 
	 * <p>
	 * For an <a href="#archive_explanation">archive URI</a>, it's just:
	 * 
	 * <pre>
	 *   scheme:authority/pathSegment1/pathSegment2...?query#fragment
	 * </pre>
	 * <p>
	 * Of course, absent components and their separators will be omitted.
	 */
	@Override
	public String toString() {
		if (cachedToString == null) {
			StringBuffer result = new StringBuffer();
			if (!isRelative()) {
				result.append(scheme);
				result.append(SCHEME_SEPARATOR);
			}

			if (isHierarchical()) {
				if (hasAuthority()) {
					if (!isArchive())
						result.append(AUTHORITY_SEPARATOR);
					result.append(authority);
				}

				if (hasDevice()) {
					result.append(SEGMENT_SEPARATOR);
					result.append(device);
				}

				if (hasAbsolutePath())
					result.append(SEGMENT_SEPARATOR);

				for (int i = 0, len = segments.length; i < len; i++) {
					if (i != 0)
						result.append(SEGMENT_SEPARATOR);
					result.append(segments[i]);
				}

				if (hasQuery()) {
					result.append(QUERY_SEPARATOR);
					result.append(query);
				}
			} else {
				result.append(authority);
			}

			if (hasFragment()) {
				result.append(FRAGMENT_SEPARATOR);
				result.append(fragment);
			}
			cachedToString = result.toString();
		}
		return cachedToString;
	}

	// Returns a string representation of this URI for debugging, explicitly
	// showing each of the components.
	String toString(boolean includeSimpleForm) {
		StringBuffer result = new StringBuffer();
		if (includeSimpleForm)
			result.append(toString());
		result.append("\n hierarchical: ");
		result.append(hierarchical);
		result.append("\n       scheme: ");
		result.append(scheme);
		result.append("\n    authority: ");
		result.append(authority);
		result.append("\n       device: ");
		result.append(device);
		result.append("\n absolutePath: ");
		result.append(absolutePath);
		result.append("\n     segments: ");
		if (segments.length == 0)
			result.append("<empty>");
		for (int i = 0, len = segments.length; i < len; i++) {
			if (i > 0)
				result.append("\n               ");
			result.append(segments[i]);
		}
		result.append("\n        query: ");
		result.append(query);
		result.append("\n     fragment: ");
		result.append(fragment);
		return result.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#toFileString()
	 */
	public String toFileString() {
		if (!isFile())
			return null;

		StringBuffer result = new StringBuffer();
		char separator = File.separatorChar;

		if (hasAuthority()) {
			result.append(separator);
			result.append(separator);
			result.append(authority);

			if (hasDevice())
				result.append(separator);
		}

		if (hasDevice())
			result.append(device);
		if (hasAbsolutePath())
			result.append(separator);

		for (int i = 0, len = segments.length; i < len; i++) {
			if (i != 0)
				result.append(separator);
			result.append(segments[i]);
		}

		return decode(result.toString());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#toPlatformString(boolean)
	 */
	public String toPlatformString(boolean decode) {
		if (isPlatform()) {
			StringBuffer result = new StringBuffer();
			for (int i = 1, len = segments.length; i < len; i++) {
				result.append('/').append(
						decode ? decode(segments[i]) : segments[i]);
			}
			return result.toString();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#appendSegment(java.lang.String)
	 */
	public URIImpl appendSegment(String segment) {
		if (!validSegment(segment)) {
			throw new IllegalArgumentException("invalid segment: " + segment);
		}

		if (!isHierarchical())
			return this;

		// absolute path or no path -> absolute path
		boolean newAbsolutePath = !hasRelativePath();

		int len = segments.length;
		String[] newSegments = new String[len + 1];
		System.arraycopy(segments, 0, newSegments, 0, len);
		newSegments[len] = segment;

		return new URIImpl(true, scheme, authority, device, newAbsolutePath,
				newSegments, query, fragment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#appendSegments(java.lang.String
	 * [])
	 */
	public URIImpl appendSegments(String[] segments) {
		if (!validSegments(segments)) {
			String s = segments == null ? "invalid segments: null"
					: "invalid segment: " + firstInvalidSegment(segments);
			throw new IllegalArgumentException(s);
		}

		if (!isHierarchical())
			return this;

		// absolute path or no path -> absolute path
		boolean newAbsolutePath = !hasRelativePath();

		int len = this.segments.length;
		int segmentsCount = segments.length;
		String[] newSegments = new String[len + segmentsCount];
		System.arraycopy(this.segments, 0, newSegments, 0, len);
		System.arraycopy(segments, 0, newSegments, len, segmentsCount);

		return new URIImpl(true, scheme, authority, device, newAbsolutePath,
				newSegments, query, fragment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#trimSegments(int)
	 */
	public URIImpl trimSegments(int i) {
		if (!isHierarchical() || i < 1)
			return this;

		String[] newSegments = NO_SEGMENTS;
		int len = segments.length - i;
		if (len > 0) {
			newSegments = new String[len];
			System.arraycopy(segments, 0, newSegments, 0, len);
		}
		return new URIImpl(true, scheme, authority, device, absolutePath,
				newSegments, query, fragment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#hasTrailingPathSeparator()
	 */
	public boolean hasTrailingPathSeparator() {
		return segments.length > 0
				&& SEGMENT_EMPTY.equals(segments[segments.length - 1]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#fileExtension()
	 */
	public String fileExtension() {
		int len = segments.length;
		if (len == 0)
			return null;

		String lastSegment = segments[len - 1];
		int i = lastSegment.lastIndexOf(FILE_EXTENSION_SEPARATOR);
		return i < 0 ? null : lastSegment.substring(i + 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#appendFileExtension(java.lang
	 * .String)
	 */
	public URI appendFileExtension(String fileExtension) {
		if (!validSegment(fileExtension)) {
			throw new IllegalArgumentException("invalid segment portion: "
					+ fileExtension);
		}

		int len = segments.length;
		if (len == 0)
			return this;

		String lastSegment = segments[len - 1];
		if (SEGMENT_EMPTY.equals(lastSegment))
			return this;
		StringBuffer newLastSegment = new StringBuffer(lastSegment);
		newLastSegment.append(FILE_EXTENSION_SEPARATOR);
		newLastSegment.append(fileExtension);

		String[] newSegments = new String[len];
		System.arraycopy(segments, 0, newSegments, 0, len - 1);
		newSegments[len - 1] = newLastSegment.toString();

		// note: segments.length > 0 -> hierarchical
		return new URIImpl(true, scheme, authority, device, absolutePath,
				newSegments, query, fragment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#trimFileExtension()
	 */
	public URI trimFileExtension() {
		int len = segments.length;
		if (len == 0)
			return this;

		String lastSegment = segments[len - 1];
		int i = lastSegment.lastIndexOf(FILE_EXTENSION_SEPARATOR);
		if (i < 0)
			return this;

		String newLastSegment = lastSegment.substring(0, i);
		String[] newSegments = new String[len];
		System.arraycopy(segments, 0, newSegments, 0, len - 1);
		newSegments[len - 1] = newLastSegment;

		// note: segments.length > 0 -> hierarchical
		return new URIImpl(true, scheme, authority, device, absolutePath,
				newSegments, query, fragment);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#isPrefix()
	 */
	public boolean isPrefix() {
		return hierarchical
				&& query == null
				&& fragment == null
				&& (hasTrailingPathSeparator() || (absolutePath && segments.length == 0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.komma.common.util.URI#replacePrefix(net.enilink
	 * .komma.common.util.URI, net.enilink.komma.common.util.URI)
	 */
	public URI replacePrefix(URI oldPrefix, URI newPrefix) {
		if (!oldPrefix.isPrefix() || !newPrefix.isPrefix()) {
			String which = oldPrefix.isPrefix() ? "new" : "old";
			throw new IllegalArgumentException("non-prefix " + which + " value");
		}

		// Get what's left of the segments after trimming the prefix.
		String[] tailSegments = getTailSegments(oldPrefix);
		if (tailSegments == null)
			return null;

		// If the new prefix has segments, it is not the root absolute path,
		// and we need to drop the trailing empty segment and append the tail
		// segments.
		String[] mergedSegments = tailSegments;
		if (newPrefix.segmentCount() != 0) {
			int segmentsToKeep = newPrefix.segmentCount() - 1;
			mergedSegments = new String[segmentsToKeep + tailSegments.length];
			System.arraycopy(newPrefix.segments(), 0, mergedSegments, 0,
					segmentsToKeep);

			if (tailSegments.length != 0) {
				System.arraycopy(tailSegments, 0, mergedSegments,
						segmentsToKeep, tailSegments.length);
			}
		}

		// no validation needed since all components are from existing URIs
		return new URIImpl(true, newPrefix.scheme(), newPrefix.authority(),
				newPrefix.device(), newPrefix.hasAbsolutePath(),
				mergedSegments, query, fragment);
	}

	// If this is a hierarchical URI reference and prefix is a prefix of it,
	// returns the portion of the path remaining after that prefix has been
	// trimmed; null otherwise.
	private String[] getTailSegments(URI prefix) {
		if (!prefix.isPrefix()) {
			throw new IllegalArgumentException("non-prefix trim");
		}

		// Don't even consider it unless this is hierarchical and has scheme,
		// authority, device and path absoluteness equal to those of the prefix.
		if (!hierarchical || !equals(scheme, prefix.scheme(), true)
				|| !equals(authority, prefix.authority())
				|| !equals(device, prefix.device())
				|| absolutePath != prefix.hasAbsolutePath()) {
			return null;
		}

		// If the prefix has no segments, then it is the root absolute path, and
		// we know this is an absolute path, too.
		if (prefix.segmentCount() == 0)
			return segments;

		// This must have no fewer segments than the prefix. Since the prefix
		// is not the root absolute path, its last segment is empty; all others
		// must match.
		int i = 0;
		int segmentsToCompare = prefix.segmentCount() - 1;
		if (segments.length <= segmentsToCompare)
			return null;

		for (; i < segmentsToCompare; i++) {
			if (!segments[i].equals(prefix.segment(i)))
				return null;
		}

		// The prefix really is a prefix of this. If this has just one more,
		// empty segment, the paths are the same.
		if (i == segments.length - 1 && SEGMENT_EMPTY.equals(segments[i])) {
			return NO_SEGMENTS;
		}

		// Otherwise, the path needs only the remaining segments.
		String[] newSegments = new String[segments.length - i];
		System.arraycopy(segments, i, newSegments, 0, newSegments.length);
		return newSegments;
	}

	/*
	 * Returns <code>true</code> if this URI contains non-ASCII characters;
	 * <code>false</code> otherwise.
	 * 
	 * This unused code is included for possible future use...
	 */
	/*
	 * public boolean isIRI() { return Iri; }
	 * 
	 * // Returns true if the given string contains any non-ASCII characters; //
	 * false otherwise. private static boolean containsNonASCII(String value) {
	 * for (int i = 0, length = value.length(); i < length; i++) { if
	 * (value.charAt(i) > 127) return true; } return false; }
	 */

	/*
	 * If this is an {@link #isIRI IRI}, converts it to a strict ASCII URI,
	 * using the procedure described in Section 3.1 of the <a
	 * href="http://www.w3.org/International/Iri-edit/draft-duerst-Iri-09.txt"
	 * >IRI Draft RFC</a>. Otherwise, this URI, itself, is returned.
	 * 
	 * This unused code is included for possible future use...
	 */
	/*
	 * public URI toASCIIURI() { if (!Iri) return this;
	 * 
	 * if (cachedASCIIURI == null) { String eAuthority =
	 * encodeAsASCII(authority); String eDevice = encodeAsASCII(device); String
	 * eQuery = encodeAsASCII(query); String eFragment =
	 * encodeAsASCII(fragment); String[] eSegments = new
	 * String[segments.length]; for (int i = 0; i < segments.length; i++) {
	 * eSegments[i] = encodeAsASCII(segments[i]); } cachedASCIIURI = new
	 * URI(hierarchical, scheme, eAuthority, eDevice, absolutePath, eSegments,
	 * eQuery, eFragment);
	 * 
	 * } return cachedASCIIURI; }
	 * 
	 * // Returns a strict ASCII encoding of the given value. Each non-ASCII //
	 * character is converted to bytes using UTF-8 encoding, which are then //
	 * represented using % escaping. private String encodeAsASCII(String value)
	 * { if (value == null) return null;
	 * 
	 * StringBuffer result = null;
	 * 
	 * for (int i = 0, length = value.length(); i < length; i++) { char c =
	 * value.charAt(i);
	 * 
	 * if (c >= 128) { if (result == null) { result = new
	 * StringBuffer(value.substring(0, i)); }
	 * 
	 * try { byte[] encoded = (new String(new char[] { c })).getBytes("UTF-8");
	 * for (int j = 0, encLen = encoded.length; j < encLen; j++) {
	 * appendEscaped(result, encoded[j]); } } catch
	 * (UnsupportedEncodingException e) { throw new WrappedException(e); } }
	 * else if (result != null) { result.append(c); }
	 * 
	 * } return result == null ? value : result.toString(); }
	 * 
	 * // Returns the number of valid, consecutive, three-character escape //
	 * sequences in the given string, starting at index i. private static int
	 * countEscaped(String s, int i) { int result = 0;
	 * 
	 * for (int length = s.length(); i < length; i += 3) { if (isEscaped(s, i))
	 * result++; } return result; }
	 */

	@Override
	public URI getURI() {
		return this;
	}

}
