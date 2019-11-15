/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: DragAndDropFeedback.java,v 1.3 2006/12/28 06:48:55 marcelop Exp $
 */
package net.enilink.komma.edit.command;

import java.util.Collection;

/**
 * This interface is implemented by any
 * {@link net.enilink.komma.common.command.ICommand} that supports detailed drag
 * and drop feedback. This interface provides synonyms for all the important
 * org.eclipse.swt.dnd.DND constants, so that commands can be written without
 * reference to SWT.
 */
public interface IDragAndDropFeedback {
	/**
	 * This is the same as org.eclipse.swt.dnd.DND.DROP_NONE.
	 */
	public final static int DROP_NONE = 0;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.DROP_COPY.
	 */
	public final static int DROP_COPY = 1;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.DROP_MOVE.
	 */
	public final static int DROP_MOVE = 2;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.DROP_LINK.
	 */
	public final static int DROP_LINK = 4;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.FEEDBACK_NONE.
	 */
	public final static int FEEDBACK_NONE = 0;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.FEEDBACK_SELECT.
	 */
	public final static int FEEDBACK_SELECT = 1;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.FEEDBACK_INSERT_BEFORE.
	 */
	public final static int FEEDBACK_INSERT_BEFORE = 2;

	/**
	 * This is the same as org.eclipse.swt.dnd.DND.FEEDBACK_INSERT_AFTER.
	 */
	public final static int FEEDBACK_INSERT_AFTER = 4;

	/**
	 * This is called repeatedly as the drag and drop information changes. The
	 * collection, which represents the dragged source, does not normally
	 * change.
	 */
	public boolean validate(Object owner, float location, int operations,
			int operation, Collection<?> collection);

	/**
	 * This returns one of the FEEDBACK_* values.
	 */
	public int getFeedback();

	/**
	 * This returns one of the DROP_* values.
	 */
	public int getOperation();
}
