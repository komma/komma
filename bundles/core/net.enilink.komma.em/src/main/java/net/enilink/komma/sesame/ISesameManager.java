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
package net.enilink.komma.sesame;

import java.util.Collection;

import net.enilink.composition.mappers.RoleMapper;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.contextaware.ContextAwareConnection;

import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.IReference;

public interface ISesameManager extends IKommaManager {
	ISesameEntity createBean(Resource resource, Collection<URI> types,
			Model model);

	<T> T create(Resource resource, Class<T> concept, Class<?>... concepts);

	ISesameEntity find(Resource resource);

	ISesameEntity find(Resource resource, Class<?>... concepts);

	IReference findRestricted(Resource resource, Class<?>... concepts);

	ContextAwareConnection getConnection();

	Object getInstance(Value value, Class<?> type);

	RoleMapper<URI> getRoleMapper();

	Value getValue(Object instance);

	<T> T rename(T bean, Resource dest);

	void setConnection(ContextAwareConnection connection);
}