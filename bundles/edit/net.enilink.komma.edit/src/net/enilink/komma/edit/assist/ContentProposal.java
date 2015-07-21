/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package net.enilink.komma.edit.assist;

import org.eclipse.core.runtime.Assert;

/**
 * A default implementation of {@link IContentProposal} that allows clients to
 * specify a content proposal using simple constructors.
 */
public class ContentProposal implements IContentProposal {
	private static final String EMPTY = ""; //$NON-NLS-1$

	private String content = EMPTY;
	private String label = EMPTY;
	private String description = EMPTY;
	private int cursorPosition = 0;
	private boolean insert = true;

	/**
	 * Create a content proposal whose label and content are the specified
	 * String. The cursor position will be located at the end of the content.
	 * 
	 * @param content
	 *            the String representing the content. Should not be
	 *            <code>null</code>.
	 */
	public ContentProposal(String content) {
		this(content, content, null);
	}

	/**
	 * Create a content proposal whose content and description are as specified
	 * in the parameters. The cursor position will be located at the end of the
	 * content.
	 * 
	 * @param content
	 *            the String representing the content. Should not be
	 *            <code>null</code>. This string will also be used as the label.
	 * @param description
	 *            the String representing the description, or <code>null</code>
	 *            if there should be no description.
	 */
	public ContentProposal(String content, String description) {
		this(content, content, description);
	}

	/**
	 * Create a content proposal whose content, label, and description are as
	 * specified in the parameters. The cursor position will be located at the
	 * end of the content.
	 * 
	 * @param content
	 *            the String representing the content. Should not be
	 *            <code>null</code>.
	 * @param label
	 *            the String representing the label. Should not be
	 *            <code>null</code>.
	 * 
	 * @param description
	 *            the String representing the description, or <code>null</code>
	 *            if there should be no description.
	 */
	public ContentProposal(String content, String label, String description) {
		this(content, label, description, content.length(), true);
	}

	/**
	 * Create a content proposal whose content, label, description, and cursor
	 * position are as specified in the parameters.
	 * 
	 * @param content
	 *            the String representing the content. Should not be
	 *            <code>null</code>.
	 * @param label
	 *            the String representing the label. Should not be
	 *            <code>null</code>.
	 * 
	 * @param description
	 *            the String representing the description, or <code>null</code>
	 *            if there should be no description.
	 * 
	 * @param cursorPosition
	 *            the zero-based index position within the contents where the
	 *            cursor should be placed after the proposal is accepted. The
	 *            range of the cursor position is from 0..N where N is the
	 *            number of characters in the content.
	 * 
	 * @param insert
	 *            controls whether this proposal inserts content into the
	 *            original text or replaces it completely.
	 * 
	 * @exception IllegalArgumentException
	 *                if the index is not between 0 and the number of characters
	 *                in the content.
	 */
	public ContentProposal(String content, String label, String description,
			int cursorPosition, boolean insert) {
		Assert.isNotNull(content);
		Assert.isNotNull(label);
		Assert.isLegal(cursorPosition >= 0
				&& cursorPosition <= content.length());
		this.content = content;
		this.label = label;
		this.description = description;
		this.cursorPosition = cursorPosition;
		this.insert = insert;
	}

	public String getContent() {
		return content;
	}

	public int getCursorPosition() {
		return cursorPosition;
	}

	public String getDescription() {
		return description;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public boolean isInsert() {
		return insert;
	}
}
