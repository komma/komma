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
 * $Id: DiagnosticChain.java,v 1.2 2005/06/08 06:19:08 nickb Exp $
 */
package net.enilink.komma.common.util;


/**
 * An accumulator of diagnostics.
 */
public interface DiagnosticChain {
	/**
	 * Adds the diagnostic to the chain.
	 */
	void add(Diagnostic diagnostic);

	/**
	 * Adds the {@link Diagnostic#getChildren children} of the diagnostic to the
	 * chain.
	 */
	void addAll(Diagnostic diagnostic);

	/**
	 * If the diagnostic has {@link Diagnostic#getChildren children},
	 * {@link #addAll add}s those children, otherwise, {@link #add add}s the
	 * diagnostic.
	 */
	void merge(Diagnostic diagnostic);
}
