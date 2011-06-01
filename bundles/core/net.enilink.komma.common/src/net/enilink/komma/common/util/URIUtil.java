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
package net.enilink.komma.common.util;

import javax.xml.namespace.QName;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class URIUtil {
	public static QName toQName(URI uri) {
		String uriString = uri.toString();
		int localNameIdx = URIUtil.getLocalNameIndex(uriString);

		String namespace = uri.toString().substring(0, localNameIdx);
		String localName = uriString.substring(localNameIdx);
		return new QName(namespace, localName);
	}

	public static URI toUri(QName qName) {
		String namespace = qName.getNamespaceURI();
		if (!(namespace.endsWith("#") || namespace.endsWith("/"))
				&& qName.getLocalPart().length() > 0) {
			namespace += "#";
		}
		return URIImpl.createURI(namespace + qName.getLocalPart());
	}

	public static String namespaceToModelUri(String namespace) {
		String uri = namespace;
		if (uri.endsWith("#")) {
			uri = uri.substring(0, uri.length() - 1);
		}

		return uri;
	}

	public static String modelUriToNamespace(String uri) {
		String namespace = uri;
		if (!namespace.endsWith("#")) {
			namespace = namespace + "#";
		}

		return namespace;
	}

	/**
	 * Finds the index of the first local name character in an (non-relative)
	 * URI. This index is determined by the following the following steps:
	 * <ul>
	 * <li>Find the <em>first</em> occurrence of the '#' character,
	 * <li>If this fails, find the <em>last</em> occurrence of the '/'
	 * character,
	 * <li>If this fails, find the <em>last</em> occurrence of the ':'
	 * character.
	 * <li>Add <tt>1<tt> to the found index and return this value.
	 * </ul>
	 * Note that the third step should never fail as every legal (non-relative)
	 * URI contains at least one ':' character to seperate the scheme from the
	 * rest of the URI. If this fails anyway, the method will throw an
	 * {@link IllegalArgumentException}.
	 * 
	 * @param uri
	 *            A URI string.
	 * @return The index of the first local name character in the URI string.
	 *         Note that this index does not reference an actual character if
	 *         the algorithm determines that there is not local name. In that
	 *         case, the return index is equal to the length of the URI string.
	 * @throws IllegalArgumentException
	 *             If the supplied URI string doesn't contain any of the
	 *             separator characters. Every legal (non-relative) URI contains
	 *             at least one ':' character to seperate the scheme from the
	 *             rest of the URI.
	 */
	public static int getLocalNameIndex(String uri) {
		int separatorIdx = uri.indexOf('#');

		if (separatorIdx < 0) {
			separatorIdx = uri.lastIndexOf('/');
		}

		if (separatorIdx < 0) {
			separatorIdx = uri.lastIndexOf(':');
		}

		if (separatorIdx < 0) {
			throw new IllegalArgumentException(
					"No separator character founds in URI: " + uri);
		}

		return separatorIdx + 1;
	}
}
