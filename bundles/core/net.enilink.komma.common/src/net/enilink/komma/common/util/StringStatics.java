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

import java.io.File;

import org.eclipse.core.runtime.IPath;

/**
 * This class contains string statics that are not to be translated.
 * 
 * @author wdiu, Wayne Diu
 * @canBeSeenBy %partners
 */
public class StringStatics {

	/** Prevent instantiation with a private constructor */
	private StringStatics() {
		/* private constructor */
	}

	/** The empty string */
	public static final String BLANK = ""; //$NON-NLS-1$

	/** The period . */
	public static final String PERIOD = "."; //$NON-NLS-1$

	/** The forward slash / */
	public static final String FORWARD_SLASH = "/"; //URLs //$NON-NLS-1$

	/** The backward slash / */
	public static final String BACKWARD_SLASH = "\\"; //$NON-NLS-1$

	/** The ellipsis ... */
	public static final String ELLIPSIS = "..."; //$NON-NLS-1$

	/** The space */
	public static final String SPACE = " "; //$NON-NLS-1$

	/** The colon : */
	public static final String COLON = ":"; //$NON-NLS-1$

	/** The double colon :: */
	public static final String DOUBLE_COLON = "::"; //$NON-NLS-1$

	/** The newline for a particular platform */
	public static final String PLATFORM_NEWLINE = System
		.getProperty("line.separator"); //$NON-NLS-1$

	/** The newline for the Windows platform */
	public static final String WINDOWS_NEWLINE = "\r\n"; //$NON-NLS-1$

	/** The newline for the Unix platform */
	public static final String UNIX_NEWLINE = "\n"; //$NON-NLS-1$

	/** The separator defined by File.separator */
	public static final String FILE_SEPARATOR = File.separator;

	/** The separator defined by IPath.SEPARATOR */
	public static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);

	/** The apostrophe ' */
	public static final String APOSTROPHE = "'"; //$NON-NLS-1$

	/** The apostrophe ' */
	public static final String AMPERSAND = "&"; //$NON-NLS-1$

	/** The greater than symbol > */
	public static final String GREATER_THAN = ">"; //$NON-NLS-1$

	/** The equals = */
	public static final String EQUALS = "="; //$NON-NLS-1$

	/** The version separator in a plugin name for release _ */
	public static final String PLUGIN_VERSION_SEPARATOR = "_"; //$NON-NLS-1$

	/** The Underscore * */
	public static final String UNDER_SCORE = "_"; //$NON-NLS-1$

	/** The hyphen * */
	public static final String HYPHEN = "-"; //$NON-NLS-1$

	/** The comma * */
	public static final String COMMA = ",";//$NON-NLS-1$
	
	/** Open Parenthesis */
	public static final String OPEN_PARENTHESIS = "("; //$NON-NLS-1$
	
	/** Close Parenthesis */
	public static final String CLOSE_PARENTHESIS = ")"; //$NON-NLS-1$
}