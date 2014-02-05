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
 * $Id: CopyAction.java,v 1.4 2006/12/28 06:50:05 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.enilink.komma.common.command.AbstractCommand;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ExtendedCompositeCommand;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.edit.command.CopyToClipboardCommand;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A copy action is implemented by creating a {@link CopyToClipboardCommand}.
 */
public class CopyAction extends CommandActionHandler {
	public CopyAction(IWorkbenchPage page, IEditingDomain domain) {
		super(page, domain, KommaEditUIPlugin.INSTANCE
				.getString("_UI_Copy_menu_item"));
	}

	public CopyAction(IWorkbenchPage page) {
		super(page, null, KommaEditUIPlugin.INSTANCE
				.getString("_UI_Copy_menu_item"));
	}

	@Override
	protected void doRun(IProgressMonitor progressMonitor) {
		Display display = Display.getCurrent();
		if (display != null && display.getFocusControl() instanceof Text) {
			((Text) display.getFocusControl()).copy();
		} else {
			super.doRun(progressMonitor);
		}
	}

	static class ExtendedCopyCommand extends ExtendedCompositeCommand implements
			AbstractCommand.INonDirtying {
		ExtendedCopyCommand(String label, List<ICommand> commandList) {
			super(label, commandList);
		}
	}

	@Override
	public ICommand createCommand(final Collection<?> selection) {
		ICommand cmd = CopyToClipboardCommand.create(domain, selection);
		return cmd.canExecute() ? new ExtendedCopyCommand(cmd.getLabel(),
				Arrays.asList(new SimpleCommand() {
					@Override
					protected CommandResult doExecuteWithResult(
							IProgressMonitor progressMonitor, IAdaptable info)
							throws ExecutionException {
						if (Display.getCurrent() != null) {
							StringBuilder sb = new StringBuilder();
							for (Iterator<?> it = selection.iterator(); it
									.hasNext();) {
								sb.append(it.next().toString());
								if (it.hasNext()) {
									sb.append(System.lineSeparator());
								}
							}
							Clipboard clipboard = new Clipboard(null);
							clipboard.setContents(
									new Object[] { sb.toString() },
									new Transfer[] { TextTransfer.getInstance() });
							clipboard.dispose();
						}
						return CommandResult.newOKCommandResult();
					}
				}, cmd))
				: IdentityCommand.INSTANCE;
	}

	public void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);
		if (workbenchPart instanceof IEditingDomainProvider) {
			domain = ((IEditingDomainProvider) workbenchPart)
					.getEditingDomain();
		}
	}
}
