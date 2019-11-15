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

import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.concepts.IResource;

public class PropertyValuesContentProvider extends ModelContentProvider
		implements IStructuredContentProvider {
	private IReference property;
	private URI propertyURI;
	private IResource subject;
	private boolean useRawObjectsInStatements;
	private TableViewer viewer;

	public PropertyValuesContentProvider(final IReference property) {
		this.property = property;
	}

	public PropertyValuesContentProvider(final URI property) {
		this.propertyURI = property;
	}

	@Override
	protected boolean addedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (property.equals(stmt.getPredicate())
				&& stmt.getSubject().equals(subject)) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}

	public Object[] getElements(Object inputElement) {
		if (subject != null) {
			Collection<IStatement> stmts = useRawObjectsInStatements ? subject
					.getPropertyStatements(property, true).toList()
					: subject.getPropertyStatements(property, true).toList();

			return stmts.toArray();
		}
		return new Object[0];
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof Resource) {
			subject = (IResource) newInput;
		} else {
			subject = null;
		}
		if (subject != null && property == null) {
			property = subject.getEntityManager().find(propertyURI);
		}
		super.inputChanged(viewer, oldInput, newInput);
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

	public boolean isUseRawObjectsInStatements() {
		return useRawObjectsInStatements;
	}

	@Override
	protected boolean removedStatement(IStatement stmt,
			Collection<Runnable> runnables) {
		if (property.equals(stmt.getPredicate())
				&& subject.equals(stmt.getSubject())) {
			postRefresh(runnables);
			return false;
		}
		return true;
	}

	public void setUseRawObjectsInStatements(boolean useRawObjectsInStatements) {
		this.useRawObjectsInStatements = useRawObjectsInStatements;
		setTransformStatementObjects(!useRawObjectsInStatements);
	}

	@Override
	protected boolean shouldRegisterListener(Viewer viewer) {
		return subject != null && viewer != null;
	}
}