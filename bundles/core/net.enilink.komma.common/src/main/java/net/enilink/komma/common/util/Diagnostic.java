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
 * $Id: Diagnostic.java,v 1.5 2006/12/05 20:19:56 emerks Exp $
 */
package net.enilink.komma.common.util;

import java.util.List;

/**
 * Information about the outcome of some activity.
 */
public interface Diagnostic {
	/**
	 * The bit mask value <code>0x0</code> for a {@link #getSeverity severity}
	 * indicating everything is okay.
	 */
	int OK = 0x0;

	/**
	 * The bit mask value <code>0x1</code> for a {@link #getSeverity severity}
	 * indicating there is an informational message.
	 */
	int INFO = 0x1;

	/**
	 * The bit mask value <code>0x2</code> for a {@link #getSeverity severity}
	 * indicating there is warning message.
	 */
	int WARNING = 0x2;

	/**
	 * The bit mask value <code>0x1</code> for a {@link #getSeverity severity}
	 * indicating there is an error message.
	 */
	int ERROR = 0x4;

	/**
	 * The bit mask value <code>0x1</code> for a {@link #getSeverity severity}
	 * indicating that the diagnosis was canceled.
	 */
	int CANCEL = 0x8;

	/**
	 * Returns an indicator of the severity of the problem.
	 */
	int getSeverity();

	/**
	 * Returns a message describing the situation.
	 */
	String getMessage();

	/**
	 * Returns the unique identifier of the source.
	 */
	String getSource();

	/**
	 * Returns {@link #getSource source-specific} identity code.
	 */
	int getCode();

	/**
	 * Returns the relevant low-level exception, or <code>null</code> if none.
	 */
	Throwable getException();

	/**
	 * Returns the arbitrary associated list of data. The first element is
	 * typically the object that is the primary source of the problem; the
	 * second element is typically some object describing the problematic
	 * feature or aspect of the primary source, and the remaining elements are
	 * additional objects associated with or describing the problem.
	 */
	List<?> getData();

	/**
	 * Returns the list of child {@link Diagnostic diagnostics}.
	 */
	List<Diagnostic> getChildren();

	/**
	 * A diagnostic indicating that everything is okay.
	 */
	Diagnostic OK_INSTANCE = new BasicDiagnostic(OK,
			"net.enilink.komma.common", 0,
			net.enilink.komma.common.CommonPlugin.INSTANCE
					.getString("_UI_OK_diagnostic_0"), null);

	/**
	 * A diagnostic indicating that the diagnosis was canceled.
	 */
	Diagnostic CANCEL_INSTANCE = new BasicDiagnostic(CANCEL,
			"net.enilink.komma.common", 0,
			net.enilink.komma.common.CommonPlugin.INSTANCE
					.getString("_UI_Cancel_diagnostic_0"), null);
}
