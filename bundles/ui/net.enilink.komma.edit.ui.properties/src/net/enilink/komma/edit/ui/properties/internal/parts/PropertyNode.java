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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.core.IStatement;

/**
 * Tree node that represents the values of a given property for a given resource
 * as its children.
 * 
 * @author Ken Wenzel
 */
public class PropertyNode {
	private IStatement firstStatement;
	private Boolean hasMultipleStatements;
	private boolean includeInferred;

	private IProperty property;
	private IResource resource;
	private List<IStatement> statements;

	public PropertyNode(IResource resource, IProperty property,
			boolean includeInferred) {
		this.resource = resource;
		this.property = property;
		this.includeInferred = includeInferred;
	}

	public Collection<IStatement> getChildren() {
		if (statements == null) {
			firstStatement = null;
			statements = getStatementIterator().toList();
		}
		return statements != null ? statements : Collections
				.<IStatement> emptyList();
	}

	public IStatement getFirstStatement() {
		if (firstStatement == null && statements == null) {
			IExtendedIterator<IStatement> stmtIt = getStatementIterator();
			firstStatement = stmtIt.next();
			hasMultipleStatements = stmtIt.hasNext();
			stmtIt.close();
		}

		return statements != null && statements.size() > 0 ? statements.get(0)
				: firstStatement;
	}

	public IProperty getProperty() {
		return property;
	}

	public IResource getResource() {
		return resource;
	}

	protected IExtendedIterator<IStatement> getStatementIterator() {
		return UniqueExtendedIterator.create(resource.getPropertyStatements(
				property, includeInferred));
	}

	public boolean hasChildren() {
		if (hasMultipleStatements == null) {
			getFirstStatement();
		}
		return hasMultipleStatements;
	}

	public void refreshChildren() {
		firstStatement = null;
		statements = null;
		hasMultipleStatements = null;
	}

	public void setResource(IResource resource) {
		this.resource = resource;
		refreshChildren();
	}
}
