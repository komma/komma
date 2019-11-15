/******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation 
 ****************************************************************************/

package net.enilink.komma.edit.ui.action;

import org.eclipse.jface.action.AbstractGroupMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.LabelRetargetAction;

import net.enilink.komma.common.util.Log;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

/**
 * An implementation of an <code>IMenuManager</code> that inherits its UI (text
 * + icon + hints) from a given action.
 * 
 * When filled in a toolbar, the menu is rendered as a tool item with two parts:
 * a button, whose icon comes from the supplied action handler, and a drop-down
 * menu arrow. When the arrow is pressed, the drop-down menu is shown. When the
 * button is pressed, the associated action is executed. The manager can have an
 * optional style to retarget the last executed action. In this case the tool
 * item UI reflects the last executed sub-action from the menu.
 * 
 * When filled in a menu, this menu shows up as a normal cascading menu with its
 * GUI inherited from the supplied action.
 * 
 * @author melaasar
 */
public class ActionMenuManager extends MenuManager {

	/**
	 * An action that provides a menu and fills it from the contribution items
	 * of the enclosing menu manager. It also retargets to the manager's
	 * supplied action handler.
	 */
	public class MenuCreatorAction extends LabelRetargetAction implements
			IMenuCreator {
		// the menu widget
		private Menu menu;

		// menu item selection listener: listens to selection events
		private Listener menuItemListener = new Listener() {
			public void handleEvent(Event event) {
				if (SWT.Selection == event.type && !event.widget.isDisposed()) {
					ActionContributionItem item = (ActionContributionItem) event.widget
							.getData();
					if (retargetLastAction) {
						setActionHandler(item.getAction());
						setDefaultAction(item.getAction());
					}
					subActionSelected(item.getAction());
				}
			}
		};

		/**
		 * Creates a new menu creator action
		 * 
		 * @param actionHandler
		 *            the action handler
		 */
		public MenuCreatorAction(IAction actionHandler) {
			super(actionHandler.getId(), actionHandler.getText());
			setEnabled(false); // initially untill a menu item is added
			setActionHandler(actionHandler);
			setMenuCreator(this);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets
		 * .Control)
		 */
		public Menu getMenu(Control parent) {
			if (menu != null)
				menu.dispose();

			menu = new Menu(parent);
			return createMenu(menu);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets
		 * .Menu)
		 */
		public Menu getMenu(Menu parent) {
			if (menu != null)
				menu.dispose();
			menu = new Menu(parent);
			return createMenu(menu);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.ui.actions.ActionFactory.IWorkbenchAction#dispose()
		 */
		public void dispose() {
			if (menu != null) {
				menu.dispose();
				menu = null;
			}
			super.dispose();
			ActionMenuManager.this.dispose();
		}

		/**
		 * Create the drop-down/pop-up menu.
		 * 
		 * @param mnu
		 *            <code>Menu</code> for which to create the drop-down/pop-up
		 *            menu
		 * @return <code>Menu</code> the drop-down/pop-up menu
		 */
		protected Menu createMenu(Menu mnu) {
			IContributionItem[] items = getRealItems();
			IContributionItem lastGroupMarker = null;
			for (int i = 0; i < items.length; i++) {
				IContributionItem item = items[i];
				if (item instanceof AbstractGroupMarker) {
					if (i == 0 || i == items.length - 1
							|| items[i + 1] instanceof AbstractGroupMarker
							|| mnu.getItemCount() < 1 || !item.isVisible()) {
						continue;
					} else {
						// Do not add last group marker until we know that there
						// will be items following it.
						lastGroupMarker = item;
					}
				} else {
					if (!item.isVisible()) {
						continue;
					}
					try {
						if (lastGroupMarker != null) {
							lastGroupMarker.fill(menu, -1);
							lastGroupMarker = null;
						}
						item.fill(menu, -1);
					} catch (Exception e) {
						Log
								.info(
										KommaEditUIPlugin.getPlugin(),
										0,
										"The contribution item (" + item.getId() + ") failed to fill within the menu"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			MenuItem menuItems[] = mnu.getItems();
			for (int i = 0; i < menuItems.length; i++) {
				if (menuItems[i].getStyle() == SWT.SEPARATOR)
					continue;
				menuItems[i].addListener(SWT.Selection, menuItemListener);
			}
			return mnu;
		}

		/**
		 * Ignores the action handler's "enable" event since "enablement" is
		 * determined by the sub-action(s) enablement state
		 * 
		 */
		protected void propagateChange(PropertyChangeEvent event) {
			if (!event.getProperty().equals(Action.ENABLED))
				super.propagateChange(event);
		}

		/**
		 * Ignores the action handler's "enable" event since "enablement" is
		 * determined by the sub-action(s)
		 * 
		 */
		protected void setActionHandler(IAction handler) {
			boolean enabled = MenuCreatorAction.this.isEnabled();
			super.setActionHandler(handler);
			MenuCreatorAction.this.setEnabled(enabled);
		}

		/**
		 * Only run the action handler if it is enabled
		 * 
		 */
		public void run() {
			if (getActionHandler() != null && getActionHandler().isEnabled())
				super.run();
			else if (getDefaultAction().isEnabled()) {
				setActionHandler(getDefaultAction());
				super.run();
			}
		}

		/**
		 * Only run the action handler if it is enabled
		 * 
		 */
		public void runWithEvent(Event event) {
			if (getActionHandler() != null && getActionHandler().isEnabled())
				super.runWithEvent(event);
			else if (getDefaultAction().isEnabled()) {
				setActionHandler(getDefaultAction());
				super.runWithEvent(event);
			}
		}

	}

	/** the associated menu action */
	protected final MenuCreatorAction action;

	/** the associated menu action */
	protected IAction defaultAction = null;

	/** the delege action contribution item */
	private final ActionContributionItem actionContributionItem;

	/** an option to retarget the last action */
	private boolean retargetLastAction;

	/**
	 * Creates a new instance of <code>ActionMenuManager</code> with a given
	 * action handler. The manager does not retarget the last selected action
	 * from the menu
	 * 
	 * @param id
	 *            The menu manager id
	 * @param actionHandler
	 *            the menu associated action handler
	 */
	public ActionMenuManager(String id, IAction actionHandler) {
		this(id, actionHandler, false);
	}

	/**
	 * Creates a new instance of <code>ActionMenuManager</code> with a given
	 * action handler and an option to retarget the last executed menu action.
	 * 
	 * @param id
	 *            The menu manager id
	 * @param actionHandler
	 *            the menu associated action handler
	 * @param retargetLastAction
	 *            whether to retarget the last action or not
	 */
	public ActionMenuManager(String id, IAction actionHandler,
			boolean retargetLastAction) {
		super(actionHandler.getText(), id);
		assert null != actionHandler;
		action = new MenuCreatorAction(actionHandler);
		defaultAction = actionHandler;
		actionContributionItem = new ActionContributionItem(action);
		this.retargetLastAction = retargetLastAction;
	}

	/**
	 * Returns whether the option to retarget last action was requested
	 * 
	 * @return <code>true</code> if retargetLastAction is enabled,
	 *         <code>false</code> otherwise
	 */
	protected boolean isRetargetLastAction() {
		return retargetLastAction;
	}

	/**
	 * Handle subaction selection
	 * 
	 * @param subActionHandler
	 *            The selected sub action handler
	 */
	protected void subActionSelected(IAction subActionHandler) {
		/* method not implemented */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets
	 * .Composite)
	 */
	public void fill(Composite parent) {
		// this is only relevant in toolbars
		retargetLastAction = false;
		actionContributionItem.fill(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets
	 * .Menu, int)
	 */
	public void fill(Menu parent, int index) {
		// this is only relevant in toolbars
		retargetLastAction = false;
		actionContributionItem.fill(parent, index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets
	 * .ToolBar, int)
	 */
	public void fill(ToolBar parent, int index) {
		actionContributionItem.fill(parent, index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#dispose()
	 */
	public void dispose() {
		actionContributionItem.dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#isEnabled()
	 */
	public boolean isEnabled() {
		return actionContributionItem.isEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionManager#isDirty()
	 */
	public boolean isDirty() {
		return actionContributionItem.isDirty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#isDynamic()
	 */
	public boolean isDynamic() {
		return actionContributionItem.isDynamic();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#isGroupMarker()
	 */
	public boolean isGroupMarker() {
		return actionContributionItem.isGroupMarker();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#isSeparator()
	 */
	public boolean isSeparator() {
		return actionContributionItem.isSeparator();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#isVisible()
	 */
	public boolean isVisible() {
		IContributionItem[] items = getRealItems();
		for (int i = 0; i < items.length; i++) {
			IContributionItem item = items[i];
			if (!(item instanceof AbstractGroupMarker) && item.isVisible()) {
				return actionContributionItem.isVisible();
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.IContributionItem#setParent(org.eclipse.jface
	 * .action.IContributionManager)
	 */
	public void setParent(IContributionManager parent) {
		actionContributionItem.setParent(parent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		actionContributionItem.setVisible(visible);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#update()
	 */
	public void update() {
		actionContributionItem.update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionItem#update(java.lang.String)
	 */
	public void update(String id) {
		actionContributionItem.update(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IMenuManager#updateAll(boolean)
	 */
	public void updateAll(boolean force) {
		update(force);

		IContributionItem[] items = getRealItems();
		for (int i = 0; i < items.length; ++i) {
			IContributionItem ci = items[i];
			if (ci instanceof IMenuManager) {
				IMenuManager mm = (IMenuManager) ci;
				if (mm.isVisible()) {
					mm.updateAll(force);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.IContributionManager#update(boolean)
	 */
	public void update(boolean force) {
		update();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.ContributionManager#itemAdded(org.eclipse.jface
	 * .action.IContributionItem)
	 */
	protected void itemAdded(IContributionItem item) {
		super.itemAdded(item);
		if (item instanceof SubContributionItem)
			item = ((SubContributionItem) item).getInnerItem();
		if (!item.isGroupMarker())
			action.setEnabled(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.action.ContributionManager#itemRemoved(org.eclipse.
	 * jface.action.IContributionItem)
	 */
	protected void itemRemoved(IContributionItem item) {
		super.itemRemoved(item);
		if (item instanceof SubContributionItem)
			item = ((SubContributionItem) item).getInnerItem();
		if (!item.isGroupMarker()) {
			action.setEnabled(false);
			IContributionItem[] items = getItems();
			for (int i = 0; i < items.length; i++)
				if (!items[i].isGroupMarker()) {
					action.setEnabled(true);
					break;
				}
		}
	}

	/**
	 * Returns the contribution items of this manager. If an item is wrapper in
	 * a SubContributionItem instance it extracts the real item instance
	 * 
	 * @return An array of real items of this contribution manager
	 */
	protected IContributionItem[] getRealItems() {
		IContributionItem[] items = getItems();
		IContributionItem[] realItems = new IContributionItem[items.length];
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof SubContributionItem) {
				realItems[i] = ((SubContributionItem) items[i]).getInnerItem();
			} else {
				realItems[i] = items[i];
			}
		}
		return realItems;
	}

	public IAction getDefaultAction() {
		return defaultAction;
	}

	protected void setDefaultAction(IAction defaultAction) {
		this.defaultAction = defaultAction;
	}

}
