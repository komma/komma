/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

/**
 * An unexpected general exception.
 * 
 * @author Ken Wenzel
 * 
 */
public class KommaException extends RuntimeException {
	private static final long serialVersionUID = 7957598184170534293L;

	public KommaException() {
		super();
	}

	public KommaException(String message, Throwable cause) {
		super(message, cause);
	}

	public KommaException(String message) {
		super(message);
	}

	public KommaException(Throwable cause) {
		super(cause);
	}

}