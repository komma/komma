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
package net.enilink.komma.model;

public interface IModelConstants {
	/**
	 * Status indicating that a document could not be created because the
	 * underlying resource is invalid.
	 */
	public static final int INVALID_RESOURCE = 995;

	/**
	 * Status indicating that a document could not be created because the
	 * underlying resource is not of an appropriate type.
	 */
	public static final int INVALID_RESOURCE_TYPE = 996;

	/**
	 * Status constant indicating that a <code>InterruptedException</code>
	 * occurred.
	 */
	public static final int INTERRRUPTED_EXCEPTION = 997;

	/**
	 * Status constant indicating that a <code>TargetException</code>
	 * occurred.
	 */
	public static final int TARGET_EXCEPTION = 998;
}
