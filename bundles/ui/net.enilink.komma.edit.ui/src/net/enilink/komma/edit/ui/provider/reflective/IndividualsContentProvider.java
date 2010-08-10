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

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;

public class IndividualsContentProvider extends ModelContentProvider implements
		IStructuredContentProvider, ILazyContentProvider {
	protected Set<IClass> classes = new HashSet<IClass>();

	protected IReference[] instanceReferences;

	protected AbstractTableViewer viewer;

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

	public Object[] getElements(Object inputElement) {
		if (!classes.isEmpty()) {
			Collection<Object> instances = new LinkedHashSet<Object>();
			for (IClass clazz : classes) {
				instances.addAll(clazz.getInstances());
			}

			return instances.toArray();
		}
		return new Object[0];
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		instanceReferences = null;
		classes.clear();
		if (newInput instanceof IClass) {
			classes.add((IClass) newInput);
		} else if (newInput != null && newInput.getClass().isArray()) {
			Object[] array = (Object[]) newInput;
			for (Object element : array) {
				classes.add((IClass) element);
			}
		}

		IModel newModel = null;
		if (!classes.isEmpty()) {
			newModel = ((IObject) classes.iterator().next()).getModel();
		}

		super.inputChanged(viewer, this.model, newModel);
	}

	@Override
	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (viewer instanceof AbstractTableViewer) {
			this.viewer = (AbstractTableViewer) viewer;
		} else {
			this.viewer = null;
		}

		// check if viewer is virtual
		if ((viewer.getControl().getStyle() & SWT.VIRTUAL) != 0) {
			Collection<IReference> instances = new LinkedHashSet<IReference>();
			for (IClass clazz : classes) {
				instances.addAll(clazz.getInstancesAsReferences());
			}
			instanceReferences = instances.toArray(new IReference[instances
					.size()]);
			this.viewer.setItemCount(instanceReferences.length);
		}
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

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return this.viewer != null && !classes.isEmpty();
	}

	@Override
	public void updateElement(int index) {
		if (index < instanceReferences.length) {
			viewer.replace(this.model.resolve(instanceReferences[index]), index);
		}
	}
}