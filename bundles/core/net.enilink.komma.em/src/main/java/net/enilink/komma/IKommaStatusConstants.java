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
package net.enilink.komma;

/**
 * Defines status codes relevant to the document UI plug-in. When a 
 * Core exception is thrown, it contain a status object describing
 * the cause of the exception. The status objects originating from the
 * document UI plug-in use the codes defined in this interface.
  */
public interface IKommaStatusConstants {	
	public static final int INTERNAL_ERROR = 10001;
 }

