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
package net.enilink.komma.model;

/**
 * Interface of objects that are associated with a {@link IModel model.} 
 */
public interface IModelAware {
	/**
	 * Returns the associated {@link IModel} which contains the data of this
	 * object.
	 * 
	 * @return model The associated model.
	 */
	IModel getModel();
}
