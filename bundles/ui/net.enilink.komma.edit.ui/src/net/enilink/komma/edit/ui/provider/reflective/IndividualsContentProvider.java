/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.ui.provider.reflective;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IStatement;

public class IndividualsContentProvider extends ModelContentProvider implements
		IStructuredContentProvider {
	protected StructuredViewer viewer;

	protected Set<IClass> classes = new HashSet<IClass>();

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		classes.clear();
		if (newInput instanceof IClass) {
			classes.add((IClass) newInput);
		} else if (newInput != null && newInput.getClass().isArray()) {
			Object[] array = (Object[]) newInput;
			for (Object element : array) {
				classes.add((IClass) element);
			}
		}

		IModel newOntModel = null;
		if (!classes.isEmpty()) {
			newOntModel = ((IObject) classes.iterator().next()).getModel();
		}

		super.inputChanged(viewer, this.model, newOntModel);
	}

	public Object[] getElements(Object inputElement) {
		if (!classes.isEmpty()) {
			Collection<Object> individuals = new LinkedHashSet<Object>();
			for (IClass ontClass : classes) {
				individuals.addAll(ontClass.getInstances(true));
			}

			return individuals.toArray();
		}
		return new Object[0];
	}

	@Override
	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (viewer instanceof StructuredViewer) {
			this.viewer = (StructuredViewer) viewer;
		} else {
			this.viewer = null;
		}
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return this.viewer != null && !classes.isEmpty();
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (RDF.PROPERTY_TYPE.equals(stmt.getPredicate())
				&& stmt.getObject() instanceof IClass) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		// TODO
		// maybe broken if reasoner is used ...
		// listener gets not notified if inferred statements are removed
		if (RDF.PROPERTY_TYPE.equals(stmt.getPredicate())
				&& stmt.getObject() instanceof IClass) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}
}