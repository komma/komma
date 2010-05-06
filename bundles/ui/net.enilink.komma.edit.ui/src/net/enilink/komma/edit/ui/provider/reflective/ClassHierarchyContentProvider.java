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

import static net.enilink.komma.util.CollectionUtil.isEmpty;
import static net.enilink.komma.util.CollectionUtil.safe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.model.IModel;
import net.enilink.komma.core.IStatement;

public class ClassHierarchyContentProvider extends ModelContentProvider
		implements ITreeContentProvider {
	private boolean showProperties;
	private TreeViewer viewer;

	public ClassHierarchyContentProvider(boolean showProperties) {
		this.showProperties = true;// showProperties;
	}

	public boolean getShowProperties() {
		return showProperties;
	}

	public void setShowProperties(boolean showProperties) {
		this.showProperties = showProperties;
	}

	protected Iterator<IClass> listRootClasses(IModel model) {
		return model.getOntology().getRootClasses().iterator();
	}

	public Object[] getElements(Object inputElement) {
		if (!(inputElement instanceof IModel)) {
			if (inputElement instanceof Collection<?>) {
				return ((Collection<?>) inputElement).toArray();
			}
			return new Object[0];
		}

		final IModel model = (IModel) inputElement;

		LinkedList<IClass> rootList = new LinkedList<IClass>();
		for (Iterator<?> it = listRootClasses(model); it.hasNext();) {
			rootList.add((IClass) it.next());
		}

		Set<IClass> elements = new LinkedHashSet<IClass>();
		while (!rootList.isEmpty()) {
			IClass root = rootList.removeFirst();
			if (root.getURI() != null) {
				// if (rootTypesSet == null
				// || rootTypesSet.contains(root.getURI())
				// || rootTypesSet
				// .contains(model.shortForm(root.getURI()))) {
				elements.add(root);
			} else {
				for (Iterator<IClass> it = root.getDirectNamedSubClasses()
						.iterator(); it.hasNext();) {
					rootList.addFirst(it.next());
				}
			}
		}

		return elements.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		List<Object> children = new ArrayList<Object>();
		if (parentElement instanceof IClass) {
			IClass ontClass = (IClass) parentElement;
			if (!isEmpty(ontClass.getOwlUnionOf())) {
				for (net.enilink.vocab.owl.Class clazz : safe(ontClass
						.getOwlUnionOf())) {
					children.add(clazz);
				}
			} else if (!isEmpty(ontClass.getOwlIntersectionOf())) {
				for (net.enilink.vocab.owl.Class clazz : safe(ontClass
						.getOwlIntersectionOf())) {
					children.add(clazz);
				}
			}

			children.addAll(ontClass.getDirectNamedSubClasses().toList());

			if (showProperties) {
				for (Iterator<IProperty> it = ontClass
						.getDeclaredProperties(false); it.hasNext();) {
					Property property = (Property) it.next();
					children.add(property);
				}
			}
		}
		return children.toArray();
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof IClass) {
			IClass ontClass = (IClass) element;
			if (ontClass.hasNamedSubClasses(true)) {
				return true;
			}
			if (showProperties) {
				return ontClass.hasDeclaredProperties(false);
			}
		}
		return false;
	}

	@Override
	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (viewer instanceof TreeViewer) {
			this.viewer = (TreeViewer) viewer;
		} else {
			this.viewer = null;
		}
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return viewer != null;
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (OWL.PROPERTY_IMPORTS.equals(stmt.getPredicate())) {
			postRefresh(runnables);
			return false;
		} else if (RDFS.PROPERTY_SUBCLASSOF.equals(stmt.getPredicate())) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (OWL.PROPERTY_IMPORTS.equals(stmt.getPredicate())) {
			postRefresh(runnables);
			return false;
		} else if (RDFS.PROPERTY_SUBCLASSOF.equals(stmt.getPredicate())) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}
}
