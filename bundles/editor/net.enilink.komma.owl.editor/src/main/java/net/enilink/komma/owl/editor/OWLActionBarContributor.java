/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.enilink.komma.owl.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import net.enilink.komma.common.ui.viewer.IViewerProvider;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.action.CreateChildrenActionContributor;
import net.enilink.komma.edit.ui.action.DeleteAction;
import net.enilink.komma.edit.ui.action.EditingDomainActionBarContributor;
import net.enilink.komma.edit.ui.action.ValidateAction;

/**
 * This is the action bar contributor for the OWL editor.
 */
public class OWLActionBarContributor extends EditingDomainActionBarContributor implements ISelectionChangedListener {
	/**
	 * This keeps track of the active editor.
	 */
	protected IEditorPart activeEditorPart;

	protected CreateChildrenActionContributor createChildActionContributor = new CreateChildrenActionContributor();

	/**
	 * This is the action used to implement removal from parent.
	 */
	protected DeleteAction removeFromParentAction;

	/**
	 * This action refreshes the viewer of the current editor if the editor
	 * implements {@link IViewerProvider}.
	 */
	protected IAction refreshViewerAction = new Action(
			OWLEditorPlugin.INSTANCE.getString("_UI_RefreshViewer_menu_item")) {
		@Override
		public boolean isEnabled() {
			return activeEditorPart instanceof IViewerProvider;
		}

		@Override
		public void run() {
			if (activeEditorPart instanceof IViewerProvider) {
				Viewer viewer = ((IViewerProvider) activeEditorPart).getViewer();
				if (viewer != null) {
					viewer.refresh();
				}
			}
		}
	};

	/**
	 * This keeps track of the current selection provider.
	 */
	protected ISelectionProvider selectionProvider;

	/**
	 * This action opens the Properties view.
	 */
	protected IAction showPropertiesViewAction = new Action(
			OWLEditorPlugin.INSTANCE.getString("_UI_ShowPropertiesView_menu_item")) {
		@Override
		public void run() {
			try {
				getPage().showView("org.eclipse.ui.views.PropertySheet");
			} catch (PartInitException exception) {
				OWLEditorPlugin.INSTANCE.log(exception);
			}
		}
	};

	/**
	 * This creates an instance of the contributor.
	 */
	public OWLActionBarContributor() {
		super(ADDITIONS_LAST_STYLE);
	}

	/**
	 * This inserts global actions before the "additions-end" separator.
	 */
	@Override
	protected void addGlobalActions(IMenuManager menuManager) {
		menuManager.insertAfter("additions-end", new Separator("ui-actions"));
		menuManager.insertAfter("ui-actions", showPropertiesViewAction);

		refreshViewerAction.setEnabled(refreshViewerAction.isEnabled());
		menuManager.insertAfter("ui-actions", refreshViewerAction);

		super.addGlobalActions(menuManager);
	}

	/**
	 * This adds to the menu bar a menu and some separators for editor
	 * additions, as well as the sub-menus for object creation items.
	 */
	@Override
	public void contributeToMenu(IMenuManager menuManager) {
		super.contributeToMenu(menuManager);

		IMenuManager submenuManager = new MenuManager(OWLEditorPlugin.INSTANCE.getString("_UI_OWLEditor_menu"),
				"owlMenuID");
		menuManager.insertAfter("additions", submenuManager);
		submenuManager.add(new Separator("settings"));
		submenuManager.add(new Separator("actions"));
		submenuManager.add(new Separator("additions"));
		submenuManager.add(new Separator("additions-end"));

		createChildActionContributor.contributeToMenu(submenuManager, "additions");

		// Force an update because Eclipse hides empty menus now.
		submenuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager menuManager) {
				menuManager.updateAll(true);
			}
		});

		addGlobalActions(submenuManager);
	}

	/**
	 * This adds Separators for editor additions to the tool bar.
	 */
	@Override
	public void contributeToToolBar(IToolBarManager toolBarManager) {
		toolBarManager.add(new Separator("owl-settings"));
		toolBarManager.add(new Separator("owl-additions"));
	}

	@Override
	public void init(IActionBars bars, IWorkbenchPage page) {
		super.init(bars, page);
		validateAction = new ValidateAction(page);
		removeFromParentAction = new DeleteAction(page, false);
	}

	/**
	 * This populates the pop-up menu before it appears.
	 */
	@Override
	public void menuAboutToShow(IMenuManager menuManager) {
		super.menuAboutToShow(menuManager);

		createChildActionContributor.menuAboutToShow(menuManager, "edit");
		menuManager.appendToGroup("edit", new ActionContributionItem(removeFromParentAction));
	}

	/**
	 * This ensures that a delete action will clean up all references to deleted
	 * objects. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected boolean removeAllReferencesOnDelete() {
		return true;
	}

	/**
	 * This implements
	 * {@link org.eclipse.jface.viewers.ISelectionChangedListener}, handling
	 * {@link org.eclipse.jface.viewers.SelectionChangedEvent}s by querying for
	 * the children and siblings that can be added to the selected object and
	 * updating the menus accordingly.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		IEditingDomain domain = ((IEditingDomainProvider) activeEditorPart).getEditingDomain();

		createChildActionContributor.selectionChanged(activeEditorPart, domain, event.getSelection());
	}

	/**
	 * When the active editor changes, this remembers the change and registers
	 * with it as a selection provider. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		activeEditorPart = part;

		// Switch to the new selection provider.
		//
		if (selectionProvider != null) {
			selectionProvider.removeSelectionChangedListener(this);
		}
		if (part == null) {
			selectionProvider = null;
		} else {
			selectionProvider = part.getSite().getSelectionProvider();
			selectionProvider.addSelectionChangedListener(this);

			// Fake a selection changed event to update the menus.
			//
			if (selectionProvider.getSelection() != null) {
				selectionChanged(new SelectionChangedEvent(selectionProvider, selectionProvider.getSelection()));
			}
		}
	}

	@Override
	public void dispose() {
		if (selectionProvider != null) {
			selectionProvider.removeSelectionChangedListener(this);
			selectionProvider = null;
		}
		if (createChildActionContributor != null) {
			createChildActionContributor.dispose();
			createChildActionContributor = null;
		}
		super.dispose();
	}
}