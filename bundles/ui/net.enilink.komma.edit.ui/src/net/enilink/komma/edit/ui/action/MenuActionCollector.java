package net.enilink.komma.edit.ui.action;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.progress.WorkbenchJob;

import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.edit.util.CollectorJob;

/**
 * {@link ICollector} implementation that can be used to populate multiple menu
 * managers with actions.
 */
abstract public class MenuActionCollector<T> extends CollectorJob<T> {
	private static final IContributionItem loadingIndicatorItem = new ActionContributionItem(
			new Action("loading ...") {
			});

	protected Collection<IAction> actions;
	protected Set<IMenuManager> menuManagers = new HashSet<IMenuManager>();
	protected volatile ISelection selection;

	public MenuActionCollector(String name, ISelection selection) {
		super(name);
		this.selection = selection;
	}

	public void addMenuManager(IMenuManager menuManager) {
		if (menuManager != null && menuManagers.add(menuManager)) {
			populateManager(menuManager, actions, loadingIndicatorItem.getId());
		}
	}

	@Override
	protected void canceling() {
		super.canceling();
		selection = null;
	}

	/**
	 * This removes from the specified <code>manager</code> all
	 * {@link org.eclipse.jface.action.ActionContributionItem}s based on the
	 * {@link org.eclipse.jface.action.IAction}s contained in the
	 * <code>actions</code> collection.
	 */
	protected void depopulateManager(IContributionManager manager,
			Collection<? extends IAction> actions) {
		manager.remove(loadingIndicatorItem);

		if (actions != null) {
			IContributionItem[] items = manager.getItems();
			for (int i = 0; i < items.length; i++) {
				// Look into SubContributionItems
				IContributionItem contributionItem = items[i];
				while (contributionItem instanceof SubContributionItem) {
					contributionItem = ((SubContributionItem) contributionItem)
							.getInnerItem();
				}

				if (contributionItem instanceof ActionContributionItem) {
					IAction action = ((ActionContributionItem) contributionItem)
							.getAction();
					if (actions.contains(action)) {
						manager.remove(contributionItem);
					}
				}
			}
		}
	}

	public void dispose() {
		cancel();

		for (IMenuManager menuManager : menuManagers) {
			depopulateManager(menuManager, actions);
		}
		selection = null;
	}

	@Override
	protected void handleObjects(final Collection<T> descriptors) {
		new WorkbenchJob("Create actions") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (selection != null) {
					Collection<IAction> newActions = generateActions(descriptors);
					if (actions == null) {
						actions = newActions;
					} else {
						actions.addAll(newActions);
					}

					for (IMenuManager menuManager : menuManagers) {
						populateManager(menuManager, newActions,
								loadingIndicatorItem.getId());
						menuManager.update(true);
					}
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	abstract protected Collection<IAction> generateActions(
			Collection<T> descriptors);

	/**
	 * This populates the specified <code>manager</code> with
	 * {@link org.eclipse.jface.action.ActionContributionItem}s based on the
	 * {@link org.eclipse.jface.action.IAction}s contained in the
	 * <code>actions</code> collection, by inserting them before the specified
	 * contribution item <code>contributionID</code>. If
	 * <code>contributionID</code> is <code>null</code>, they are simply added.
	 */
	protected void populateManager(IContributionManager manager,
			Collection<? extends IAction> actions, String contributionID) {
		if (actions != null) {
			if (done) {
				manager.remove(loadingIndicatorItem);
			}

			for (IAction action : actions) {
				if (contributionID != null) {
					manager.insertBefore(contributionID, action);
				} else {
					manager.add(action);
				}
			}
		} else {
			manager.add(loadingIndicatorItem);
		}
	}
}