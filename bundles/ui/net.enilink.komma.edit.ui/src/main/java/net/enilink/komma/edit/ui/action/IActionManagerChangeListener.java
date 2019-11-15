/******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package net.enilink.komma.edit.ui.action;

import java.util.EventListener;

/**
 * The interface for all objects that are interested in action manager change
 * events. To be such a listener, a class has to implement this interface and
 * register itself as a listener on an action manager by calling
 * <code>addActionManagerChangeListener()</code>. When no longer interested in
 * receiving event notifications, it can deregister itself as a listener by
 * calling <code>removeActionManagerChangeListener()</code> on the action
 * manager.
 * 
 * @author khussey
 * 
 * @see org.eclipse.gmf.runtime.common.ui.action.ActionManager
 * @see org.eclipse.gmf.runtime.common.ui.action.ActionManagerChangeEvent
 */
public interface IActionManagerChangeListener extends EventListener {

    /**
     * Handles an event indicating that an action manager has changed.
     * 
     * @param event The action manager change event to be handled.
     */
    public void actionManagerChanged(ActionManagerChangeEvent event);

}
