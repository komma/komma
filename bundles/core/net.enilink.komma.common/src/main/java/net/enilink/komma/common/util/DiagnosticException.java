/**
 * <copyright>
 *
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
 * $Id: DiagnosticException.java,v 1.3 2006/12/05 20:19:56 emerks Exp $
 */
package net.enilink.komma.common.util;

import org.eclipse.core.runtime.CoreException;

/**
 * A checked exception representing a diagnosed failure.
 * <p>
 * Diagnostic exceptions contain a diagnostic describing the cause of the
 * exception.
 * </p>
 * 
 * @see Diagnostic
 */
public class DiagnosticException extends Exception {
	private static final long serialVersionUID = 1L;

	private Diagnostic diagnostic;

	public DiagnosticException(Diagnostic diagnostic) {
		super(diagnostic.getMessage(), diagnostic.getException());
		this.diagnostic = diagnostic;
	}

	public final Diagnostic getDiagnostic() {
		return diagnostic;
	}

	public static CoreException toCoreException(DiagnosticException exception) {
		return new CoreException(BasicDiagnostic.toIStatus(exception));
	}
}
