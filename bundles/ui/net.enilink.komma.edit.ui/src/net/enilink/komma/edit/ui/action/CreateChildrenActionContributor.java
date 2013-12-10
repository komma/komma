package net.enilink.komma.edit.ui.action;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.commons.ui.CommonsUi;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

public class CreateChildrenActionContributor {
	/**
	 * This is the menu manager into which menu contribution items should be
	 * added for CreateChild actions.
	 */
	protected IMenuManager createChildMenuManager;

	/**
	 * This is the menu manager into which menu contribution items should be
	 * added for CreateSibling actions.
	 */
	protected IMenuManager createSiblingMenuManager;

	protected MenuActionCollector<Object> newChildCollector;

	protected MenuActionCollector<Object> newSiblingCollector;

	/**
	 * This adds the sub-menus for object creation items.
	 */
	public void contributeToMenu(IMenuManager menuManager, String contributionId) {
		// Prepare for CreateChild item addition or removal.
		createChildMenuManager = new MenuManager(
				KommaEditUIPlugin.INSTANCE
						.getString("_UI_CreateChild_menu_item"));
		menuManager.insertBefore(contributionId, createChildMenuManager);

		// Prepare for CreateSibling item addition or removal.
		createSiblingMenuManager = new MenuManager(
				KommaEditUIPlugin.INSTANCE
						.getString("_UI_CreateSibling_menu_item"));
		menuManager.insertBefore(contributionId, createSiblingMenuManager);
	}

	public void dispose() {
		disposeCollectors();
	}

	protected void disposeCollectors() {
		if (newChildCollector != null) {
			newChildCollector.dispose();
			newChildCollector = null;
		}
		if (newSiblingCollector != null) {
			newSiblingCollector.dispose();
			newSiblingCollector = null;
		}
	}

	/**
	 * This generates a {@link CreateChildAction} for each object in
	 * <code>descriptors</code>, and returns the collection of these actions.
	 */
	protected Collection<IAction> generateCreateChildActions(
			IWorkbenchPart part, IEditingDomain domain,
			Collection<?> descriptors, ISelection selection) {
		Collection<IAction> actions = new ArrayList<IAction>();
		if (descriptors != null) {
			for (Object descriptor : descriptors) {
				CreateChildAction action = new CreateChildAction(part, domain,
						selection, descriptor);
				action.init();
				actions.add(action);
			}
		}
		return actions;
	}

	/**
	 * This generates a {@link CreateSiblingAction} for each object in
	 * <code>descriptors</code>, and returns the collection of these actions.
	 */
	protected Collection<IAction> generateCreateSiblingActions(
			IWorkbenchPart part, IEditingDomain domain,
			Collection<?> descriptors, ISelection selection) {
		Collection<IAction> actions = new ArrayList<IAction>();
		if (descriptors != null) {
			for (Object descriptor : descriptors) {
				CreateSiblingAction action = new CreateSiblingAction(part,
						domain, selection, descriptor);
				action.init();
				actions.add(action);
			}
		}
		return actions;
	}

	/**
	 * This populates the pop-up menu before it appears.
	 */
	public void menuAboutToShow(IMenuManager menuManager, String contributionId) {
		if (newChildCollector != null) {
			MenuManager submenuManager = new MenuManager(
					KommaEditUIPlugin.INSTANCE
							.getString("_UI_CreateChild_menu_item"));
			newChildCollector.addMenuManager(submenuManager);
			menuManager.insertBefore(contributionId, submenuManager);

			CommonsUi.activateCallback("newChildCollector");
			newChildCollector.schedule();
		}
		if (newSiblingCollector != null) {
			MenuManager submenuManager = new MenuManager(
					KommaEditUIPlugin.INSTANCE
							.getString("_UI_CreateSibling_menu_item"));
			newSiblingCollector.addMenuManager(submenuManager);
			menuManager.insertBefore(contributionId, submenuManager);

			CommonsUi.activateCallback("newSiblingCollector");
			newSiblingCollector.schedule();
		}
	}

	/**
	 * This implements
	 * {@link org.eclipse.jface.viewers.ISelectionChangedListener}, handling
	 * {@link org.eclipse.jface.viewers.SelectionChangedEvent}s by querying for
	 * the children and siblings that can be added to the selected object and
	 * updating the menus accordingly.
	 */
	public void selectionChanged(final IWorkbenchPart part,
			final IEditingDomain domain, ISelection selection) {
		// Query the new selection for appropriate new child/sibling descriptors
		if (selection instanceof IStructuredSelection
				&& ((IStructuredSelection) selection).size() == 1) {
			final Object object = ((IStructuredSelection) selection)
					.getFirstElement();

			// dispose current collectors
			disposeCollectors();

			newChildCollector = new MenuActionCollector<Object>(
					"Prepare create child actions", selection) {
				@Override
				protected Collection<IAction> generateActions(
						Collection<Object> descriptors) {
					return generateCreateChildActions(part, domain,
							descriptors, selection);
				}

				protected IStatus run(IProgressMonitor monitor) {
					try {
						domain.getModelSet().getUnitOfWork().begin();
						domain.getNewChildDescriptors(object, null, this);
					} finally {
						domain.getModelSet().getUnitOfWork().end();
					}
					return Status.OK_STATUS;
				}

				@Override
				protected void canceling() {
					super.canceling();
					deactivateCallback();
				}

				@Override
				public void done() {
					super.done();
					deactivateCallback();
				}

				void deactivateCallback() {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							CommonsUi.deactivateCallback("newChildCollector");
						}
					});
				}
			};
			if (createChildMenuManager != null) {
				newChildCollector.addMenuManager(createChildMenuManager);
				newChildCollector.schedule();
			}

			newSiblingCollector = new MenuActionCollector<Object>(
					"Prepare create silbing actions", selection) {
				@Override
				protected Collection<IAction> generateActions(
						Collection<Object> descriptors) {
					return generateCreateSiblingActions(part, domain,
							descriptors, selection);
				}

				protected IStatus run(IProgressMonitor monitor) {
					try {
						domain.getModelSet().getUnitOfWork().begin();
						domain.getNewChildDescriptors(null, object, this);
					} finally {
						domain.getModelSet().getUnitOfWork().end();
					}
					return Status.OK_STATUS;
				}

				@Override
				protected void canceling() {
					super.canceling();
					deactivateCallback();
				}

				@Override
				public void done() {
					super.done();
					deactivateCallback();
				}

				void deactivateCallback() {
					display.asyncExec(new Runnable() {
						@Override
						public void run() {
							CommonsUi.deactivateCallback("newChildCollector");
						}
					});
				}
			};
			if (createSiblingMenuManager != null) {
				newSiblingCollector.addMenuManager(createSiblingMenuManager);
				newSiblingCollector.schedule();
			}
		}
	}
}
