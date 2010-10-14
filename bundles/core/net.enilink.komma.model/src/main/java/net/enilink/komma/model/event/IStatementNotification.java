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
package net.enilink.komma.model.event;

import org.openrdf.model.Resource;

import net.enilink.komma.common.notify.INotification;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.core.IReference;

public interface IStatementNotification extends INotification {
	IModelSet getModelSet();
	
	IReference getSubject();

	IReference getPredicate();

	Object getObject();

	IReference[] getContexts();

	boolean isAdd();
}
