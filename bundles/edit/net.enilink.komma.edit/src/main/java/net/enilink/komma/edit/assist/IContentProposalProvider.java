/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.assist;

/**
 * IContentProposalProvider provides an array of IContentProposals that are
 * appropriate for a textual dialog field, given the field's current content and
 * the current cursor position.
 */
public interface IContentProposalProvider {

	/**
	 * Return an array of content proposals representing the valid proposals for
	 * a field.
	 * 
	 * @param contents
	 *            the current contents of the text field
	 * @param position
	 *            the current position of the cursor in the contents
	 * 
	 * @return the array of {@link IContentProposal} that represent valid
	 *         proposals for the field.
	 */
	IContentProposal[] getProposals(String contents, int position);
}
