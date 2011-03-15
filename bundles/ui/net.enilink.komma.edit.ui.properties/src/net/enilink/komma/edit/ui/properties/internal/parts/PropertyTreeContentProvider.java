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
package net.enilink.komma.edit.ui.properties.internal.parts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.provider.reflective.ModelContentProvider;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.util.ISparqlConstants;
import net.enilink.komma.util.Pair;

public class PropertyTreeContentProvider extends ModelContentProvider implements
		ITreeContentProvider {
	private boolean includeInferred = true;

	/**
	 * Current top-level object
	 */
	private IEntity input;

	/**
	 * Maps (subject, predicate) pairs to their corresponding property nodes
	 */
	@SuppressWarnings("unchecked")
	protected Map<Pair<IReference, IReference>, PropertyNode> subjectPredicateToNode = Collections
			.synchronizedMap(new ReferenceMap(ReferenceMap.HARD,
					ReferenceMap.WEAK, true));

	/**
	 * Handle additions or removals of statements by refreshing the
	 * corresponding property node or the whole viewer.
	 * 
	 * @param stmt
	 *            The statement which was added or removed.
	 * @param runnables
	 *            List of runnables that execute the viewer updates afterwards.
	 * @return <code>true</code> if other pending notifications should be
	 *         processed, else <code>false</code>
	 */
	protected boolean addedOrRemovedStatement(IStatement stmt,
			Collection<Runnable> runnables, boolean added) {
		Pair<IReference, IReference> subjectPredicate = new Pair<IReference, IReference>(
				stmt.getSubject(), stmt.getPredicate());
		PropertyNode existing = subjectPredicateToNode.get(subjectPredicate);
		if (existing != null) {
			if (!added) {
				postRefresh(runnables);
				return false;
			}
			existing.refreshChildren();
			postRefresh(Arrays.asList(existing), true, runnables);
		} else if (stmt.getSubject().equals(input)) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		return addedOrRemovedStatement(stmt, runnables, true);
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof PropertyNode) {
			return ((PropertyNode) parent).getChildren().toArray();
		}

		if (parent instanceof IStatement
				&& ((IStatement) parent).getObject() instanceof IResource) {
			return getElements(((IStatement) parent).getObject());
		}

		return new Object[0];
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IResource) {
			String SELECT_PROPERTIES = ISparqlConstants.PREFIX //
					+ "SELECT DISTINCT ?property " //
					+ "WHERE { " //
					+ "?resource ?property ?object" //
					+ "} ORDER BY ?property";

			IExtendedIterator<IProperty> result = ((IEntity) inputElement)
					.getEntityManager().createQuery(SELECT_PROPERTIES)
					.setParameter("resource", (IEntity) inputElement)
					.setIncludeInferred(includeInferred)
					.evaluate(IProperty.class);

			Map<Pair<IReference, IReference>, PropertyNode> nodes = new LinkedHashMap<Pair<IReference, IReference>, PropertyNode>();
			for (IProperty property : result) {
				Pair<IReference, IReference> subjectPredicate = new Pair<IReference, IReference>(
						((IEntity) inputElement).getReference(),
						property.getReference());
				PropertyNode node = subjectPredicateToNode
						.get(subjectPredicate);
				if (node == null) {
					node = new PropertyNode((IResource) inputElement, property,
							includeInferred);
					subjectPredicateToNode.put(subjectPredicate, node);
				} else {
					node.refreshChildren();
				}

				nodes.put(subjectPredicate, node);
			}
			subjectPredicateToNode.keySet().retainAll(nodes.keySet());

			return nodes.values().toArray();
		}
		return getChildren(inputElement);
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof PropertyNode
				&& !((PropertyNode) element).isIncomplete()
				|| (element instanceof IStatement && ((IStatement) element)
						.getObject() instanceof IResource);
	}

	@Override
	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		if (input == null || !input.equals(newInput)) {
			if (newInput instanceof IResource) {
				// reuse existing property nodes to preserve expanded state
				mapPropertyNodes(input, (IResource) newInput);

				input = (IEntity) newInput;
			} else {
				input = null;
				subjectPredicateToNode.clear();
			}
		}
	}

	/**
	 * Reuses existing property nodes for new input elements. This ensures that
	 * the expanded top-level nodes is preserved when input is changed.
	 * 
	 * @param oldInput
	 *            The old input element
	 * @param newInput
	 *            The new input element
	 */
	private void mapPropertyNodes(IEntity oldInput, IResource newInput) {
		List<PropertyNode> nodes2keep = new ArrayList<PropertyNode>();
		for (Iterator<PropertyNode> it = subjectPredicateToNode.values()
				.iterator(); it.hasNext();) {
			PropertyNode node = it.next();
			if (node.getResource().equals(oldInput)) {
				node.setResource(newInput);
				nodes2keep.add(node);
			}
		}
		subjectPredicateToNode.clear();
		for (PropertyNode node : nodes2keep) {
			Pair<IReference, IReference> subjectPredicate = new Pair<IReference, IReference>(
					node.getResource().getReference(), node.getProperty()
							.getReference());
			subjectPredicateToNode.put(subjectPredicate, node);
		}
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		return addedOrRemovedStatement(stmt, runnables, false);
	}

	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return viewer != null;
	}
}