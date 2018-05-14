/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
 * $Id: IViewerNotification.java,v 1.2 2005/06/08 06:17:05 nickb Exp $
 */
package net.enilink.komma.edit.provider;

import net.enilink.komma.common.notify.INotification;

/**
 * A description of viewer changes required by an EMF notification. The EMF
 * change is described through the base <code>Notification</code> interface.
 */
public interface IViewerNotification extends INotification {
	/**
	 * The element to update or from which to refresh. The whole viewer is
	 * indicated by the null value.
	 */
	Object getElement();

	/**
	 * Whether the content under the element should be structurally refreshed.
	 */
	boolean isContentRefresh();

	/**
	 * Whether the label and icon for the element should be updated.
	 */
	boolean isLabelUpdate();
}
