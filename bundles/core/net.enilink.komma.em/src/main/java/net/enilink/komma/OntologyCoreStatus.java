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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class OntologyCoreStatus extends Status {

	private OntologyCoreStatus(int severity, int code, String message,
			Throwable throwable) {
		super(severity, KommaCore.PLUGIN_ID, code, message, throwable);
	}

	public static IStatus createError(int code, Throwable throwable) {
		String message = throwable.getMessage();
		if (message == null) {
			message = throwable.getClass().getName();
		}
		return new OntologyCoreStatus(IStatus.ERROR, code, message, throwable);
	}

	public static IStatus createError(int code, String message,
			Throwable throwable) {
		return new OntologyCoreStatus(IStatus.ERROR, code, message, throwable);
	}

	public static IStatus createWarning(int code, String message,
			Throwable throwable) {
		return new OntologyCoreStatus(IStatus.WARNING, code, message, throwable);
	}

	public static IStatus createInfo(int code, String message,
			Throwable throwable) {
		return new OntologyCoreStatus(IStatus.INFO, code, message, throwable);
	}
}
