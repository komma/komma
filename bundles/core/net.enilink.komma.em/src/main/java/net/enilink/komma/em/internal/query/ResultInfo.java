/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal.query;

import java.util.List;

public class ResultInfo {
	public boolean typeRestricted = false;
	public List<Class<?>> types;

	public ResultInfo(boolean typeRestricted, List<Class<?>> types) {
		this.typeRestricted = typeRestricted;
		this.types = types;
	}
}