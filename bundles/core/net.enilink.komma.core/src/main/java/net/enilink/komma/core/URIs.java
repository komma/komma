/**
 * <copyright>
 * <p>
 * Copyright (c) 2002-2006 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * Contributors:
 * IBM - Initial API and implementation
 *
 * </copyright>
 */
package net.enilink.komma.core;

import java.io.File;
import java.io.Serial;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Helper class for Uniform Resource Identifiers (URIs), as specified by <a
 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>. A <code>URI</code>
 * instance can be created by specifying values for its components, or by
 * providing a single URI string, which is parsed into its components. Static
 * factory methods whose names begin with "create" are used for both forms of
 * object creation.
 *
 * <p>
 * Like <code>String</code>, <code>URI</code> implementations are immutable; a
 * <code>URI</code> instance offers several by-value methods that return a new
 * <code>URI</code> object based on its current state. Most useful, a relative
 * <code>URI</code> can be {@link URI#resolve(URI) resolve}d against a base
 * absolute <code>URI</code> -- the latter typically identifies the document in
 * which the former appears. The inverse to this is {@link URI#deresolve(URI)
 * deresolve}, which answers the question, "what relative URI will resolve,
 * against the given base, to this absolute URI?"
 *
 * <p>
 * <a name="device_explanation"> The primary enhancement beyond the RFC
 * description is an optional device component. Instead of treating the device
 * as just another segment in the path, it can be stored as a separate component
 * (almost a sub-authority), with the root below it. For example, resolving
 * <code>/bar</code> against <code>file:///c:/foo</code> would result in
 * <code>file:///c:/bar</code> being returned. Also, you cannot take the parent
 * of a device, so resolving <code>..</code> against <code>file:///c:/</code>
 * would not yield <code>file:///</code>, as you might expect. This feature is
 * useful when working with file-scheme URIs, as devices do not typically occur
 * in protocol-based ones. A device-enabled <code>URI</code> is created by
 * parsing a string with {@link #createURI(String) createURI}; if the first
 * segment of the path ends with the <code>:</code> character, it is stored
 * (including the colon) as the device, instead. Alternately, either the
 * {@link #createHierarchicalURI(String, String, String, String, String)
 * no-path} or the
 * {@link #createHierarchicalURI(String, String, String, String[], String, String)
 * absolute-path} form of <code>createHierarchicalURI()</code> can be used, in
 * which a non-null <code>device</code> parameter can be specified.
 *
 * <p>
 * <a name="archive_explanation"> The other enhancement provides support for the
 * almost-hierarchical form used for files within archives, such as the JAR
 * scheme, defined for the Java Platform in the documentation for
 * {@link java.net.JarURLConnection}. By default, this support is enabled for
 * absolute URIs with scheme equal to "jar", "zip", or "archive" (ignoring
 * case), and is implemented by a hierarchical URI, whose authority includes the
 * entire URI of the archive, up to and including the <code>!</code> character.
 * The URI of the archive must have no fragment. The whole archive URI must have
 * no device and an absolute path. Special handling is supported for
 * {@link #createURI(String) creating}, {@link #validArchiveAuthority
 * validating}, {@link URI#devicePath getting the path} from, and
 * {@link #toString() displaying} archive URIs. In all other operations,
 * including {@link URI#resolve(URI) resolving} and {@link URI#deresolve(URI)
 * deresolving}, they are handled like any ordinary URI. The schemes that
 * identify archive URIs can be changed from their default by setting the
 * <code>net.enilink.komma.core.URIs.archiveSchemes</code> system property.
 * Multiple schemes should be space separated, and the test of whether a URI's
 * scheme matches is always case-insensitive.
 *
 * <p>
 * This implementation does not impose all of the restrictions on character
 * validity that are specified in the RFC. Static methods whose names begin with
 * "valid" are used to test whether a given string is valid value for the
 * various URI components. Presently, these tests place no restrictions beyond
 * what would have been required in order for {@link #createURI(String)
 * createURI} to have parsed them correctly from a single URI string. If
 * necessary in the future, these tests may be made more strict, to better
 * conform to the RFC.
 *
 * <p>
 * Another group of static methods, whose names begin with "encode", use percent
 * escaping to encode any characters that are not permitted in the various URI
 * components. Another static method is provided to {@link #decode decode}
 * encoded strings. An escaped character is represented as a percent symbol (
 * <code>%</code>), followed by two hex digits that specify the character code.
 * These encoding methods are more strict than the validation methods described
 * above. They ensure validity according to the RFC, with one exception:
 * non-ASCII characters.
 *
 * <p>
 * The RFC allows only characters that can be mapped to 7-bit US-ASCII
 * representations. Non-ASCII, single-byte characters can be used only via
 * percent escaping, as described above. This implementation uses Java's Unicode
 * <code>char</code> and <code>String</code> representations, and makes no
 * attempt to encode characters 0xA0 and above. Characters in the range
 * 0x80-0x9F are still escaped. In this respect, EMF's notion of a URI is
 * actually more like an IRI (Internationalized Resource Identifier), for which
 * an RFC is now in
 * <href="http://www.w3.org/International/Iri-edit/draft-duerst-Iri-09.txt"
 * >draft form</a>.
 *
 * <p>
 * Finally, note the difference between a <code>null</code> parameter to the
 * static factory methods and an empty string. The former signifies the absence
 * of a given URI component, while the latter simply makes the component blank.
 * This can have a significant effect when resolving. For example, consider the
 * following two URIs: <code>/bar</code> (with no authority) and
 * <code>///bar</code> (with a blank authority). Imagine resolving them against
 * a base with an authority, such as <code>http://www.eclipse.org/</code>. The
 * former case will yield <code>http://www.eclipse.org/bar</code>, as the base
 * authority will be preserved. In the latter case, the empty authority will
 * override the base authority, resulting in <code>http:///bar</code>!
 */
public final class URIs {
	// A cache of URIs, keyed by the strings from which they were created.
	// The fragment of any URI is removed before caching it here, to minimize
	// the size of the cache in the usual case where most URIs only differ by
	// the fragment.
	private static final URICache uriCache = new URICache();

	private static class URICache extends HashMap<String, WeakReference<URI>> {
		@Serial
		private static final long serialVersionUID = 1L;

		static final int MIN_LIMIT = 1000;
		int count;
		int limit = MIN_LIMIT;

		public synchronized URI get(String key) {
			WeakReference<URI> reference = super.get(key);
			return reference == null ? null : reference.get();
		}

		public synchronized void put(String key, URI value) {
			super.put(key, new WeakReference<URI>(value));
			if (++count > limit) {
				cleanGCedValues();
			}
		}

		private void cleanGCedValues() {
			for (Iterator<Map.Entry<String, WeakReference<URI>>> i = entrySet()
					.iterator(); i.hasNext(); ) {
				Map.Entry<String, WeakReference<URI>> entry = i.next();
				WeakReference<URI> reference = entry.getValue();
				if (reference.get() == null) {
					i.remove();
				}
			}
			count = 0;
			limit = Math.max(MIN_LIMIT, size() / 2);
		}
	}

	// The lower-cased schemes that will be used to identify archive URIs.
	private static final Set<String> archiveSchemes;

	// Identifies a file-type absolute URI.
	static final String SCHEME_ARCHIVE = "archive";
	static final String SCHEME_FILE = "file";
	static final String SCHEME_JAR = "jar";
	static final String SCHEME_PLATFORM = "platform";
	static final String SCHEME_ZIP = "zip";

	// Special segment values interpreted at resolve and resolve time.
	static final String SEGMENT_EMPTY = "";
	static final String SEGMENT_SELF = ".";
	static final String SEGMENT_PARENT = "..";
	static final String[] NO_SEGMENTS = new String[0];

	// Separators for parsing a URI string.
	static final char SCHEME_SEPARATOR = ':';
	static final String AUTHORITY_SEPARATOR = "//";
	static final char DEVICE_IDENTIFIER = ':';
	static final char SEGMENT_SEPARATOR = '/';
	static final char QUERY_SEPARATOR = '?';
	static final char FRAGMENT_SEPARATOR = '#';
	static final char USER_INFO_SEPARATOR = '@';
	static final char PORT_SEPARATOR = ':';
	static final char FILE_EXTENSION_SEPARATOR = '.';
	static final char ARCHIVE_IDENTIFIER = '!';
	static final String ARCHIVE_SEPARATOR = "!/";

	// Characters to use in escaping.
	private static final char ESCAPE = '%';
	private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	// Some character classes, as defined in RFC 2396's BNF for URI.
	// These are 128-bit bitmasks, stored as two longs, where the Nth bit is set
	// iff the ASCII character with value N is included in the set. These are
	// created with the highBitmask() and lowBitmask() methods defined below,
	// and a character is tested against them using matches().
	//
	private static final long ALPHA_HI = highBitmask('a', 'z')
			| highBitmask('A', 'Z');
	private static final long ALPHA_LO = lowBitmask('a', 'z')
			| lowBitmask('A', 'Z');
	private static final long DIGIT_HI = highBitmask('0', '9');
	private static final long DIGIT_LO = lowBitmask('0', '9');
	private static final long ALPHANUM_HI = ALPHA_HI | DIGIT_HI;
	private static final long ALPHANUM_LO = ALPHA_LO | DIGIT_LO;
	private static final long HEX_HI = DIGIT_HI | highBitmask('A', 'F')
			| highBitmask('a', 'f');
	private static final long HEX_LO = DIGIT_LO | lowBitmask('A', 'F')
			| lowBitmask('a', 'f');
	private static final long UNRESERVED_HI = ALPHANUM_HI
			| highBitmask("-_.!~*'()");
	private static final long UNRESERVED_LO = ALPHANUM_LO
			| lowBitmask("-_.!~*'()");
	private static final long RESERVED_HI = highBitmask(";/?:@&=+$,");
	private static final long RESERVED_LO = lowBitmask(";/?:@&=+$,");
	private static final long URIC_HI = RESERVED_HI | UNRESERVED_HI; // |
	// ucschar
	// |
	// escaped
	private static final long URIC_LO = RESERVED_LO | UNRESERVED_LO;

	// Additional useful character classes, including characters valid in
	// certain
	// URI components and separators used in parsing them out of a string.
	//
	private static final long SEGMENT_CHAR_HI = UNRESERVED_HI
			| highBitmask(";:@&=+$,"); // | ucschar | escaped
	private static final long SEGMENT_CHAR_LO = UNRESERVED_LO
			| lowBitmask(";:@&=+$,");
	private static final long PATH_CHAR_HI = SEGMENT_CHAR_HI | highBitmask('/'); // |
	// ucschar
	// |
	// escaped
	private static final long PATH_CHAR_LO = SEGMENT_CHAR_LO | lowBitmask('/');
	// private static final long SCHEME_CHAR_HI = ALPHANUM_HI |
	// highBitmask("+-.");
	// private static final long SCHEME_CHAR_LO = ALPHANUM_LO |
	// lowBitmask("+-.");
	private static final long MAJOR_SEPARATOR_HI = highBitmask(":/?#");
	private static final long MAJOR_SEPARATOR_LO = lowBitmask(":/?#");
	private static final long SEGMENT_END_HI = highBitmask("/?#");
	private static final long SEGMENT_END_LO = lowBitmask("/?#");

	// Static initializer for archiveSchemes.
	static {
		Set<String> set = new HashSet<String>();
		String propertyValue = System
				.getProperty("net.enilink.komma.core.URIs.archiveSchemes");

		if (propertyValue == null) {
			set.add(SCHEME_JAR);
			set.add(SCHEME_ZIP);
			set.add(SCHEME_ARCHIVE);
		} else {
			for (StringTokenizer t = new StringTokenizer(propertyValue); t
					.hasMoreTokens(); ) {
				set.add(t.nextToken().toLowerCase());
			}
		}

		archiveSchemes = Collections.unmodifiableSet(set);
	}

	// Returns the lower half bitmask for the given ASCII character.
	private static long lowBitmask(char c) {
		return c < 64 ? 1L << c : 0L;
	}

	// Returns the upper half bitmask for the given ACSII character.
	private static long highBitmask(char c) {
		return c >= 64 && c < 128 ? 1L << (c - 64) : 0L;
	}

	// Returns the lower half bitmask for all ASCII characters between the two
	// given characters, inclusive.
	private static long lowBitmask(char from, char to) {
		long result = 0L;
		if (from < 64 && from <= to) {
			to = to < 64 ? to : 63;
			for (char c = from; c <= to; c++) {
				result |= (1L << c);
			}
		}
		return result;
	}

	// Returns the upper half bitmask for all AsCII characters between the two
	// given characters, inclusive.
	private static long highBitmask(char from, char to) {
		return to < 64 ? 0 : lowBitmask((char) (from < 64 ? 0 : from - 64),
				(char) (to - 64));
	}

	// Returns the lower half bitmask for all the ASCII characters in the given
	// string.
	private static long lowBitmask(String chars) {
		long result = 0L;
		for (int i = 0, len = chars.length(); i < len; i++) {
			char c = chars.charAt(i);
			if (c < 64)
				result |= (1L << c);
		}
		return result;
	}

	// Returns the upper half bitmask for all the ASCII characters in the given
	// string.
	private static long highBitmask(String chars) {
		long result = 0L;
		for (int i = 0, len = chars.length(); i < len; i++) {
			char c = chars.charAt(i);
			if (c >= 64 && c < 128)
				result |= (1L << (c - 64));
		}
		return result;
	}

	// Returns whether the given character is in the set specified by the given
	// bitmask.
	private static boolean matches(char c, long highBitmask, long lowBitmask) {
		if (c >= 128)
			return false;
		return c < 64 ? ((1L << c) & lowBitmask) != 0
				: ((1L << (c - 64)) & highBitmask) != 0;
	}

	// Debugging method: converts the given long to a string of binary digits.
	/*
	 * private static String toBits(long l) { StringBuffer result = new
	 * StringBuffer(); for (int i = 0; i < 64; i++) { boolean b = (l & 1L) != 0;
	 * result.insert(0, b ? '1' : '0'); l >>= 1; } return result.toString(); }
	 */

	/**
	 * Static factory method for a generic, non-hierarchical URI. There is no
	 * concept of a relative non-hierarchical URI; such an object cannot be
	 * created.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>scheme</code> is null, if <code>scheme</code> is
	 *                an <a href="#archive_explanation">archive URI</a> scheme,
	 *                or if <code>scheme</code>, <code>opaquePart</code>, or
	 *                <code>fragment</code> is not valid according to
	 *                {@link #validScheme validScheme}, {@link #validOpaquePart
	 *                validOpaquePart}, or {@link #validFragment validFragment},
	 *                respectively.
	 */
	public static URI createGenericURI(String scheme, String opaquePart,
	                                   String fragment) {
		if (scheme == null) {
			throw new IllegalArgumentException("relative non-hierarchical URI");
		}

		if (isArchiveScheme(scheme)) {
			throw new IllegalArgumentException("non-hierarchical archive URI");
		}

		validateURI(false, scheme, opaquePart, null, false, NO_SEGMENTS, null,
				fragment);
		return new URIImpl(false, scheme, opaquePart, null, false, NO_SEGMENTS,
				null, fragment);
	}

	/**
	 * Static factory method for a hierarchical URI with no path. The URI will
	 * be relative if <code>scheme</code> is non-null, and absolute otherwise.
	 * An absolute URI with no path requires a non-null <code>authority</code>
	 * and/or <code>device</code>.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>scheme</code> is non-null while
	 *                <code>authority</code> and <code>device</code> are null,
	 *                if <code>scheme</code> is an <a
	 *                href="#archive_explanation">archive URI</a> scheme, or if
	 *                <code>scheme</code>, <code>authority</code>,
	 *                <code>device</code>, <code>query</code>, or
	 *                <code>fragment</code> is not valid according to
	 *                {@link #validScheme validSheme}, {@link #validAuthority
	 *                validAuthority}, {@link #validDevice validDevice},
	 *                {@link #validQuery validQuery}, or {@link #validFragment
	 *                validFragment}, respectively.
	 */
	public static URI createHierarchicalURI(String scheme, String authority,
	                                        String device, String query, String fragment) {
		if (scheme != null && authority == null && device == null) {
			throw new IllegalArgumentException(
					"absolute hierarchical URI without authority, device, path");
		}

		if (isArchiveScheme(scheme)) {
			throw new IllegalArgumentException("archive URI with no path");
		}

		validateURI(true, scheme, authority, device, false, NO_SEGMENTS, query,
				fragment);
		return new URIImpl(true, scheme, authority, device, false, NO_SEGMENTS,
				query, fragment);
	}

	/**
	 * Static factory method for a hierarchical URI with absolute path. The URI
	 * will be relative if <code>scheme</code> is non-null, and absolute
	 * otherwise.
	 *
	 * @param segments
	 *            an array of non-null strings, each representing one segment of
	 *            the path. As an absolute path, it is automatically preceded by
	 *            a <code>/</code> separator. If desired, a trailing separator
	 *            should be represented by an empty-string segment as the last
	 *            element of the array.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>scheme</code> is an <a
	 *                href="#archive_explanation">archive URI</a> scheme and
	 *                <code>device</code> is non-null, or if <code>scheme</code>
	 *                , <code>authority</code>, <code>device</code>,
	 *                <code>segments</code>, <code>query</code>, or
	 *                <code>fragment</code> is not valid according to
	 *                {@link #validScheme validScheme}, {@link #validAuthority
	 *                validAuthority} or {@link #validArchiveAuthority
	 *                validArchiveAuthority}, {@link #validDevice validDevice},
	 *                {@link #validSegments validSegments}, {@link #validQuery
	 *                validQuery}, or {@link #validFragment validFragment}, as
	 *                appropriate.
	 */
	public static URI createHierarchicalURI(String scheme, String authority,
	                                        String device, String[] segments, String query, String fragment) {
		if (isArchiveScheme(scheme) && device != null) {
			throw new IllegalArgumentException("archive URI with device");
		}

		segments = fix(segments);
		validateURI(true, scheme, authority, device, true, segments, query,
				fragment);
		return new URIImpl(true, scheme, authority, device, true, segments,
				query, fragment);
	}

	/**
	 * Static factory method for a relative hierarchical URI with relative path.
	 *
	 * @param segments
	 *            an array of non-null strings, each representing one segment of
	 *            the path. A trailing separator is represented by an
	 *            empty-string segment at the end of the array.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>segments</code>, <code>query</code>, or
	 *                <code>fragment</code> is not valid according to
	 *                {@link #validSegments validSegments}, {@link #validQuery
	 *                validQuery}, or {@link #validFragment validFragment},
	 *                respectively.
	 */
	public static URI createHierarchicalURI(String[] segments, String query,
	                                        String fragment) {
		segments = fix(segments);
		validateURI(true, null, null, null, false, segments, query, fragment);
		return new URIImpl(true, null, null, null, false, segments, query,
				fragment);
	}

	// Converts null to length-zero array, and clones array to ensure
	// immutability.
	private static String[] fix(String[] segments) {
		return segments == null ? NO_SEGMENTS : (String[]) segments.clone();
	}

	/**
	 * Static factory method based on parsing a URI string, with <a
	 * href="#device_explanation">explicit device support</a> and handling for
	 * <a href="#archive_explanation">archive URIs</a> enabled. The specified
	 * string is parsed as described in <a
	 * href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>, and an
	 * appropriate <code>URI</code> is created and returned. Note that validity
	 * testing is not as strict as in the RFC; essentially, only separator
	 * characters are considered. So, for example, non-Latin alphabet characters
	 * appearing in the scheme would not be considered an error.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if any component parsed from <code>uri</code> is not valid
	 *                according to {@link #validScheme validScheme},
	 *                {@link #validOpaquePart validOpaquePart},
	 *                {@link #validAuthority validAuthority},
	 *                {@link #validArchiveAuthority validArchiveAuthority},
	 *                {@link #validDevice validDevice}, {@link #validSegments
	 *                validSegments}, {@link #validQuery validQuery}, or
	 *                {@link #validFragment validFragment}, as appropriate.
	 */
	public static URI createURI(String uri) {
		return createURIWithCache(uri);
	}

	/**
	 * Static factory method that encodes and parses the given URI string.
	 * Appropriate encoding is performed for each component of the URI. If more
	 * than one <code>#</code> is in the string, the last one is assumed to be
	 * the fragment's separator, and any others are encoded.
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if any component parsed from <code>uri</code> is not valid
	 *                according to {@link #validScheme validScheme},
	 *                {@link #validOpaquePart validOpaquePart},
	 *                {@link #validAuthority validAuthority},
	 *                {@link #validArchiveAuthority validArchiveAuthority},
	 *                {@link #validDevice validDevice}, {@link #validSegments
	 *                validSegments}, {@link #validQuery validQuery}, or
	 *                {@link #validFragment validFragment}, as appropriate.
	 */
	public static URI createURI(String uri, boolean ignoreEscaped) {
		return createURIWithCache(encodeURI(uri, ignoreEscaped,
				URI.FRAGMENT_LAST_SEPARATOR));
	}

	/**
	 * Static factory method that encodes and parses the given URI string.
	 * Appropriate encoding is performed for each component of the URI. Control
	 * is provided over which, if any, <code>#</code> should be taken as the
	 * fragment separator and which should be encoded.
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 *
	 * @param fragmentLocationStyle
	 *            one of {@link URI#FRAGMENT_NONE},
	 *            {@link URI#FRAGMENT_FIRST_SEPARATOR}, or
	 *            {@link URI#FRAGMENT_LAST_SEPARATOR}, indicating which, if any, of
	 *            the <code>#</code> characters should be considered the
	 *            fragment separator. Any others will be encoded.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if any component parsed from <code>uri</code> is not valid
	 *                according to {@link #validScheme validScheme},
	 *                {@link #validOpaquePart validOpaquePart},
	 *                {@link #validAuthority validAuthority},
	 *                {@link #validArchiveAuthority validArchiveAuthority},
	 *                {@link #validDevice validDevice}, {@link #validSegments
	 *                validSegments}, {@link #validQuery validQuery}, or
	 *                {@link #validFragment validFragment}, as appropriate.
	 */
	public static URI createURI(String uri, boolean ignoreEscaped,
	                            int fragmentLocationStyle) {
		return createURIWithCache(encodeURI(uri, ignoreEscaped,
				fragmentLocationStyle));
	}

	// Uses a cache to speed up creation of a URI from a string. The cache
	// is consulted to see if the URI, less any fragment, has already been
	// created. If needed, the fragment is re-appended to the cached URI,
	// which is considerably more efficient than creating the whole URI from
	// scratch. If the URI wasn't found in the cache, it is created using
	// parseIntoURI() and then cached. This method should always be used
	// by string-parsing factory methods, instead of parseIntoURI() directly.
	private static URI createURIWithCache(String uri) {
		int i = uri.indexOf(FRAGMENT_SEPARATOR);
		String base = i == -1 ? uri : uri.substring(0, i);
		String fragment = i == -1 ? null : uri.substring(i + 1);

		URI result = uriCache.get(base);

		if (result == null) {
			result = parseIntoURI(base);
			uriCache.put(base, result);
		}

		if (fragment != null) {
			result = result.appendFragment(fragment);
		}
		return result;
	}

	// String-parsing implementation.
	private static URI parseIntoURI(String uri) {
		boolean hierarchical = true;
		String scheme = null;
		String authority = null;
		String device = null;
		boolean absolutePath = false;
		String[] segments = NO_SEGMENTS;
		String query = null;
		String fragment = null;

		int i = 0;
		int j = find(uri, i, MAJOR_SEPARATOR_HI, MAJOR_SEPARATOR_LO);

		if (j < uri.length() && uri.charAt(j) == SCHEME_SEPARATOR) {
			scheme = uri.substring(i, j);
			i = j + 1;
		}

		boolean archiveScheme = isArchiveScheme(scheme);
		if (archiveScheme) {
			j = uri.lastIndexOf(ARCHIVE_SEPARATOR);
			if (j == -1) {
				throw new IllegalArgumentException("no archive separator");
			}
			authority = uri.substring(i, ++j);
			i = j;
		} else if (uri.startsWith(AUTHORITY_SEPARATOR, i)) {
			i += AUTHORITY_SEPARATOR.length();
			j = find(uri, i, SEGMENT_END_HI, SEGMENT_END_LO);
			authority = uri.substring(i, j);
			i = j;
		} else if (scheme != null
				&& (i == uri.length() || uri.charAt(i) != SEGMENT_SEPARATOR)) {
			hierarchical = false;
			j = uri.indexOf(FRAGMENT_SEPARATOR, i);
			if (j == -1)
				j = uri.length();
			authority = uri.substring(i, j);
			i = j;
		}

		if (!archiveScheme && i < uri.length()
				&& uri.charAt(i) == SEGMENT_SEPARATOR) {
			j = find(uri, i + 1, SEGMENT_END_HI, SEGMENT_END_LO);
			String s = uri.substring(i + 1, j);

			if (!s.isEmpty() && s.charAt(s.length() - 1) == DEVICE_IDENTIFIER) {
				device = s;
				i = j;
			}
		}

		if (i < uri.length() && uri.charAt(i) == SEGMENT_SEPARATOR) {
			i++;
			absolutePath = true;
		}

		if (segmentsRemain(uri, i)) {
			List<String> segmentList = new ArrayList<String>();

			while (segmentsRemain(uri, i)) {
				j = find(uri, i, SEGMENT_END_HI, SEGMENT_END_LO);
				segmentList.add(uri.substring(i, j));
				i = j;

				if (i < uri.length() && uri.charAt(i) == SEGMENT_SEPARATOR) {
					if (!segmentsRemain(uri, ++i))
						segmentList.add(SEGMENT_EMPTY);
				}
			}
			segments = new String[segmentList.size()];
			segmentList.toArray(segments);
		}

		if (i < uri.length() && uri.charAt(i) == QUERY_SEPARATOR) {
			j = uri.indexOf(FRAGMENT_SEPARATOR, ++i);
			if (j == -1)
				j = uri.length();
			query = uri.substring(i, j);
			i = j;
		}

		if (i < uri.length()) // && uri.charAt(i) == FRAGMENT_SEPARATOR
		// (implied)
		{
			fragment = uri.substring(++i);
		}

		validateURI(hierarchical, scheme, authority, device, absolutePath,
				segments, query, fragment);
		return new URIImpl(hierarchical, scheme, authority, device,
				absolutePath, segments, query, fragment);
	}

	// Checks whether the string contains any more segments after the one that
	// starts at position i.
	private static boolean segmentsRemain(String uri, int i) {
		return i < uri.length() && uri.charAt(i) != QUERY_SEPARATOR
				&& uri.charAt(i) != FRAGMENT_SEPARATOR;
	}

	// Finds the next occurrence of one of the characters in the set represented
	// by the given bitmask in the given string, beginning at index i. The index
	// of the first found character, or s.length() if there is none, is
	// returned. Before searching, i is limited to the range [0, s.length()].
	//
	private static int find(String s, int i, long highBitmask, long lowBitmask) {
		int len = s.length();
		if (i >= len)
			return len;

		for (i = Math.max(i, 0); i < len; i++) {
			if (matches(s.charAt(i), highBitmask, lowBitmask))
				break;
		}
		return i;
	}

	/**
	 * Static factory method based on parsing a {@link java.io.File} path
	 * string. The <code>pathName</code> is converted into an appropriate form,
	 * as follows: platform specific path separators are converted to
	 * <code>/<code>; the path is encoded; and a "file" scheme and, if missing,
	 * a leading <code>/</code>, are added to an absolute path. The result is
	 * then parsed using {@link #createURI(String) createURI}.
	 *
	 * <p>
	 * The encoding step escapes all spaces, <code>#</code> characters, and
	 * other characters disallowed in URIs, as well as <code>?</code>, which
	 * would delimit a path from a query. Decoding is automatically performed by
	 * {@link URI#toFileString}, and can be applied to the values
	 * returned by other accessors by via the static {@link #decode(String)
	 * decode} method.
	 *
	 * <p>
	 * A relative path with a specified device (something like
	 * <code>C:myfile.txt</code>) cannot be expressed as a valid URI.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if <code>pathName</code> specifies a device and a relative
	 *                path, or if any component of the path is not valid
	 *                according to {@link #validAuthority validAuthority},
	 *                {@link #validDevice validDevice}, or
	 *                {@link #validSegments validSegments}, {@link #validQuery
	 *                validQuery}, or {@link #validFragment validFragment}.
	 */
	public static URI createFileURI(String pathName) {
		File file = new File(pathName);
		String uri = File.separatorChar != '/' ? pathName.replace(
				File.separatorChar, SEGMENT_SEPARATOR) : pathName;
		uri = encode(uri, PATH_CHAR_HI, PATH_CHAR_LO, false);
		if (file.isAbsolute()) {
			return createURI((uri.charAt(0) == SEGMENT_SEPARATOR ? "file:"
					: "file:/")
					+ uri);
		} else {
			URI result = createURI(uri);
			if (result.scheme() != null) {
				throw new IllegalArgumentException(
						"invalid relative pathName: " + pathName);
			}
			return result;
		}
	}

	/**
	 * Static factory method based on parsing a workspace-relative path string,
	 * with an option to encode the created URI.
	 *
	 * <p>
	 * The <code>pathName</code> must be of the form:
	 *
	 * <pre>
	 *   /project-name/path
	 * </pre>
	 *
	 * <p>
	 * Platform-specific path separators will be converted to slashes. If not
	 * included, the leading path separator will be added. The result will be of
	 * this form, which is parsed using {@link #createURI(String) createURI}:
	 *
	 * <pre>
	 *   platform:/resource/project-name/path
	 * </pre>
	 *
	 * <p>
	 * This scheme supports relocatable projects in Eclipse and in stand-alone
	 * EMF.
	 *
	 * <p>
	 * Depending on the <code>encode</code> argument, the path may be
	 * automatically encoded to escape all spaces, <code>#</code> characters,
	 * and other characters disallowed in URIs, as well as <code>?</code>, which
	 * would delimit a path from a query. Decoding can be performed with the
	 * static {@link #decode(String) decode} method.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if any component parsed from the path is not valid
	 *                according to {@link #validDevice validDevice},
	 *                {@link #validSegments validSegments}, {@link #validQuery
	 *                validQuery}, or {@link #validFragment validFragment}.
	 *
	 */
	public static URI createPlatformResourceURI(String pathName, boolean encode) {
		return createPlatformURI("platform:/resource", "platform:/resource/",
				pathName, encode);
	}

	/**
	 * Static factory method based on parsing a plug-in-based path string, with
	 * an option to encode the created URI.
	 *
	 * <p>
	 * The <code>pathName</code> must be of the form:
	 *
	 * <pre>
	 *   /plugin-id/path
	 * </pre>
	 *
	 * <p>
	 * Platform-specific path separators will be converted to slashes. If not
	 * included, the leading path separator will be added. The result will be of
	 * this form, which is parsed using {@link #createURI(String) createURI}:
	 *
	 * <pre>
	 *   platform:/plugin/plugin-id/path
	 * </pre>
	 *
	 * <p>
	 * This scheme supports relocatable plug-in content in Eclipse.
	 *
	 * <p>
	 * Depending on the <code>encode</code> argument, the path may be
	 * automatically encoded to escape all spaces, <code>#</code> characters,
	 * and other characters disallowed in URIs, as well as <code>?</code>, which
	 * would delimit a path from a query. Decoding can be performed with the
	 * static {@link #decode(String) decode} method.
	 *
	 * @exception java.lang.IllegalArgumentException
	 *                if any component parsed from the path is not valid
	 *                according to {@link #validDevice validDevice},
	 *                {@link #validSegments validSegments}, {@link #validQuery
	 *                validQuery}, or {@link #validFragment validFragment}.
	 *
	 */
	public static URI createPlatformPluginURI(String pathName, boolean encode) {
		return createPlatformURI("platform:/plugin", "platform:/plugin/",
				pathName, encode);
	}

	// Private constructor for use of platform factory methods.
	private static URI createPlatformURI(String unrootedBase,
	                                     String rootedBase, String pathName, boolean encode) {
		if (File.separatorChar != SEGMENT_SEPARATOR) {
			pathName = pathName.replace(File.separatorChar, SEGMENT_SEPARATOR);
		}

		if (encode) {
			pathName = encode(pathName, PATH_CHAR_HI, PATH_CHAR_LO, false);
		}
		return createURI((pathName.charAt(0) == SEGMENT_SEPARATOR ? unrootedBase
				: rootedBase)
				+ pathName);
	}

	// Validates all of the URI components. Factory methods should call this
	// before using the constructor, though they must ensure that the
	// inter-component requirements described in their own Javadocs are all
	// satisfied, themselves. If a new URI is being constructed out of
	// an existing URI, this need not be called. Instead, just the new
	// components may be validated individually.
	private static void validateURI(boolean hierarchical, String scheme,
	                                String authority, String device, boolean absolutePath,
	                                String[] segments, String query, String fragment) {
		if (!validScheme(scheme)) {
			throw new IllegalArgumentException("invalid scheme: " + scheme);
		}
		if (!hierarchical && !validOpaquePart(authority)) {
			throw new IllegalArgumentException("invalid opaquePart: "
					+ authority);
		}
		if (hierarchical && !isArchiveScheme(scheme)
				&& !validAuthority(authority)) {
			throw new IllegalArgumentException("invalid authority: "
					+ authority);
		}
		if (hierarchical && isArchiveScheme(scheme)
				&& !validArchiveAuthority(authority)) {
			throw new IllegalArgumentException("invalid authority: "
					+ authority);
		}
		if (!validDevice(device)) {
			throw new IllegalArgumentException("invalid device: " + device);
		}
		if (!validSegments(segments)) {
			String s = segments == null ? "invalid segments: null"
					: "invalid segment: " + firstInvalidSegment(segments);
			throw new IllegalArgumentException(s);
		}
		if (!validQuery(query)) {
			throw new IllegalArgumentException("invalid query: " + query);
		}
		if (!validFragment(fragment)) {
			throw new IllegalArgumentException("invalid fragment: " + fragment);
		}
	}

	// Returns null if the specified value is null or would be a valid path
	// segment array of a URI; otherwise, the value of the first invalid
	// segment.
	static String firstInvalidSegment(String[] value) {
		if (value == null)
			return null;
		for (String s : value) {
			if (!validSegment(s))
				return s;
		}
		return null;
	}

	// Alternate, stricter implementations of the following validation methods
	// are provided, commented out, for possible future use...

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the scheme component of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A valid scheme may be null or contain any characters except for the
	 * following: <code>: / ? #</code>
	 */
	public static boolean validScheme(String value) {
		return value == null
				|| !contains(value, MAJOR_SEPARATOR_HI, MAJOR_SEPARATOR_LO);

		// <p>A valid scheme may be null, or consist of a single letter followed
		// by any number of letters, numbers, and the following characters:
		// <code>+ - .</code>

		// if (value == null) return true;
		// return value.length() != 0 &&
		// matches(value.charAt(0), ALPHA_HI, ALPHA_LO) &&
		// validate(value, SCHEME_CHAR_HI, SCHEME_CHAR_LO, false, false);
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the opaque part component of a URI; <code>false</code>
	 * otherwise.
	 *
	 * <p>
	 * A valid opaque part must be non-null, non-empty, and not contain the
	 * <code>#</code> character. In addition, its first character must not be
	 * <code>/</code>
	 */
	public static boolean validOpaquePart(String value) {
		return value != null && value.indexOf(FRAGMENT_SEPARATOR) == -1
				&& !value.isEmpty() && value.charAt(0) != SEGMENT_SEPARATOR;

		// <p>A valid opaque part must be non-null and non-empty. It may contain
		// any allowed URI characters, but its first character may not be
		// <code>/</code>

		// return value != null && value.length() != 0 &&
		// value.charAt(0) != SEGMENT_SEPARATOR &&
		// validate(value, URIC_HI, URIC_LO, true, true);
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the authority component of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A valid authority may be null or contain any characters except for the
	 * following: <code>/ ? #</code>
	 */
	public static boolean validAuthority(String value) {
		return value == null
				|| !contains(value, SEGMENT_END_HI, SEGMENT_END_LO);

		// A valid authority may be null or contain any allowed URI characters
		// except
		// for the following: <code>/ ?</code>

		// return value == null || validate(value, SEGMENT_CHAR_HI,
		// SEGMENT_CHAR_LO, true, true);
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the authority component of an <a
	 * href="#archive_explanation">archive URI</a>; <code>false</code>
	 * otherwise.
	 *
	 * <p>
	 * To be valid, the authority, itself, must be a URI with no fragment,
	 * followed by the character <code>!</code>.
	 */
	public static boolean validArchiveAuthority(String value) {
		if (value != null && !value.isEmpty()
				&& value.charAt(value.length() - 1) == ARCHIVE_IDENTIFIER) {
			try {
				URI archiveURI = createURI(value.substring(0,
						value.length() - 1));
				return !archiveURI.hasFragment();
			} catch (IllegalArgumentException e) {
				// Ignore the exception and return false.
			}
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the device component of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A valid device may be null or non-empty, containing any characters except
	 * for the following: <code>/ ? #</code> In addition, its last character
	 * must be <code>:</code>
	 */
	public static boolean validDevice(String value) {
		if (value == null)
			return true;
		int len = value.length();
		return len > 0 && value.charAt(len - 1) == DEVICE_IDENTIFIER
				&& !contains(value, SEGMENT_END_HI, SEGMENT_END_LO);
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be a
	 * valid path segment of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A valid path segment must be non-null and not contain any of the
	 * following characters: <code>/ ? #</code>
	 */
	public static boolean validSegment(String value) {
		return value != null
				&& !contains(value, SEGMENT_END_HI, SEGMENT_END_LO);

		// <p>A valid path segment must be non-null and may contain any allowed
		// URI
		// characters except for the following: <code>/ ?</code>

		// return value != null && validate(value, SEGMENT_CHAR_HI,
		// SEGMENT_CHAR_LO, true, true);
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be a
	 * valid path segment array of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A valid path segment array must be non-null and contain only path
	 * segments that are valid according to {@link #validSegment validSegment}.
	 */
	public static boolean validSegments(String[] value) {
		if (value == null)
			return false;
		for (String s : value) {
			if (!validSegment(s))
				return false;
		}
		return true;
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the query component of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A valid query may be null or contain any characters except for
	 * <code>#</code>
	 */
	public static boolean validQuery(String value) {
		return value == null || value.indexOf(FRAGMENT_SEPARATOR) == -1;

		// <p>A valid query may be null or contain any allowed URI characters.

		// return value == null || validate(value, URIC_HI, URIC_LO, true,
		// true);
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the fragment component of a URI; <code>false</code> otherwise.
	 *
	 * <p>
	 * A fragment is taken to be unconditionally valid.
	 */
	public static boolean validFragment(String value) {
		return true;

		// <p>A valid fragment may be null or contain any allowed URI
		// characters.

		// return value == null || validate(value, URIC_HI, URIC_LO, true,
		// true);
	}

	// Tests the non-null string value to see if it contains only ASCII
	// characters in the set represented by the specified 128-bit bitmask,
	// as well as, optionally, non-ASCII characters 0xA0 and above, and,
	// also optionally, escape sequences of % followed by two hex digits.
	// This method is used for the new, strict URI validation that is not
	// not currently in place.
	/*
	 * private static boolean validate(String value, long highBitmask, long
	 * lowBitmask, boolean allowNonASCII, boolean allowEscaped) { for (int i =
	 * 0, length = value.length(); i < length; i++) { char c = value.charAt(i);
	 *
	 * if (matches(c, highBitmask, lowBitmask)) continue; if (allowNonASCII && c
	 * >= 160) continue; if (allowEscaped && isEscaped(value, i)) { i += 2;
	 * continue; } return false; } return true; }
	 */

	// Searches the specified string for any characters in the set represented
	// by the 128-bit bitmask. Returns true if any occur, or false otherwise.
	private static boolean contains(String s, long highBitmask, long lowBitmask) {
		for (int i = 0, len = s.length(); i < len; i++) {
			if (matches(s.charAt(i), highBitmask, lowBitmask))
				return true;
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if the specified <code>value</code> would be
	 * valid as the scheme of an <a href="#archive_explanation">archive URI</a>;
	 * <code>false</code> otherwise.
	 */
	public static boolean isArchiveScheme(String value) {
		// Returns true if the given value is an archive scheme, as defined by
		// the org.eclipse.emf.common.util.URI.archiveSchemes system property.
		// By default, "jar", "zip", and "archive" are considered archives.
		return value != null && archiveSchemes.contains(value.toLowerCase());
	}

	/**
	 * Encodes a string so as to produce a valid opaque part value, as defined
	 * by the RFC. All excluded characters, such as space and <code>#</code>,
	 * are escaped, as is <code>/</code> if it is the first character.
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 */
	public static String encodeOpaquePart(String value, boolean ignoreEscaped) {
		String result = encode(value, URIC_HI, URIC_LO, ignoreEscaped);
		return result != null && !result.isEmpty()
				&& result.charAt(0) == SEGMENT_SEPARATOR ? "%2F"
				+ result.substring(1) : result;
	}

	/**
	 * Encodes a string so as to produce a valid authority, as defined by the
	 * RFC. All excluded characters, such as space and <code>#</code>, are
	 * escaped, as are <code>/</code> and <code>?</code>
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 */
	public static String encodeAuthority(String value, boolean ignoreEscaped) {
		return encode(value, SEGMENT_CHAR_HI, SEGMENT_CHAR_LO, ignoreEscaped);
	}

	/**
	 * Encodes a string so as to produce a valid segment, as defined by the RFC.
	 * All excluded characters, such as space and <code>#</code>, are escaped,
	 * as are <code>/</code> and <code>?</code>
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 */
	public static String encodeSegment(String value, boolean ignoreEscaped) {
		return encode(value, SEGMENT_CHAR_HI, SEGMENT_CHAR_LO, ignoreEscaped);
	}

	/**
	 * Encodes a string so as to produce a valid query, as defined by the RFC.
	 * Only excluded characters, such as space and <code>#</code>, are escaped.
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 */
	public static String encodeQuery(String value, boolean ignoreEscaped) {
		return encode(value, URIC_HI, URIC_LO, ignoreEscaped);
	}

	/**
	 * Encodes a string so as to produce a valid fragment, as defined by the
	 * RFC. Only excluded characters, such as space and <code>#</code>, are
	 * escaped.
	 *
	 * @param ignoreEscaped
	 *            <code>true</code> to leave <code>%</code> characters unescaped
	 *            if they already begin a valid three-character escape sequence;
	 *            <code>false</code> to encode all <code>%</code> characters.
	 *            Note that if a <code>%</code> is not followed by 2 hex digits,
	 *            it will always be escaped.
	 */
	public static String encodeFragment(String value, boolean ignoreEscaped) {
		return encode(value, URIC_HI, URIC_LO, ignoreEscaped);
	}

	// Encodes a complete URI, optionally leaving % characters unescaped when
	// beginning a valid three-character escape sequence. We can either treat
	// the first or # as a fragment separator, or encode them all.
	private static String encodeURI(String uri, boolean ignoreEscaped,
	                                int fragmentLocationStyle) {
		if (uri == null)
			return null;

		StringBuilder result = new StringBuilder();

		int i = uri.indexOf(SCHEME_SEPARATOR);
		if (i != -1) {
			String scheme = uri.substring(0, i);
			result.append(scheme);
			result.append(SCHEME_SEPARATOR);
		}

		int j = fragmentLocationStyle == URI.FRAGMENT_FIRST_SEPARATOR ? uri
				.indexOf(FRAGMENT_SEPARATOR)
				: fragmentLocationStyle == URI.FRAGMENT_LAST_SEPARATOR ? uri
				.lastIndexOf(FRAGMENT_SEPARATOR) : -1;

		if (j != -1) {
			String sspart = uri.substring(++i, j);
			result.append(encode(sspart, URIC_HI, URIC_LO, ignoreEscaped));
			result.append(FRAGMENT_SEPARATOR);

			String fragment = uri.substring(++j);
			result.append(encode(fragment, URIC_HI, URIC_LO, ignoreEscaped));
		} else {
			String sspart = uri.substring(++i);
			result.append(encode(sspart, URIC_HI, URIC_LO, ignoreEscaped));
		}

		return result.toString();
	}

	// Encodes the given string, replacing each ASCII character that is not in
	// the set specified by the 128-bit bitmask and each non-ASCII character
	// below 0xA0 by an escape sequence of % followed by two hex digits. If
	// % is not in the set but ignoreEscaped is true, then % will not be encoded
	// iff it already begins a valid escape sequence.
	private static String encode(String value, long highBitmask,
	                             long lowBitmask, boolean ignoreEscaped) {
		if (value == null)
			return null;

		StringBuffer result = null;

		for (int i = 0, len = value.length(); i < len; i++) {
			char c = value.charAt(i);

			if (!matches(c, highBitmask, lowBitmask) && c < 160
					&& (!ignoreEscaped || !isEscaped(value, i))) {
				if (result == null) {
					result = new StringBuffer(value.substring(0, i));
				}
				appendEscaped(result, (byte) c);
			} else if (result != null) {
				result.append(c);
			}
		}
		return result == null ? value : result.toString();
	}

	// Tests whether an escape occurs in the given string, starting at index i.
	// An escape sequence is a % followed by two hex digits.
	private static boolean isEscaped(String s, int i) {
		return s.charAt(i) == ESCAPE && s.length() > i + 2
				&& matches(s.charAt(i + 1), HEX_HI, HEX_LO)
				&& matches(s.charAt(i + 2), HEX_HI, HEX_LO);
	}

	// Computes a three-character escape sequence for the byte, appending
	// it to the StringBuffer. Only characters up to 0xFF should be escaped;
	// all but the least significant byte will be ignored.
	private static void appendEscaped(StringBuffer result, byte b) {
		result.append(ESCAPE);

		// The byte is automatically widened into an int, with sign extension,
		// for shifting. This can introduce 1's to the left of the byte, which
		// must be cleared by masking before looking up the hex digit.
		//
		result.append(HEX_DIGITS[(b >> 4) & 0x0F]);
		result.append(HEX_DIGITS[b & 0x0F]);
	}

	/**
	 * Decodes the given string by interpreting three-digit escape sequences as
	 * the bytes of a UTF-8 encoded character and replacing them with the
	 * characters they represent. Incomplete escape sequences are ignored and
	 * invalid UTF-8 encoded bytes are treated as extended ASCII characters.
	 */
	public static String decode(String value) {
		if (value == null)
			return null;

		int i = value.indexOf('%');
		if (i < 0) {
			return value;
		} else {
			StringBuilder result = new StringBuilder(value.substring(0, i));
			byte[] bytes = new byte[4];
			int receivedBytes = 0;
			int expectedBytes = 0;
			for (int len = value.length(); i < len; i++) {
				if (isEscaped(value, i)) {
					char character = unescape(value.charAt(i + 1),
							value.charAt(i + 2));
					i += 2;

					if (expectedBytes > 0) {
						if ((character & 0xC0) == 0x80) {
							bytes[receivedBytes++] = (byte) character;
						} else {
							expectedBytes = 0;
						}
					} else if (character >= 0x80) {
						if ((character & 0xE0) == 0xC0) {
							bytes[receivedBytes++] = (byte) character;
							expectedBytes = 2;
						} else if ((character & 0xF0) == 0xE0) {
							bytes[receivedBytes++] = (byte) character;
							expectedBytes = 3;
						} else if ((character & 0xF8) == 0xF0) {
							bytes[receivedBytes++] = (byte) character;
							expectedBytes = 4;
						}
					}

					if (expectedBytes > 0) {
						if (receivedBytes == expectedBytes) {
							switch (receivedBytes) {
								case 2: {
									result.append((char) ((bytes[0] & 0x1F) << 6 | bytes[1] & 0x3F));
									break;
								}
								case 3: {
									result.append((char) ((bytes[0] & 0xF) << 12
											| (bytes[1] & 0X3F) << 6 | bytes[2] & 0x3F));
									break;
								}
								case 4: {
									result.appendCodePoint(((bytes[0] & 0x7) << 18
											| (bytes[1] & 0X3F) << 12
											| (bytes[2] & 0X3F) << 6 | bytes[3] & 0x3F));
									break;
								}
							}
							receivedBytes = 0;
							expectedBytes = 0;
						}
					} else {
						for (int j = 0; j < receivedBytes; ++j) {
							result.append((char) bytes[j]);
						}
						receivedBytes = 0;
						result.append(character);
					}
				} else {
					for (int j = 0; j < receivedBytes; ++j) {
						result.append((char) bytes[j]);
					}
					receivedBytes = 0;
					result.append(value.charAt(i));
				}
			}
			return result.toString();
		}
	}

	// Returns the character encoded by % followed by the two given hex digits,
	// which is always 0xFF or less, so can safely be casted to a byte. If
	// either character is not a hex digit, a bogus result will be returned.
	private static char unescape(char highHexDigit, char lowHexDigit) {
		return (char) ((valueOf(highHexDigit) << 4) | valueOf(lowHexDigit));
	}

	// Returns the int value of the given hex digit.
	private static int valueOf(char hexDigit) {
		if (hexDigit >= 'A' && hexDigit <= 'F') {
			return hexDigit - 'A' + 10;
		}
		if (hexDigit >= 'a' && hexDigit <= 'f') {
			return hexDigit - 'a' + 10;
		}
		if (hexDigit >= '0' && hexDigit <= '9') {
			return hexDigit - '0';
		}
		return 0;
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
}
