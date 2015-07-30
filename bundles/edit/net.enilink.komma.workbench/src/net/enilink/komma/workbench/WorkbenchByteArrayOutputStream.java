/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: WorkbenchByteArrayOutputStream.java,v $$
 *  $$Revision: 1.2 $$  $$Date: 2005/02/15 23:04:14 $$ 
 */
package net.enilink.komma.workbench;

import java.io.IOException;

import org.eclipse.core.resources.IFile;

import net.enilink.komma.model.eclipse.PlatformResourceURIHandler;

/**
 * ByteArray OutputStream for the Workbench. It works with the synchronizer
 * {@link org.eclipse.jem.util.emf.workbench.ResourceSetWorkbenchSynchronizer}to
 * notify of a save. It also changes all of the line separators to the current
 * system line separator, i.e. if there are some '\r' and we are on Windows(R)
 * then they will be converted to '\n\r'), if the setting for the stream is to
 * do eol conversion.
 * 
 * @since 1.0.0
 */
public class WorkbenchByteArrayOutputStream extends
		PlatformResourceURIHandler.PlatformResourceOutputStream {
	private boolean fConvertEOL = false;

	protected ModelSetWorkbenchSynchronizer synchronizer;

	/**
	 * Construct with an IFile
	 * 
	 * @param aFile
	 * 
	 * @since 1.0.0
	 */
	public WorkbenchByteArrayOutputStream(IFile aFile) {
		this(aFile, null);
	}

	/**
	 * Construct with a IFile and a synchronizer. This way synchronizer will
	 * know when file is about to be saved.
	 * 
	 * @param aFile
	 * @param aSynchronizer
	 * 
	 * @since 1.0.0
	 */
	public WorkbenchByteArrayOutputStream(IFile aFile,
			ModelSetWorkbenchSynchronizer aSynchronizer) {
		super(aFile, false, true, null);
		synchronizer = aSynchronizer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		if (synchronizer != null)
			synchronizer.preSave(file);
		super.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.emf.ecore.resource.impl.URIConverterImpl.
	 * PlatformResourceOutputStream#flush()
	 */
	public void flush() throws IOException {
		if (synchronizer != null)
			synchronizer.preSave(file);
		super.flush();
	}

	/*
	 * Convert the end of line characters.
	 */
	private int convertEOL(byte[] data) {
		// Algorithm:
		// Remove all '\r' chars
		// Replace all '\n' chars with line seperator chars

		String EOL = System.getProperties().getProperty("line.separator"); //$NON-NLS-1$
		byte[] EOLBytes = EOL.getBytes();

		int out = 0;

		for (int in = 0; in < data.length; in++) {
			if (data[in] == '\r') {
				// don't output (ie, throw the char away)
			} else if (data[in] == '\n') {
				// The code does not currently handle expanding the array
				if ((in - out + 1) < EOLBytes.length)
					throw new UnsupportedOperationException(
							"WorkbenchByteArrayOutputStream: Expanding EOL chars not implemented"); //$NON-NLS-1$

				for (int i = 0; i < EOLBytes.length; i++) {
					data[out++] = EOLBytes[i];
				}
			} else {
				// Just copy the data
				data[out++] = data[in];
			}
		}

		return out;
	}

	/**
	 * Is EOL conversion turned on.
	 * 
	 * @return <code>true</code> if EOL conversion is turned on.
	 * 
	 * @since 1.0.0
	 */
	public boolean isConvertEOLChars() {
		return fConvertEOL;
	}

	/**
	 * Set the EOL conversion flag.
	 * 
	 * @param set
	 *            <code>true</code> if EOL should be converted to current line
	 *            separator.
	 * 
	 * @since 1.0.0
	 */
	public void setConvertEOLChars(boolean set) {
		fConvertEOL = set;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.ByteArrayOutputStream#toByteArray()
	 */
	public synchronized byte[] toByteArray() {
		byte[] contents = super.toByteArray();
		if (isConvertEOLChars())
			convertEOL(contents);
		return contents;
	}
}