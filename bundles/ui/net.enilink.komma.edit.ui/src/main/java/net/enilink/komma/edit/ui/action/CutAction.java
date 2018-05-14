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
 * $Id: CutAction.java,v 1.4 2006/12/28 06:50:04 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.lang.reflect.Method;
import java.util.Collection;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.edit.command.CutToClipboardCommand;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;

/**
 * A cut action is implemented by creating a {@link CutToClipboardCommand}.
 */
public class CutAction extends CommandActionHandler {
	static Method CUT_METHOD;
	static {
		try {
			CUT_METHOD = Text.class.getMethod("cut");
		} catch (Exception e) {
			// ignore
		}
	}

	public CutAction(IWorkbenchPage page) {
		super(page, KommaEditUIPlugin.INSTANCE.getString("_UI_Cut_menu_item"));
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		Display display = Display.getCurrent();
		if (CUT_METHOD != null && display != null
				&& display.getFocusControl() instanceof Text) {
			try {
				CUT_METHOD.invoke(display.getFocusControl());
			} catch (Exception e) {
				// ignore
			}
		} else {
			super.doRun(progressMonitor);
		}
	}

	@Override
	public ICommand createCommand(Collection<?> selection) {
		ICommand cmd = CutToClipboardCommand.create(domain, selection);
		return cmd.canExecute() ? cmd : IdentityCommand.INSTANCE;
	}
}
