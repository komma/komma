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
package net.enilink.komma.internal.sesame;


import org.openrdf.model.Resource;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.SesameReference;

/**
 * Stores the resource and manager for a bean and implements equals, hashCode,
 * and toString.
 * 
 * @author James Leigh
 * 
 */
public abstract class SesameEntitySupport implements
		IEntity, ISesameEntity, ISesameManagerAware {
	private ISesameManager manager;
	private SesameReference reference;

	public IKommaManager getKommaManager() {
		return manager;
	}

	public URI getURI() {
		return reference.getURI();
	}

	@Override
	public ISesameManager getSesameManager() {
		return manager;
	}

	@Override
	public Resource getSesameResource() {
		return reference.getSesameResource();
	}

	@Override
	public void initSesameManager(ISesameManager manager) {
		this.manager = manager;
	}

	@Override
	public void initSesameReference(SesameReference reference) {
		this.reference = reference;
	}

	public IReference getReference() {
		return reference;
	}

	public void refresh() {
		// do nothing
	}

	@Override
	public String toString() {
		return reference.toString();
	}
	
	@Override
	public int hashCode() {
		return reference.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return reference.equals(obj);
	}
}
