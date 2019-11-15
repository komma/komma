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
package net.enilink.komma.edit.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.edit.ui.provider.AdapterFactoryContentProvider;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;
import net.enilink.komma.edit.ui.wizards.NewObjectWizard;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;

public abstract class CreateObjectAction extends Action {
	Shell shell;
	Object[] types;

	public CreateObjectAction(Shell shell) {
		super("Create", null);
		this.shell = shell;
	}

	@Override
	public void run() {
		NewObjectWizard wizard = new NewObjectWizard(getModel(), getModel()
				.getManager().find(OWL.TYPE_THING),
				new AdapterFactoryLabelProvider(getAdapterFactory()),
				new AdapterFactoryContentProvider(getAdapterFactory())) {
			@Override
			public boolean performFinish() {
				final Object[] types = getObjectTypes();
				final URI name = getObjectName();
				shell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						IResource entity = model.getManager().find(name,
								IResource.class);
						try {
							model.getManager().getTransaction().begin();

							for (Object type : types) {
								entity.getRdfTypes().add((IClass) type);
							}

							model.getManager().getTransaction().commit();
						} catch (KommaException e) {
							model.getManager().getTransaction().rollback();
							return;
						}
					}
				});

				return true;
			}
		};

		if (types != null) {
			wizard.setObjectTypes(types);
		}

		WizardDialog wizardDialog = new WizardDialog(shell, wizard);
		wizardDialog.open();
	}

	public void setTypes(Object... types) {
		this.types = types;
	}

	protected abstract IAdapterFactory getAdapterFactory();

	protected abstract IModel getModel();
}
