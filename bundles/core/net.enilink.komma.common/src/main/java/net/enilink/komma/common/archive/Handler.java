/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id$
 */
package net.enilink.komma.common.archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * A URL stream handler that can be {@link #register() registered} to support
 * archive access protocol. It uses {@link ArchiveURLConnection} to implement
 * the connection.
 */
public class Handler extends URLStreamHandler {
	/**
	 * Registers this class. A handler for protocol "xyz" is registered by
	 * providing a class named Handler implementing {@link URLStreamHandler} in
	 * a package called named xyz in a package of your choosing, and then
	 * registering that chosen prefix package name in the system property for
	 * <code>"java.protocol.handler.pkgs"</code>, which is an "|" separated list
	 * of package prefixes to search for handlers.
	 */
	public static void register() {
		String javaProtocolHandlerPkgs = System
				.getProperty("java.protocol.handler.pkgs");
		if (javaProtocolHandlerPkgs == null
				|| javaProtocolHandlerPkgs.length() == 0) {
			javaProtocolHandlerPkgs = "org.eclipse.emf.common";
		} else {
			javaProtocolHandlerPkgs += "|org.eclipse.emf.common";
		}
		System.setProperty("java.protocol.handler.pkgs",
				javaProtocolHandlerPkgs);
	}

	/**
	 * Registers this handler and interprets each argument as URL to be opened,
	 * read in, and written out to System.out.
	 * 
	 * @param args
	 *            URLs to open, read, and write to System.out
	 * @throws IOException
	 *             if there are problems opening or reading from the URLs, or
	 *             writing to System.out.
	 */
	public static void main(String[] args) throws IOException {
		register();

		for (int i = 0; i < args.length; ++i) {
			InputStream inputStream = new URL(args[i]).openStream();
			byte[] bytes = new byte[4048];
			for (int size; (size = inputStream.read(bytes, 0, bytes.length)) > -1;) {
				System.out.write(bytes, 0, size);
			}
		}
	}

	/**
	 * Creates an instance.
	 */
	public Handler() {
		super();
	}

	/**
	 * Overrides parsing the URL to validate constraints on well formed archive
	 * syntax.
	 * 
	 * @see URLStreamHandler#parseURL(java.net.URL, java.lang.String, int, int)
	 */
	@Override
	protected void parseURL(URL url, String specification, int start, int limit) {
		super.parseURL(url, specification, start, limit);

		// There needs to be another URL protocol right after the archive
		// protocol, and not a "/".
		//
		if (start > limit || specification.charAt(start) == '/') {
			throw new IllegalArgumentException(
					"archive protocol must be immediately followed by another URL protocol "
							+ specification);
		}

		// There must be at least one archive path.
		//
		int archiveSeparator = specification.indexOf("!/", start);
		if (archiveSeparator < 0) {
			throw new IllegalArgumentException("missing archive separators "
					+ specification.substring(start, limit));
		}

		// Parse to count the archive paths that must will be delegated to the
		// nested URL based on the number of schemes at the start.
		//
		for (int i = start, end = specification.indexOf('/', start) - 1; (i = specification
				.indexOf(':', i)) < end; ++i) {
			// There should be at least one archive separator per scheme.
			//
			archiveSeparator = specification
					.indexOf("!/", archiveSeparator + 2);
			if (archiveSeparator < 0) {
				throw new IllegalArgumentException(
						"too few archive separators " + specification);
			}
		}
	}

	/**
	 * Returns a new {@link ArchiveURLConnection}.
	 */
	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		return new ArchiveURLConnection(url);
	}
}
