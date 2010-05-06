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
package net.enilink.komma.concepts;

import org.openrdf.model.Resource;

import net.enilink.komma.core.IReference;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.ISesameResourceAware;
import net.enilink.komma.util.ISparqlConstants;

public abstract class BehaviorBase implements ISparqlConstants, ISesameEntity {
	protected Resource getSesameResource(IReference entity) {
		return ((ISesameResourceAware) entity).getSesameResource();
	}

	public ISesameManager getSesameManager() {
		return (ISesameManager) getKommaManager();
	}
}