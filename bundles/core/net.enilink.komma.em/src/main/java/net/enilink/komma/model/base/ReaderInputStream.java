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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderInputStream extends InputStream {
	private static final Pattern XML_HEADER = Pattern
			.compile("<\\?xml\\s+(?:version\\s*=\\s*\"[^\"]*\"\\s+)encoding\\s*=\\s*\"\\s*([^\\s\"]*)\"\\s*\\?>");

	public static String getEncoding(String xmlString) {
		Matcher matcher = XML_HEADER.matcher(xmlString);
		return matcher.lookingAt() ? matcher.group(1) : null;
	}

	/**
	 * @since 2.4
	 */
	public static String getEncoding(Reader xmlReader) {
		try {
			xmlReader.mark(100);
			char[] buffer = new char[100];
			int length = xmlReader.read(buffer);
			if (length > -1) {
				Matcher matcher = XML_HEADER.matcher(new String(buffer, 0,
						length));
				return matcher.lookingAt() ? matcher.group(1) : null;
			} else {
				return null;
			}
		} catch (IOException exception) {
			return null;
		} finally {
			try {
				xmlReader.reset();
			} catch (IOException exception) {
				// Ignore.
			}
		}
	}

	protected String encoding;
	protected Reader reader;
	protected Buffer buffer;

	public ReaderInputStream(Reader reader, String encoding) {
		this.reader = reader;
		this.encoding = encoding;
	}

	/**
	 * @since 2.4
	 */
	public ReaderInputStream(Reader xmlReader) {
		this.reader = xmlReader.markSupported() ? xmlReader
				: new BufferedReader(xmlReader);
		this.encoding = getEncoding(this.reader);
	}

	public ReaderInputStream(String string, String encoding) {
		this(new StringReader(string), encoding);
	}

	public ReaderInputStream(String xmlString) {
		this(new StringReader(xmlString), getEncoding(xmlString));
	}

	@Override
	public int read() throws IOException {
		if (buffer == null) {
			buffer = new Buffer(100);
		}

		return buffer.read();
	}

	public Reader asReader() {
		return reader;
	}

	public String getEncoding() {
		return encoding;
	}

	@Override
	public void close() throws IOException {
		super.close();
		reader.close();
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		reader.reset();
	}

	protected class Buffer extends ByteArrayOutputStream {
		protected int index;
		protected char[] characters;
		protected OutputStreamWriter writer;

		public Buffer(int size) throws IOException {
			super(size);
			characters = new char[size];
			writer = new OutputStreamWriter(this, encoding);
		}

		public int read() throws IOException {
			if (index < count) {
				return buf[index++];
			} else {
				index = 0;
				reset();

				int readCount = reader.read(characters);
				if (readCount < 0) {
					return -1;
				} else {
					writer.write(characters, 0, readCount);
					writer.flush();
					return buf[index++];
				}
			}
		}
	}
}