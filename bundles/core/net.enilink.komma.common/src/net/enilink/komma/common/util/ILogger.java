/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: Logger.java,v 1.2 2005/06/08 06:19:08 nickb Exp $
 */
package net.enilink.komma.common.util;

/**
 * A logger of log entries. It can be implemented by different logging
 * facilities depending on the runtime. It is plastic and intended to support
 * any underlying logging facility.
 */
public interface ILogger {
	/**
	 * Logs an entry.
	 * 
	 * @param logEntry
	 *            a plastic entry to log.
	 */
	void log(Object logEntry);
}
