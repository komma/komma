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
package net.enilink.komma.edit.ui.provider.reflective;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.vocab.owl.OWL;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.em.concepts.IResource;

public class ModelImportsContentProvider extends ModelContentProvider implements
		IStructuredContentProvider {
	private TableViewer viewer;
	private IResource subject;
	private IEntity property;

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof IResource) {
			subject = (IResource) newInput;
		} else {
			subject = null;
		}
		super.inputChanged(viewer, oldInput, newInput);
		if (model != null) {
			property = (IEntity) model.resolve(OWL.PROPERTY_IMPORTS);
		} else {
			property = null;
		}
	}

	public Object[] getElements(Object inputElement) {
		if (subject != null && property != null) {
			return subject.getPropertyStatements(property, false).toSet()
					.toArray();
		}
		return new Object[0];
	}

	@Override
	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (viewer instanceof TableViewer) {
			this.viewer = (TableViewer) viewer;
		} else {
			this.viewer = null;
		}
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return subject != null && viewer != null;
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (property == null) {
			return false;
		}
		if (property.equals(stmt.getPredicate())
				&& stmt.getSubject().equals(subject)) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (property == null) {
			return false;
		}
		if (property.equals(stmt.getPredicate())
				&& stmt.getSubject().equals(subject)) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}
}
