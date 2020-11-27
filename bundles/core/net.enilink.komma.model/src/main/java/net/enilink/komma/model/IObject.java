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
package net.enilink.komma.model;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.em.concepts.IResource;

/**
 * The base interface of a bean that is contained within an {@link IModel}.
 * 
 * <p>
 * An <code>IObject</code> is an entity that additionally implements the {@link IModelAware}
 * interface to access the associated model via {@link #getModel()}.
 * 
 * <p> An <code>IObject</code> instance also implements the {@link IResource} interface
 * that allows to access RDF types and properties of the resource by using
 * general object-oriented methods (comparable to Java reflection). 
 *
 */
public interface IObject extends IEntity, IResource, IModelAware {
}