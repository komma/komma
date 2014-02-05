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
 * $Id: PasteAction.java,v 1.4 2006/12/28 06:50:04 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.Collection;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.edit.command.PasteFromClipboardCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A paste action is implemented by creating a {@link PasteFromClipboardCommand}
 * .
 */
public class PasteAction extends CommandActionHandler {
	public PasteAction(IWorkbenchPage page, IEditingDomain domain) {
		super(page, domain, KommaEditUIPlugin.INSTANCE
				.getString("_UI_Paste_menu_item"));
	}

	public PasteAction(IWorkbenchPage page) {
		super(page, null, KommaEditUIPlugin.INSTANCE
				.getString("_UI_Paste_menu_item"));
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		Display display = Display.getCurrent();
		if (display != null && display.getFocusControl() instanceof Text) {
			((Text) display.getFocusControl()).paste();
		} else {
			super.doRun(progressMonitor);
		}
	}

	@Override
	public ICommand createCommand(Collection<?> selection) {
		if (selection.size() == 1) {
			return PasteFromClipboardCommand.create(domain, selection
					.iterator().next(), null);
		} else {
			return IdentityCommand.INSTANCE;
		}
	}

	public void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);
		if (workbenchPart instanceof IEditingDomainProvider) {
			domain = ((IEditingDomainProvider) workbenchPart)
					.getEditingDomain();
		}
	}
}
