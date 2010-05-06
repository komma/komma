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
import java.util.Collection;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.edit.ui.provider.reflective.ModelContentProvider;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.util.ISparqlConstants;

public class PropertyTreeContentProvider extends ModelContentProvider implements
		ITreeContentProvider {
	private boolean useRawObjectsInStatements;

	private boolean includeInferred = true;

	public void setIncludeInferred(boolean includeInferred) {
		this.includeInferred = includeInferred;
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		/*
		 * if (property.equals(stmt.getPredicate()) &&
		 * stmt.getSubject().equals(subject)) { postRefresh(runnables); return
		 * false; } return true;
		 */
		return true;
	}

	public Object[] getElements(Object inputElement) {
		String SELECT_PROPERTIES = ISparqlConstants.PREFIX //
				+ "SELECT DISTINCT ?property " //
				+ "WHERE { " //
				+ "?resource ?property ?object" //
				+ "} ORDER BY ?property";

		IExtendedIterator<IProperty> result = ((IEntity) inputElement)
				.getKommaManager().createQuery(SELECT_PROPERTIES).setParameter(
						"resource", (IEntity) inputElement).setIncludeInferred(
						includeInferred).evaluate(IProperty.class);

		Collection<PropertyNode> nodes = new ArrayList<PropertyNode>();
		for (IProperty property : result) {
			IExtendedIterator<? extends IStatement> stmtIt = UniqueExtendedIterator
					.create(((IResource) inputElement).getPropertyStatements(
							property, includeInferred));

			// nur erstes holen
			if (stmtIt.hasNext()) {
				PropertyNode node = new PropertyNode(stmtIt.next(), stmtIt
						.hasNext());
				stmtIt.close();
				nodes.add(node);
			}
		}
		return nodes.toArray();
	}

	@Override
	protected void internalInputChanged(Viewer viewer, Object oldInput,
			Object newInput) {
		/*
		 * if (viewer instanceof TableViewer) { this.viewer = (TableViewer)
		 * viewer; } else { this.viewer = null; }
		 */
	}

	public boolean isUseRawObjectsInStatements() {
		return useRawObjectsInStatements;
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		/*
		 * if (property.equals(stmt.getPredicate()) &&
		 * subject.equals(stmt.getSubject())) { postRefresh(runnables); return
		 * false; }
		 */
		return true;
	}

	public void setUseRawObjectsInStatements(boolean useRawObjectsInStatements) {
		this.useRawObjectsInStatements = useRawObjectsInStatements;
		setTransformStatementObjects(!useRawObjectsInStatements);
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return false;
		// return viewer != null;
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof PropertyNode) {
			IExtendedIterator<?> stmtIt = UniqueExtendedIterator
					.create(((IResource) ((PropertyNode) parent).getStatement()
							.getSubject()).getPropertyStatements(
							((PropertyNode) parent).getStatement()
									.getPredicate(), includeInferred));

			return stmtIt.toList().toArray();
		}

		if (parent instanceof IStatement
				&& ((IStatement) parent).getObject() instanceof IResource) {
			return getElements(((IStatement) parent).getObject());
		}

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return element instanceof PropertyNode
				&& ((PropertyNode) element).hasChildren()
				|| (element instanceof IStatement && ((IStatement) element)
						.getObject() instanceof IResource);
	}
}