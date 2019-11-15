/**
 * <copyright> 
 *
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: LoadResourceAction.java,v 1.15 2008/02/29 20:49:08 emerks Exp $
 */
package net.enilink.komma.edit.ui.rcp.commands;

import net.enilink.komma.common.ui.rcp.dialogs.ResourceDialog;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.commands.AbstractHandler;
import net.enilink.komma.edit.ui.rcp.KommaEditUIRCP;
import net.enilink.komma.edit.ui.util.EditUIUtil;
import net.enilink.komma.model.IModel;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * An command handler to load a model into an editing domain's model set.
 */
public class LoadModelHandler extends AbstractHandler {
	@Override
	public void execute(ExecutionEvent event, IProgressMonitor monitor)
			throws ExecutionException {
		LoadModelDialog loadResourceDialog = new LoadModelDialog(PlatformUI
				.getWorkbench().getActiveWorkbenchWindow().getShell(),
				getEditingDomainChecked(event));
		loadResourceDialog.open();
	}

	public static class LoadModelDialog extends ResourceDialog {
		protected IEditingDomain domain;

		public LoadModelDialog(Shell parent) {
			this(parent, null);
		}

		public LoadModelDialog(Shell parent, IEditingDomain domain) {
			super(parent, KommaEditUIRCP.INSTANCE
					.getString("_UI_LoadModelDialog_title"), SWT.OPEN
					| SWT.MULTI);
			this.domain = domain;
		}

		@Override
		protected boolean processResources() {
			if (domain != null) {
				for (URI uri : getURIs()) {
					try {
						if (!processModel(domain.getModelSet().getModel(uri,
								true))) {
							return false;
						}
					} catch (RuntimeException exception) {
						KommaEditUIPlugin.INSTANCE.log(exception);
					}
				}
			}
			return true;
		}

		protected boolean processModel(final IModel model) {
			// try to open model in editor
			getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						EditUIUtil.openEditor(model.getOntology());
					} catch (PartInitException e) {
						KommaEditUIPlugin.INSTANCE.log(e);
					}
				}
			});
			return true;
		}
	}
}
