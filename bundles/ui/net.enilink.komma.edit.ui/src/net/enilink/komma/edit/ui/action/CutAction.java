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

import java.util.Collection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.command.CutToClipboardCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

/**
 * A cut action is implemented by creating a {@link CutToClipboardCommand}.
 */
public class CutAction extends CommandActionHandler {
	public CutAction(IWorkbenchPage page, IEditingDomain domain) {
		super(page, domain, KommaEditUIPlugin.INSTANCE
				.getString("_UI_Cut_menu_item"));
	}

	public CutAction(IWorkbenchPage page) {
		super(page, null, KommaEditUIPlugin.INSTANCE.getString("_UI_Cut_menu_item"));
	}

	@Override
	public ICommand createCommand(Collection<?> selection) {
		return CutToClipboardCommand.create(domain, selection);
	}

	public void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);
		if (workbenchPart instanceof IEditingDomainProvider) {
			domain = ((IEditingDomainProvider) workbenchPart)
					.getEditingDomain();
		}
	}
}
