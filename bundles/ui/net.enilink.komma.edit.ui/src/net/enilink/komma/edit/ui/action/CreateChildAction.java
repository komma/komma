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
 * $Id: CreateChildAction.java,v 1.5 2007/09/30 10:31:29 emerks Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.CommandWrapper;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.edit.command.CreateChildCommand;
import net.enilink.komma.edit.command.IChildDescriptor;
import net.enilink.komma.edit.command.ICommandActionDelegate;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.provider.AdapterFactory;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelAware;

/**
 * A child creation action is implemented by creating a
 * {@link CreateChildCommand}.
 */
public class CreateChildAction extends StaticSelectionCommandAction {
	class ExtendedCreateChildCommand extends CommandWrapper implements
			ICommandActionDelegate {
		Object owner;

		ExtendedCreateChildCommand(Object owner, ICommand createChildCommand) {
			super(createChildCommand);
			this.owner = owner;
		}

		@Override
		protected CommandResult doExecuteWithResult(
				IProgressMonitor progressMonitor, IAdaptable info)
				throws ExecutionException {
			final IModel model;
			if (owner instanceof IModelAware) {
				model = ((IModelAware) owner).getModel();
			} else if (owner instanceof IModel) {
				model = (IModel) owner;
			} else {
				return CommandResult
						.newErrorCommandResult("No model provided for this operation.");
			}

			final IChildDescriptor childDescriptor = (IChildDescriptor) descriptor;

			Object treeInput = null;
			ILabelProvider labelProvider = null;
			ITreeContentProvider treeContentProvider = null;
			if (childDescriptor.requiresType()) {
				treeInput = model.getManager().find(
						OWL.NAMESPACE_URI.appendFragment("Thing"));
				treeContentProvider = new AdapterFactoryContentProvider(
						getAdapterFactory());
				labelProvider = new AdapterFactoryLabelProvider(
						getAdapterFactory());
			}

			NewObjectWizard newWizard = new NewObjectWizard(model, treeInput,
					labelProvider, treeContentProvider) {
				@Override
				public boolean performFinish() {
					if (showTypePage()) {
						Object[] types = getObjectTypes();

						((IChildDescriptor) descriptor).setTypes(Arrays
								.asList(types));
					}
					if (showNamePage()) {
						((IChildDescriptor) descriptor)
								.setName(getObjectName());
					}
					return true;
				}

				@Override
				protected boolean showNamePage() {
					return childDescriptor.requiresName();
				}
			};

			WizardDialog wizardDialog = new WizardDialog(Display.getCurrent()
					.getActiveShell(), newWizard);
			if (wizardDialog.open() != Window.OK) {
				return CommandResult.newCancelledCommandResult();
			}

			return super.doExecuteWithResult(progressMonitor, info);
		}

		@Override
		public Object getImage() {
			return ((ICommandActionDelegate) getCommand()).getImage();
		}

		@Override
		public String getText() {
			return ((ICommandActionDelegate) getCommand()).getText();
		}

		@Override
		public String getToolTipText() {
			return ((ICommandActionDelegate) getCommand()).getToolTipText();
		}

		@Override
		public String getDescription() {
			return ((ICommandActionDelegate) getCommand()).getDescription();
		}
	}

	/**
	 * This describes the child to be created.
	 */
	protected Object descriptor;

	/**
	 * This constructs an instance of an action that uses the workbench part's
	 * editing domain to create a child specified by <code>descriptor</code> for
	 * the single object in the <code>selection</code>.
	 */
	public CreateChildAction(IWorkbenchPart workbenchPart,
			ISelection selection, Object descriptor) {
		super(workbenchPart, selection);
		this.descriptor = descriptor;
	}

	/**
	 * This constructs an instance of an action that uses the given editing
	 * domain to create a child specified by <code>descriptor</code> for the
	 * single object in the <code>selection</code>.
	 */
	public CreateChildAction(IWorkbenchPart workbenchPart,
			IEditingDomain editingDomain, ISelection selection,
			Object descriptor) {
		super(workbenchPart, editingDomain, selection);
		this.descriptor = descriptor;
	}

	/**
	 * This creates the command for
	 * {@link StaticSelectionCommandAction#createActionCommand}.
	 */
	@Override
	protected ICommand createActionCommand(IEditingDomain editingDomain,
			Collection<?> collection) {
		if (collection.size() == 1) {
			final Object owner = collection.iterator().next();
			ICommand createChildCommand = createCreateChildCommand(
					editingDomain, owner, collection);

			if (createChildCommand.canExecute()
					&& descriptor instanceof IChildDescriptor
					&& ((IChildDescriptor) descriptor).requiresName()) {
				return new ExtendedCreateChildCommand(owner, createChildCommand);
			}
			return createChildCommand;
		}
		return UnexecutableCommand.INSTANCE;
	}

	protected ICommand createCreateChildCommand(IEditingDomain editingDomain,
			Object owner, Collection<?> collection) {
		return CreateChildCommand.create(editingDomain, owner, descriptor,
				collection);
	}

	protected IAdapterFactory getAdapterFactory() {
		if (editingDomain instanceof AdapterFactoryEditingDomain) {
			return ((AdapterFactoryEditingDomain) editingDomain)
					.getAdapterFactory();
		}
		return new AdapterFactory() {
			@Override
			protected Object createAdapter(Object object, Object type) {
				return null;
			}
		};
	}
}
