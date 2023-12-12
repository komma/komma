/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id$
 */
package net.enilink.komma.common.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.enilink.komma.core.URIs;

/**
 * A connection that can access an entry in an archive, and then recursively an
 * entry in that archive, and so on. For example, it can be used just like jar:
 * or zip:, only the archive paths can repeat, e.g.,
 * 
 * <pre>
 *  archive:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
 * </pre>
 * 
 * The general recursive pattern is
 * 
 * <pre>
 *  archive:$nestedURL${/!$archivePath$}+
 * </pre>
 * 
 * So the nested URL for the example above is
 * 
 * <pre>
 *  file:///c:/temp/example.zip
 * </pre>
 * 
 * <p>
 * Since the nested URL may itself contain archive schemes, the subsequence of
 * the archive paths that should be associated with the nested URL is determined
 * by finding the nth archive separator, i.e., the nth !/, where n is the number
 * of ":"s before the first "/" of the nested URL, i.e., the number of nested
 * schemes. For example, for a more complex case where the nested URL is itself
 * an archive-based scheme, e.g.,
 * 
 * <pre>
 *  archive:jar:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
 * </pre>
 * 
 * the nested URL is correctly parsed to skip to the second archive separator as
 * 
 * <pre>
 *  jar:file:///c:/temp/example.zip!/org/example/nested.zip
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * The logic for accessing archives can be tailored and reused independant from
 * its usage as a URL connection. This is normally done by using the constructor
 * {@link #ArchiveURLConnection(String)} and overriding
 * {@link #createInputStream(String)} and {@link #createOutputStream(String)}.
 * The behavior can be tailored by overriding {@link #emulateArchiveScheme()}
 * and {@link #useZipFile()}.
 * </p>
 */
public class ArchiveURLConnection extends URLConnection {
	/**
	 * The cached string version of the {@link #url URL}.
	 */
	protected String urlString;

	/**
	 * Constructs a new connection for the URL.
	 * 
	 * @param url
	 *            the URL of this connection.
	 */
	public ArchiveURLConnection(URL url) {
		super(url);
		urlString = url.toString();
	}

	/**
	 * Constructs a new archive accessor. This constructor forwards a null URL
	 * to be super constructor, so an instance built with this constructor
	 * <b>cannot</b> be used as a URLConnection. The logic for accessing
	 * archives and for delegating to the nested URL can be reused in other
	 * applications, without creating an URLs.
	 * 
	 * @param url
	 *            the URL of the archive.
	 */
	protected ArchiveURLConnection(String url) {
		super(null);
		urlString = url;
	}

	/**
	 * </p> Returns whether the implementation will handle all the archive
	 * accessors directly. For example, whether
	 * 
	 * <pre>
	 *  archive:jar:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
	 * </pre>
	 * 
	 * will be handled as if it were specified as
	 * 
	 * <pre>
	 *  archive:file:///c:/temp/example.zip!/org/example/nested.zip!/org/example/deeply-nested.html
	 * </pre>
	 * 
	 * Override this only if you are reusing the logic of retrieving an input
	 * stream into an archive and hence are likely to be overriding
	 * createInputStream, which is the point of delegation to the nested URL for
	 * recursive stream creation. </p>
	 * 
	 * @return whether the implementation will handle all the archive accessors
	 *         directly.
	 */
	protected boolean emulateArchiveScheme() {
		return false;
	}

	/**
	 * Returns whether to handle the special case of a nested URL with file:
	 * schema using a {@link ZipFile}. This gives more efficient direct access
	 * to the root entry, e.g.,
	 * 
	 * <pre>
	 *  archive:file:///c:/temp/example.zip!/org/example/nested.html
	 * </pre>
	 * 
	 * @return whether to handle the special case of a nested URL with file:
	 *         schema using a ZipFile.
	 */
	protected boolean useZipFile() {
		return false;
	}

	/**
	 * Record that this is connected.
	 */
	@Override
	public void connect() throws IOException {
		connected = true;
	}

	protected String getNestedURL() throws IOException {
		// There must be at least one archive path.
		//
		int archiveSeparator = urlString.indexOf("!/");
		if (archiveSeparator < 0) {
			throw new MalformedURLException("missing archive separators "
					+ urlString);
		}

		// There needs to be another URL protocol right after the archive
		// protocol, and not a "/".
		//
		int start = urlString.indexOf(':') + 1;
		if (start > urlString.length() || urlString.charAt(start) == '/') {
			throw new IllegalArgumentException(
					"archive protocol must be immediately followed by another URL protocol "
							+ urlString);
		}

		// Parse to extract the archives that will be delegated to the nested
		// URL based on the number of schemes at the start.
		//
		for (int i = start, end = urlString.indexOf("/") - 1; (i = urlString
				.indexOf(":", i)) < end;) {
			if (emulateArchiveScheme()) {
				// Skip a scheme for the archive accessor to be handled directly
				// here.
				//
				start = ++i;
			} else {
				// Skip an archive accessor to be handled by delegation to the
				// scheme in nested URL.
				//
				archiveSeparator = urlString
						.indexOf("!/", archiveSeparator + 2);
				if (archiveSeparator < 0) {
					throw new MalformedURLException(
							"too few archive separators " + urlString);
				}
				++i;
			}
		}

		return urlString.substring(start, archiveSeparator);
	}

	/**
	 * Creates the input stream for the URL.
	 * 
	 * @return the input stream for the URL.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		// Create the delegate URL.
		//
		String nestedURL = getNestedURL();

		// The cutoff point to the next archive.
		//
		int archiveSeparator = urlString.indexOf(nestedURL)
				+ nestedURL.length();
		int nextArchiveSeparator = urlString
				.indexOf("!/", archiveSeparator + 2);

		// Construct the input stream in a special efficient way for case of a
		// file scheme.
		//
		InputStream inputStream;
		ZipEntry inputZipEntry = null;
		if (!useZipFile() || !nestedURL.startsWith("file:")) {
			// Just get the stream from the URL.
			//
			inputStream = createInputStream(nestedURL);
		} else {
			// The name to be used for the entry.
			//
			String entry = URIs.decode(nextArchiveSeparator < 0 ? urlString
					.substring(archiveSeparator + 2) : urlString.substring(
					archiveSeparator + 2, nextArchiveSeparator));

			// Skip over this archive path to the next one, since we are
			// handling this one special.
			//
			archiveSeparator = nextArchiveSeparator;
			nextArchiveSeparator = urlString
					.indexOf("!/", archiveSeparator + 2);

			// Go directly to the right entry in the zip file,
			// get the stream,
			// and wrap it so that closing it closes the zip file.
			//
			final ZipFile zipFile = new ZipFile(URIs.decode(nestedURL
					.substring(5)));
			inputZipEntry = zipFile.getEntry(entry);
			InputStream zipEntryInputStream = inputZipEntry == null ? null
					: zipFile.getInputStream(inputZipEntry);
			if (zipEntryInputStream == null) {
				throw new IOException("Archive entry not found " + urlString);
			}
			inputStream = new FilterInputStream(zipEntryInputStream) {
				@Override
				public void close() throws IOException {
					super.close();
					zipFile.close();
				}
			};
		}

		// Loop over the archive paths.
		//
		LOOP: while (archiveSeparator > 0) {
			inputZipEntry = null;

			// The entry name to be matched.
			//
			String entry = URIs.decode(nextArchiveSeparator < 0 ? urlString
					.substring(archiveSeparator + 2) : urlString.substring(
					archiveSeparator + 2, nextArchiveSeparator));

			// Wrap the input stream as a zip stream to scan it's contents for a
			// match.
			//
			ZipInputStream zipInputStream = new ZipInputStream(inputStream);
			while (zipInputStream.available() >= 0) {
				ZipEntry zipEntry = zipInputStream.getNextEntry();
				if (zipEntry == null) {
					break;
				} else if (entry.equals(zipEntry.getName())) {
					inputZipEntry = zipEntry;
					inputStream = zipInputStream;

					// Skip to the next archive path and continue the loop.
					//
					archiveSeparator = nextArchiveSeparator;
					nextArchiveSeparator = urlString.indexOf("!/",
							archiveSeparator + 2);
					continue LOOP;
				}
			}

			zipInputStream.close();
			throw new IOException("Archive entry not found " + urlString);
		}

		return deliver(inputZipEntry, inputStream);
	}

	protected InputStream deliver(ZipEntry zipEntry, InputStream inputStream)
			throws IOException {
		return inputStream;
	}

	/**
	 * Creates an input stream for the nested URL by calling
	 * {@link URL#openStream() opening} a stream on it.
	 * 
	 * @param nestedURL
	 *            the nested URL for which a stream is required.
	 * @return the open stream of the nested URL.
	 */
	protected InputStream createInputStream(String nestedURL)
			throws IOException {
		return new URL(nestedURL).openStream();
	}

	/**
	 * Creates the output stream for the URL.
	 * 
	 * @return the output stream for the URL.
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return getOutputStream(false, -1);
	}

	public void delete() throws IOException {
		getOutputStream(true, -1).close();
	}

	public void setTimeStamp(long timeStamp) throws IOException {
		getOutputStream(false, timeStamp).close();
	}

	@SuppressWarnings("resource")
	private OutputStream getOutputStream(boolean delete, long timeStamp)
			throws IOException {
		// Create the delegate URL
		//
		final String nestedURL = getNestedURL();

		// Create a temporary file where the existing contents of the archive
		// can be written
		// before the new contents are added.
		//
		final File tempFile = File.createTempFile("Archive", "zip");

		// Record the input and output streams for closing in case of failure so
		// that handles are not left open.
		//
		InputStream sourceInputStream = null;
		OutputStream tempOutputStream = null;
		try {
			// Create the output stream to the temporary file and the input
			// stream for the delegate URL.
			//
			tempOutputStream = new FileOutputStream(tempFile);
			try {
				sourceInputStream = createInputStream(nestedURL);
			} catch (IOException exception) {
				// Continue processing if the file doesn't exist so that we try
				// create a new empty one.
			}

			// Record them as generic streams to record state during the loop
			// that emulates recursion.
			//
			OutputStream outputStream = tempOutputStream;
			InputStream inputStream = sourceInputStream;

			// The cutoff point to the next archive.
			//
			int archiveSeparator = urlString.indexOf(nestedURL)
					+ nestedURL.length();
			int nextArchiveSeparator = urlString.indexOf("!/",
					archiveSeparator + 2);

			// The most deeply nested output stream that will be returned
			// wrapped as the result.
			//
			ZipOutputStream zipOutputStream;

			// A buffer for transferring archive contents.
			//
			final byte[] bytes = new byte[4096];

			// We expect there to be at least one archive path.
			//
			ZipEntry outputZipEntry;
			boolean found = false;
			for (;;) {
				// The name that will be used as the archive entry.
				//
				String entry = URIs.decode(nextArchiveSeparator < 0 ? urlString
						.substring(archiveSeparator + 2) : urlString.substring(
						archiveSeparator + 2, nextArchiveSeparator));

				// Wrap the current result as a zip stream, and record it for
				// loop-based recursion.
				//
				zipOutputStream = null;

				// Wrap the current input as a zip stream, and record it for
				// loop-based recursion.
				//
				ZipInputStream zipInputStream = inputStream == null ? null
						: new ZipInputStream(inputStream);
				inputStream = zipInputStream;

				// Loop over the entries in the zip stream.
				//
				while (zipInputStream != null
						&& zipInputStream.available() >= 0) {
					// If this entry isn't the end marker
					// and isn't the matching one that we are replacing...
					//
					ZipEntry zipEntry = zipInputStream.getNextEntry();
					if (zipEntry == null) {
						break;
					} else {
						boolean match = entry.equals(zipEntry.getName());
						if (!found) {
							found = match && nextArchiveSeparator < 0;
						}
						if (timeStamp != -1 || !match) {
							if (zipOutputStream == null) {
								zipOutputStream = new ZipOutputStream(
										outputStream);
								outputStream = zipOutputStream;
							}
							// Transfer the entry and its contents.
							//
							if (timeStamp != -1 && match
									&& nextArchiveSeparator < 0) {
								zipEntry.setTime(timeStamp);
							}
							zipOutputStream.putNextEntry(zipEntry);
							for (int size; (size = zipInputStream.read(bytes,
									0, bytes.length)) > -1;) {
								zipOutputStream.write(bytes, 0, size);
							}
						}
					}
				}

				// Find the next archive path and continue "recursively" if
				// there is one.
				//
				archiveSeparator = nextArchiveSeparator;
				nextArchiveSeparator = urlString.indexOf("!/",
						archiveSeparator + 2);

				if ((delete || timeStamp != -1) && archiveSeparator < 0) {
					if (!found) {
						throw new IOException("Archive entry not found "
								+ urlString);
					}
					// Create no entry since we are deleting and return
					// immediately.
					//
					outputZipEntry = null;
					break;
				} else {
					// Create a new or replaced entry and continue processing
					// the remaining archives.
					//
					outputZipEntry = new ZipEntry(entry);
					if (zipOutputStream == null) {
						zipOutputStream = new ZipOutputStream(outputStream);
						outputStream = zipOutputStream;
					}
					zipOutputStream.putNextEntry(outputZipEntry);
					if (archiveSeparator > 0) {
						continue;
					} else {
						break;
					}
				}
			}

			// Ensure that it won't be closed in the finally block.
			//
			tempOutputStream = null;

			// Wrap the deepest result so that on close, the results are finally
			// transferred.
			//
			final boolean deleteRequired = sourceInputStream != null;
			FilterOutputStream result = new FilterOutputStream(
					zipOutputStream == null ? outputStream : zipOutputStream) {
				protected boolean isClosed;

				@Override
				public void close() throws IOException {
					// Make sure we close only once.
					//
					if (!isClosed) {
						isClosed = true;

						// Close for real so that the temporary file is ready to
						// be read.
						//
						super.close();

						boolean useRenameTo = nestedURL.startsWith("file:");

						// If the delegate URI can be handled as a file,
						// we'll hope that renaming it will be really efficient.
						//
						if (useRenameTo) {
							File targetFile = new File(URIs.decode(nestedURL
									.substring(5)));
							if (deleteRequired && !targetFile.delete()) {
								throw new IOException("cannot delete "
										+ targetFile.getPath());
							} else if (!tempFile.renameTo(targetFile)) {
								useRenameTo = false;
							}
						}
						if (!useRenameTo) {
							// Try to transfer it by reading the contents of the
							// temporary file
							// and writing them to the output stream of the
							// delegate.
							//
							InputStream inputStream = null;
							OutputStream outputStream = null;
							try {
								inputStream = new FileInputStream(tempFile);
								outputStream = createOutputStream(nestedURL);
								for (int size; (size = inputStream.read(bytes,
										0, bytes.length)) > -1;) {
									outputStream.write(bytes, 0, size);
								}
							} finally {
								// Make sure they are closed no matter what bad
								// thing happens.
								//
								if (inputStream != null) {
									inputStream.close();
								}
								if (outputStream != null) {
									outputStream.close();
								}
							}
						}
					}
				}
			};
			return outputZipEntry == null ? result : deliver(outputZipEntry,
					result);
		} finally {
			// Close in case of failure to complete.
			//
			if (tempOutputStream != null) {
				tempOutputStream.close();
			}

			// Close if we created this.
			//
			if (sourceInputStream != null) {
				sourceInputStream.close();
			}
		}
	}

	protected OutputStream deliver(ZipEntry zipEntry, OutputStream outputStream)
			throws IOException {
		return outputStream;
	}

	/**
	 * Creates an output stream for the nested URL by calling
	 * {@link URL#openConnection() opening} a stream on it.
	 * 
	 * @param nestedURL
	 *            the nested URL for which a stream is required.
	 * @return the open stream of the nested URL.
	 */
	protected OutputStream createOutputStream(String nestedURL)
			throws IOException {
		URL url = new URL(nestedURL);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setDoOutput(true);
		return urlConnection.getOutputStream();
	}
}
