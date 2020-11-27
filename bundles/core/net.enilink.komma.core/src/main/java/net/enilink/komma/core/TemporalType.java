/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
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
 * Type used to indicate a specific mapping of {@link java.util.Date} or
 * {@link java.util.Calendar}.
 *
 */
public enum TemporalType {

	/** Map as java.sql.Date */
	DATE,

	/** Map as java.sql.Time */
	TIME,

	/** Map as java.sql.Timestamp */
	TIMESTAMP
}
