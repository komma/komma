package net.enilink.komma.edit.ui.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.WorkbenchJob;

import net.enilink.komma.common.util.ICollector;
import net.enilink.komma.edit.ui.provider.ExtendedImageRegistry;
import net.enilink.komma.edit.util.CollectorJob;

/**
 * {@link ICollector} implementation that can be used to populate multiple menu
 * managers with actions.
 */
abstract public class MenuActionCollector<T> extends CollectorJob<T> {
	protected boolean shouldSchedule = true;

	private static final IContributionItem loadingIndicatorItem = new ActionContributionItem(
			new Action("loading ...") {
			});

	private static final int MAX_MENU_ENTRIES = 20;
	protected Collection<IAction> menuActions;
	protected Collection<IAction> allActions;
	protected WeakHashMap<IMenuManager, Boolean> menuManagers = new WeakHashMap<IMenuManager, Boolean>();
	protected volatile ISelection selection;
	protected Display display = Display.getCurrent();
	protected List<Job> handlers = new ArrayList<Job>();

	class ShowAllCreateActions extends Action {
		public ShowAllCreateActions() {
			super("other...");
		}

		@Override
		public void run() {
			ElementListSelectionDialog selectionDialog = new ElementListSelectionDialog(
					display.getActiveShell(), new LabelProvider() {
						@Override
						public String getText(Object element) {
							return ((IAction) element).getText();
						}

						@Override
						public Image getImage(Object element) {
							return ExtendedImageRegistry.getInstance()
									.getImage(
											((IAction) element)
													.getImageDescriptor());
						}
					});
			selectionDialog.setHelpAvailable(false);
			selectionDialog.setElements(MenuActionCollector.this.allActions
					.toArray());
			if (selectionDialog.open() == Window.OK) {
				IAction selected = (IAction) selectionDialog.getFirstResult();
				if (selected != null) {
					selected.run();
				}
			}
		}
	};

	public MenuActionCollector(String name, ISelection selection) {
		super(name);
		this.selection = selection;
	}

	public void addMenuManager(IMenuManager menuManager) {
		if (menuManager != null && menuManagers.put(menuManager, true) != null) {
			populateManager(menuManager, menuActions,
					loadingIndicatorItem.getId());
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
		for (IMenuManager menuManager : menuManagers.keySet()) {
			depopulateManager(menuManager, menuActions);
		}
		selection = null;
	}

	@Override
	public void done() {
		super.done();
		synchronized (handlers) {
			while (!handlers.isEmpty()) {
				try {
					handlers.wait();
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	@Override
	public boolean shouldSchedule() {
		// force job to be only scheduled once
		if (shouldSchedule) {
			shouldSchedule = false;
			return true;
		}
		return false;
	}

	@Override
	protected void handleObjects(final Collection<T> descriptors) {
		final Job handler = new WorkbenchJob(display, "Create actions") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					if (selection != null) {
						int startActionsSize = allActions == null ? 0
								: allActions.size();

						Collection<IAction> newActions = generateActions(descriptors);
						if (allActions == null) {
							allActions = newActions;
						} else {
							allActions.addAll(newActions);
						}

						if (allActions.size() > MAX_MENU_ENTRIES) {
							// show only MAX_MENU_ENTRIES
							if (startActionsSize < MAX_MENU_ENTRIES) {
								newActions = new ArrayList<IAction>(newActions)
										.subList(0, MAX_MENU_ENTRIES
												- startActionsSize);
								newActions.add(new ShowAllCreateActions());
							} else {
								// do not add any more actions
								return Status.OK_STATUS;
							}
						}
						if (menuActions == null) {
							menuActions = new ArrayList<IAction>();
						}
						menuActions.addAll(newActions);
						for (IMenuManager menuManager : menuManagers.keySet()) {
							populateManager(menuManager, newActions,
									loadingIndicatorItem.getId());
							menuManager.update(true);
						}
					}
					return Status.OK_STATUS;
				} finally {
					synchronized (handlers) {
						handlers.remove(this);
						handlers.notify();
					}
				}
			}
		};
		synchronized (handlers) {
			handlers.add(handler);
		}
		handler.schedule();
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