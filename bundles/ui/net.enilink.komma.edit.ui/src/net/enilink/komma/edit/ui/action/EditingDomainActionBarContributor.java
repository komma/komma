/**
 * <copyright> 
 *
 * Copyright (c) 2002, 2009 IBM Corporation and others.
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
 * $Id: EditingDomainActionBarContributor.java,v 1.14 2007/06/14 18:32:37 emerks Exp $
 */
package net.enilink.komma.edit.ui.action;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.views.properties.IPropertySheetPage;

import net.enilink.komma.edit.domain.IEditingDomainProvider;

/**
 * This is a contributor for an editor, multi-page or otherwise, that implements
 * {@link IEditingDomainProvider}. It automatically hooks up the Undo, Redo,
 * Cut, Copy, Paste, and Delete actions on the Edit menu to the corresponding
 * commands supported by the {@link org.eclipse.emf.edit.domain.EditingDomain}.
 * The editor site'selection provider is used to keep the Cut, Copy, Paste, and
 * Delete actions up-to-date. The actions are also refreshed every time the
 * editor fires to its {@link IPropertyListener}s.
 * <p>
 * Another very useful feature of this contributor is that it can be used as
 * follows:
 * 
 * <pre>
 * ((IMenuListener) ((IEditorSite) getSite()).getActionBarContributor())
 * 		.menuAboutToShow(menuManager);
 * </pre>
 * 
 * to contribute the Edit menu actions to a pop-up menu.
 */
public class EditingDomainActionBarContributor extends
		MultiPageEditorActionBarContributor implements IMenuListener {
	/**
	 * This is the action used to implement delete.
	 */
	protected DeleteAction deleteAction;

	/**
	 * This is the action used to implement cut.
	 */
	protected CutAction cutAction;

	/**
	 * This is the action used to implement copy.
	 */
	protected CopyAction copyAction;

	/**
	 * This is the action used to implement paste.
	 */
	protected PasteAction pasteAction;

	/**
	 * This is the action used to implement undo.
	 */
	protected UndoAction undoAction;

	/**
	 * This is the action used to implement redo.
	 */
	protected RedoAction redoAction;

	/**
	 * This is the action used to perform validation.
	 */
	protected ValidateAction validateAction;

	/**
	 * This style bit indicates that the "additions" separator should come after
	 * the "edit" separator.
	 */
	public static final int ADDITIONS_LAST_STYLE = 0x1;

	/**
	 * This is used to encode the style bits.
	 */
	protected int style;

	/**
	 * This creates an instance of the contributor.
	 */
	public EditingDomainActionBarContributor() {
		super();
	}

	/**
	 * This creates an instance of the contributor.
	 */
	public EditingDomainActionBarContributor(int style) {
		super();
		this.style = style;
	}

	@Override
	public void init(IActionBars actionBars) {
		super.init(actionBars);
		ISharedImages sharedImages = PlatformUI.getWorkbench()
				.getSharedImages();

		deleteAction = new DeleteAction(getPage(),
				removeAllReferencesOnDelete());
		deleteAction.setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
				deleteAction);

		cutAction = new CutAction(getPage());
		cutAction.setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);

		copyAction = new CopyAction(getPage());
		copyAction.setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
				copyAction);

		pasteAction = new PasteAction(getPage());
		pasteAction.setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
				pasteAction);

		undoAction = new UndoAction(getPage());
		undoAction.setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_UNDO));
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
				undoAction);

		redoAction = new RedoAction(getPage());
		redoAction.setImageDescriptor(sharedImages
				.getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
				redoAction);
	}

	/**
	 * This determines whether or not the delete action should clean up all
	 * references to the deleted objects. It is false by default.
	 */
	protected boolean removeAllReferencesOnDelete() {
		return true;
	}

	@Override
	public void contributeToMenu(IMenuManager menuManager) {
		super.contributeToMenu(menuManager);
	}

	@Override
	public void contributeToStatusLine(IStatusLineManager statusLineManager) {
		super.contributeToStatusLine(statusLineManager);
	}

	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		super.contributeToToolBar(toolBarManager);
	}

	public void shareGlobalActions(IPage page, IActionBars actionBars) {
		if (!(page instanceof IPropertySheetPage)) {
			actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(),
					deleteAction);
			actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(),
					cutAction);
			actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
					copyAction);
			actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
					pasteAction);
		}
		actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(),
				undoAction);
		actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(),
				redoAction);
	}

	@Override
	public void setActivePage(IEditorPart part) {
		// Do nothing
	}

	@Override
	public void dispose() {
		if (deleteAction != null) {
			deleteAction.dispose();
		}

		if (cutAction != null) {
			cutAction.dispose();
		}

		if (copyAction != null) {
			copyAction.dispose();
		}

		if (pasteAction != null) {
			pasteAction.dispose();
		}

		if (undoAction != null) {
			undoAction.dispose();
		}

		if (redoAction != null) {
			redoAction.dispose();
		}

		if (validateAction != null) {
			validateAction.dispose();
		}
		super.dispose();
	}

	/**
	 * This implements {@link org.eclipse.jface.action.IMenuListener} to help
	 * fill the context menus with contributions from the Edit menu.
	 */
	public void menuAboutToShow(IMenuManager menuManager) {
		// Add our standard marker.
		if ((style & ADDITIONS_LAST_STYLE) == 0) {
			menuManager.add(new Separator("additions"));
		}
		menuManager.add(new Separator("edit"));

		// Add the edit menu actions.
		menuManager.add(new ActionContributionItem(undoAction));
		menuManager.add(new ActionContributionItem(redoAction));
		menuManager.add(new Separator());
		menuManager.add(new ActionContributionItem(cutAction));
		menuManager.add(new ActionContributionItem(copyAction));
		menuManager.add(new ActionContributionItem(pasteAction));
		menuManager.add(new Separator());
		menuManager.add(new ActionContributionItem(deleteAction));
		menuManager.add(new Separator());

		if ((style & ADDITIONS_LAST_STYLE) != 0) {
			menuManager.add(new Separator("additions"));
			menuManager.add(new Separator());
		}
		// Add our other standard marker.
		menuManager.add(new Separator("additions-end"));

		addGlobalActions(menuManager);
	}

	/**
	 * This inserts global actions before the "additions-end" separator.
	 */
	protected void addGlobalActions(IMenuManager menuManager) {
		String key = (style & ADDITIONS_LAST_STYLE) == 0 ? "additions-end"
				: "additions";
		if (validateAction != null) {
			menuManager.insertBefore(key, new ActionContributionItem(
					validateAction));
		}

		if (validateAction != null) {
			menuManager.insertBefore(key, new Separator());
		}
	}
}
