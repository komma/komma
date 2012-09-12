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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.AbstractTableViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IIndexableLazyContentProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ILazyTreeContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.edit.provider.ISearchableItemProvider;
import net.enilink.komma.edit.provider.SparqlSearchableItemProvider;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.IValue;

public class StatementPatternContentProvider extends ModelContentProvider
		implements IStructuredContentProvider, ILazyContentProvider,
		IIndexableLazyContentProvider, ILazyTreeContentProvider,
		ISearchableItemProvider {
	protected Set<IStatementPattern> patterns = new HashSet<IStatementPattern>();

	protected Object[] instanceReferences;
	protected Map<Object, Integer> instanceToIndex;

	protected StructuredViewer viewer;
	protected boolean isVirtualViewer;

	protected IReference descendantProperty;

	public void setDescendantProperty(IReference descendantProperty) {
		this.descendantProperty = descendantProperty;
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		for (IStatementPattern pattern : patterns) {
			if (stmt.matchesIgnoreContext(pattern)) {
				postRefresh(runnables);
				return false;
			}
		}
		doUpdate(stmt, runnables);

		return true;
	}

	protected void doUpdate(IStatement stmt, Collection<Runnable> runnables) {
		if (instanceToIndex != null) {
			int index = findElement(stmt.getSubject());
			if (index >= 0) {
				postRefresh(Arrays.asList(stmt.getSubject()), true, runnables);
			}
			index = findElement(stmt.getPredicate());
			if (index >= 0) {
				postRefresh(Arrays.asList(stmt.getPredicate()), true, runnables);
			}
			index = findElement(stmt.getObject());
			if (index >= 0) {
				postRefresh(Arrays.asList(stmt.getObject()), true, runnables);
			}
		}
	}

	@Override
	public IExtendedIterator<?> find(Object expression, Object parent, int limit) {
		IExtendedIterator<Object> results = NiceIterator.emptyIterator();
		if (!patterns.isEmpty()) {
			SparqlSearchableItemProvider searchableProvider = new SparqlSearchableItemProvider() {
				@Override
				protected IEntityManager getEntityManager(Object parent) {
					return model.getManager();
				}

				@Override
				protected String getQueryFindPatterns(Object parent) {
					StringBuilder sb = getTriplePatterns(descendantProperty != null ? "?root"
							: "?s");
					if (descendantProperty != null) {
						sb.append(" ?root ?descendantProperty ?s .");
					}
					return sb.toString();
				}

				@Override
				protected void setQueryParameters(IQuery<?> query, Object parent) {
					setParameters(query);
				}
			};
			results = results.andThen(searchableProvider.find(expression, null,
					10));
		}
		return results;
	}

	protected StringBuilder getTriplePatterns(String subject) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (IStatementPattern pattern : patterns) {
			sb.append("{ ");
			if (pattern.getSubject() != null) {
				sb.append("?s").append(i);
			} else {
				sb.append(subject);
			}
			sb.append(' ');

			if (pattern.getPredicate() != null) {
				sb.append("?p").append(i);
			} else {
				sb.append(subject);
			}
			sb.append(' ');

			if (pattern.getObject() != null) {
				sb.append("?o").append(i);
			} else {
				sb.append(subject);
			}
			sb.append(" }");

			i++;

			if (i < patterns.size()) {
				sb.append(" UNION ");
			}
		}
		return sb;
	}

	protected void setParameters(IQuery<?> query) {
		int i = 0;
		for (IStatementPattern pattern : patterns) {
			if (pattern.getSubject() != null) {
				query.setParameter("s" + i, pattern.getSubject());
			}

			if (pattern.getPredicate() != null) {
				query.setParameter("p" + i, pattern.getPredicate());
			}

			if (pattern.getObject() != null) {
				query.setParameter("o" + i, pattern.getObject());
			}

			i++;
		}
		if (descendantProperty != null) {
			query.setParameter("descendantProperty", descendantProperty);
		}
	}

	protected IQuery<?> createQuery() {
		final IEntityManager em = model.getManager();
		StringBuilder querySb = new StringBuilder();
		querySb.append("SELECT ?s WHERE { ");
		querySb.append(getTriplePatterns("?s"));

		if (descendantProperty != null) {
			querySb.append(" FILTER NOT EXISTS { ");
			querySb.append(getTriplePatterns("?other"));
			querySb.append(" ?other ?descendantProperty ?s");
			querySb.append(" }");
		}

		querySb.append(" } ORDER BY ?s");
		IQuery<?> query = em.createQuery(querySb.toString());
		setParameters(query);
		return query;
	}

	public Object[] getElements(Object inputElement) {
		if (!patterns.isEmpty()) {
			return createQuery().evaluate().toList().toArray();
		}
		return new Object[0];
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		instanceToIndex = null;
		instanceReferences = null;
		patterns.clear();
		if (newInput instanceof IStatementPattern) {
			patterns.add((IStatementPattern) newInput);
		} else if (newInput != null && newInput.getClass().isArray()) {
			Object[] array = (Object[]) newInput;
			for (Object element : array) {
				patterns.add((IStatementPattern) element);
			}
		}

		if (viewer instanceof ColumnViewer) {
			this.viewer = (ColumnViewer) viewer;
		} else {
			this.viewer = null;
		}
		this.isVirtualViewer = (this.viewer.getControl().getStyle() & SWT.VIRTUAL) != 0;

		IModel newModel = getModelFromPatterns(patterns);
		super.inputChanged(viewer, this.model, newModel);

		// virtual table viewers do not ask for element count
		if (newInput != null && viewer instanceof AbstractTableViewer
				&& (viewer.getControl().getStyle() & SWT.VIRTUAL) != 0) {
			updateChildCount(newInput, -1);
		}
	}

	protected IModel getModelFromPatterns(Collection<IStatementPattern> patterns) {
		for (IStatementPattern pattern : patterns) {
			if (pattern.getContext() instanceof IModel) {
				return (IModel) pattern.getContext();
			}
			if (pattern.getSubject() instanceof IObject) {
				return ((IObject) pattern.getSubject()).getModel();
			}
			if (pattern.getObject() instanceof IObject) {
				return ((IObject) pattern.getObject()).getModel();
			}
			if (pattern.getPredicate() instanceof IObject) {
				return ((IObject) pattern.getPredicate()).getModel();
			}
		}
		return null;
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		// TODO
		// maybe broken if reasoner is used ...
		// listener gets not notified if inferred statements are removed
		for (IStatementPattern pattern : patterns) {
			if (stmt.matchesIgnoreContext(pattern)) {
				postRefresh(runnables);
				return false;
			}
		}
		doUpdate(stmt, runnables);
		return true;
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return this.viewer != null && !patterns.isEmpty();
	}

	protected Object resolve(Object value) {
		if (value instanceof IValue) {
			return model.getManager().toInstance((IValue) value);
		}
		return value;
	}

	@Override
	public void updateElement(int index) {
		if (instanceReferences != null && index < instanceReferences.length) {
			((AbstractTableViewer) viewer).replace(
					resolve(instanceReferences[index]), index);
		}
	}

	@Override
	public void updateElement(Object parent, int index) {
		Object element = resolve(instanceReferences[index]);
		((TreeViewer) viewer).replace(parent, index, element);
		updateChildCount(element, -1);
	}

	@Override
	public void updateChildCount(Object element, int currentChildCount) {
		Collection<Object> values = new LinkedHashSet<Object>();
		if (model != null && !patterns.isEmpty()) {
			values.addAll(createQuery().evaluateRestricted(IReference.class)
					.toList());
		}
		instanceReferences = values.toArray();
		instanceToIndex = new HashMap<Object, Integer>(
				instanceReferences.length);
		int i = 0;
		for (Object instance : instanceReferences) {
			instanceToIndex.put(instance, i++);
		}
		if (viewer instanceof TreeViewer) {
			((TreeViewer) viewer).setChildCount(element,
					instanceReferences.length);
		} else if (viewer instanceof AbstractTableViewer) {
			((AbstractTableViewer) viewer)
					.setItemCount(instanceReferences.length);
		}
	}

	@Override
	protected void postRefresh(Collection<Runnable> runnables) {
		// correctly refresh elements in case of virtual viewer
		if (isVirtualViewer) {
			// remove all previous update operations
			runnables.clear();
			runnables.add(new Runnable() {
				public void run() {
					updateChildCount(viewer.getInput(), -1);
					((StructuredViewer) viewer).refresh();

					// make sure no other update operations are executed after
					// this
					// one
					executedFullRefresh = true;
				}
			});
		} else {
			super.postRefresh(runnables);
		}
	}

	@Override
	public Object getParent(Object element) {
		// TODO find a reasonable pattern based implementation for this method
		return null;
	}

	@Override
	public int findElement(Object element) {
		if (instanceToIndex == null) {
			return -1;
		}
		Integer index = instanceToIndex.get(element);
		return index != null ? index : -1;
	}
}