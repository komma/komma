/**
 * <copyright>
 *
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
 * $Id$
 */
package net.enilink.komma.common.ui.viewer;

import org.eclipse.jface.viewers.Viewer;

/**
 * Interface common to all objects that provide a viewer.
 */
public interface IViewerProvider {
	Viewer getViewer();
}
