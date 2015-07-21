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
import net.enilink.komma.edit.ui.provider.reflective.ModelContentProvider;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;

public class PropertyTreeContentProvider extends ModelContentProvider implements
		ITreeContentProvider {
	private PropertyNode.Options options = new PropertyNode.Options();
	{
		options.includeInferred = true;
	}

	private boolean includeInverse = false;

	/**
	 * Current top-level object
	 */
	private IEntity input;

	static class Key {
		IReference resource;
		IReference predicate;
		boolean inverse;

		public Key(IReference resource, IReference predicate, boolean inverse) {
			this.resource = resource;
			this.predicate = predicate;
			this.inverse = inverse;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (inverse ? 1231 : 1237);
			result = prime * result
					+ ((predicate == null) ? 0 : predicate.hashCode());
			result = prime * result
					+ ((resource == null) ? 0 : resource.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (inverse != other.inverse)
				return false;
			if (predicate == null) {
				if (other.predicate != null)
					return false;
			} else if (!predicate.equals(other.predicate))
				return false;
			if (resource == null) {
				if (other.resource != null)
					return false;
			} else if (!resource.equals(other.resource))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "(" + resource + ", " + predicate + ", " + inverse + ")";
		}
	}

	/**
	 * Maps (subject, predicate) pairs to their corresponding property nodes
	 */
	@SuppressWarnings("unchecked")
	protected Map<Key, PropertyNode> resourcePredicateToNode = Collections
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
		PropertyNode existing = resourcePredicateToNode.get(new Key(stmt
				.getSubject(), stmt.getPredicate(), false));
		if (existing == null && stmt.getObject() instanceof IReference) {
			resourcePredicateToNode.get(new Key(stmt.getSubject(), stmt
					.getPredicate(), true));
		}
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

		if (parent instanceof StatementNode
				&& ((StatementNode) parent).getValue() instanceof IResource) {
			return getElements(((StatementNode) parent).getValue());
		}

		return new Object[0];
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof IResource) {
			String SELECT_PROPERTIES = ISparqlConstants.PREFIX //
					+ "SELECT DISTINCT ?property "
					+ (includeInverse ? "?invProperty " : "") //
					+ "WHERE { " //
					+ "{ ?resource ?property ?object }" //
					+ (includeInverse ? " UNION { ?subject ?invProperty ?resource FILTER (?subject != ?resource) }"
							: "")//
					+ "} ORDER BY ?property "
					+ (includeInverse ? "?invProperty" : "");

			IQuery<?> query = ((IEntity) inputElement).getEntityManager()
					.createQuery(SELECT_PROPERTIES, options.includeInferred)
					.setParameter("resource", (IEntity) inputElement);

			query.bindResultType("property", IProperty.class);
			query.bindResultType("invProperty", IProperty.class);

			@SuppressWarnings("rawtypes")
			IExtendedIterator<IBindings> result = (IExtendedIterator<IBindings>) query
					.evaluate(IBindings.class);
			Map<Key, PropertyNode> nodes = new LinkedHashMap<Key, PropertyNode>();
			List<IProperty> invProperties = new ArrayList<IProperty>();
			for (IBindings<?> bindings : result) {
				IProperty property = (IProperty) bindings.get("property");
				if (property != null) {
					createNode(nodes, (IResource) inputElement, property, false);
				}
				if (includeInverse) {
					IProperty invProperty = (IProperty) bindings
							.get("invProperty");
					if (invProperty != null) {
						invProperties.add(invProperty);
					}
				}
			}
			for (IProperty invProperty : invProperties) {
				createNode(nodes, (IResource) inputElement, invProperty, true);
			}
			return nodes.values().toArray();
		}
		return getChildren(inputElement);
	}

	protected PropertyNode createNode(Map<Key, PropertyNode> nodes,
			IResource resource, IProperty property, boolean inverse) {
		Key key = new Key(resource.getReference(), property.getReference(),
				inverse);
		PropertyNode node = resourcePredicateToNode.get(key);
		if (node == null) {
			node = new PropertyNode(resource, property, inverse, options);
			resourcePredicateToNode.put(key, node);
		} else {
			node.refreshChildren();
		}
		if (nodes != null) {
			nodes.put(key, node);
		}
		return node;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof PropertyNode
				&& !((PropertyNode) element).isIncomplete()
				|| (element instanceof StatementNode && ((StatementNode) element)
						.getStatement().getObject() instanceof IResource);
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
				resourcePredicateToNode.clear();
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
		for (Iterator<PropertyNode> it = resourcePredicateToNode.values()
				.iterator(); it.hasNext();) {
			PropertyNode node = it.next();
			if (node.getResource().equals(oldInput)) {
				node.setResource(newInput);
				nodes2keep.add(node);
			}
		}
		resourcePredicateToNode.clear();
		for (PropertyNode node : nodes2keep) {
			Key key = new Key(node.getResource().getReference(), node
					.getProperty().getReference(), node.isInverse());
			resourcePredicateToNode.put(key, node);
		}
	}

	/**
	 * Make <code>node</code> known to this content provider and add (subject,
	 * predicate) to <code>node</code> association.
	 */
	public void registerPropertyNode(PropertyNode node) {
		Key key = new Key(node.getResource().getReference(), node.getProperty()
				.getReference(), node.isInverse());
		if (!resourcePredicateToNode.containsKey(key)) {
			resourcePredicateToNode.put(key, node);
		}
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		return addedOrRemovedStatement(stmt, runnables, false);
	}

	public void setIncludeInferred(boolean includeInferred) {
		options.includeInferred = includeInferred;
	}

	public void setIncludeInverse(boolean includeInverse) {
		this.includeInverse = includeInverse;
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return viewer != null;
	}
}