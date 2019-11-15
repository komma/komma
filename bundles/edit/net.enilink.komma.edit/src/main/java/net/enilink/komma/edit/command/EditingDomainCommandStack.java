/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.command;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.command.AbstractCommand.INoChangeRecording;
import net.enilink.komma.common.command.AbstractCommand.INonDirtying;
import net.enilink.komma.common.command.BasicCommandStack;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.edit.domain.IEditingDomain;

public class EditingDomainCommandStack extends BasicCommandStack {
	private IEditingDomain domain;

	public IEditingDomain getDomain() {
		return domain;
	}

	public void setEditingDomain(IEditingDomain domain) {
		this.domain = domain;
	}

	@Override
	public IStatus execute(ICommand command, IProgressMonitor monitor,
			IAdaptable info) throws ExecutionException {
		if (command != null
				&& command.canExecute()
				&& !(command instanceof INoChangeRecording || command instanceof INonDirtying)) {
			command = new RecordingWrapperCommand(domain, command);
		}
		return super.execute(command, monitor, info);
	}

}
