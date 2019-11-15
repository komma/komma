/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.ui.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.Form;

public interface IMessageContainer {
	/**
	 * Sets the form message.
	 * 
	 * @param newMessage
	 *            the message text or <code>null</code> to reset.
	 * @param newType
	 *            as defined in
	 *            {@link org.eclipse.jface.dialogs.IMessageProvider}.
	 * @param messages
	 * 			 an optional array of children that itemize individual
	 * 			messages or <code>null</code> for a simple message.
	 * @see Form#setMessage(String, int)
	 */
	void setMessage(String newMessage, int newType, IMessage[] messages);

	void setMessage(String newMessage, int newType);
	
	Composite getComposite();
}
