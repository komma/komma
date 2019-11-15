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
package net.enilink.komma.edit.ui.dialogs;

import org.eclipse.osgi.util.NLS;

/**
 * Message class for dialog messages.
 */
public class DialogMessages extends NLS {
	private static final String BUNDLE_NAME = DialogMessages.class.getPackage()
			.getName()
			+ ".messages";//$NON-NLS-1$

	// FilteredItemsSelectionDialog
	public static String FilteredItemsSelectionDialog_cacheSearchJob_taskName;
	public static String FilteredItemsSelectionDialog_menu;
	public static String FilteredItemsSelectionDialog_refreshJob;
	public static String FilteredItemsSelectionDialog_progressRefreshJob;
	public static String FilteredItemsSelectionDialog_cacheRefreshJob;
	public static String FilteredItemsSelectionDialog_cacheRefreshJob_checkDuplicates;
	public static String FilteredItemsSelectionDialog_cacheRefreshJob_getFilteredElements;
	public static String FilteredItemsSelectionDialog_patternLabel;
	public static String FilteredItemsSelectionDialog_listLabel;
	public static String FilteredItemsSelectionDialog_toggleStatusAction;
	public static String FilteredItemsSelectionDialog_removeItemsFromHistoryAction;
	public static String FilteredItemsSelectionDialog_searchJob_taskName;
	public static String FilteredItemsSelectionDialog_separatorLabel;
	public static String FilteredItemsSelectionDialog_storeError;
	public static String FilteredItemsSelectionDialog_restoreError;
	public static String FilteredItemsSelectionDialog_nItemsSelected;

	// AbstractSearcher
	public static String FilteredItemsSelectionDialog_jobLabel;
	public static String FilteredItemsSelectionDialog_jobError;
	public static String FilteredItemsSelectionDialog_jobCancel;

	// GranualProgressMonitor
	public static String FilteredItemsSelectionDialog_taskProgressMessage;
	public static String FilteredItemsSelectionDialog_subtaskProgressMessage;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, DialogMessages.class);
	}
}
