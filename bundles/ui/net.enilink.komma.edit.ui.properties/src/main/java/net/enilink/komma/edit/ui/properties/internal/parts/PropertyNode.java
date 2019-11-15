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
package net.enilink.komma.edit.ui.properties.internal.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.vocab.rdf.RDF;

/**
 * Tree node that represents the values of a given property for a given resource
 * as its children.
 */
public class PropertyNode extends StatementNode {
	private static Options DEFAULT_OPTIONS = new Options();

	public static class Options {
		public boolean includeInferred;
	}

	private boolean createNewStatementOnEdit;
	private Boolean hasMultipleStatements;

	private Options options;

	private IStatement statement;

	private IProperty property;
	private IResource resource;
	private List<PropertyStatementNode> children;
	IStatement[] statements;

	public PropertyNode(IResource resource, IProperty property,
			boolean inverse, Options options) {
		super(inverse);
		this.resource = resource;
		this.property = property;
		this.options = options != null ? options : DEFAULT_OPTIONS;
	}

	/**
	 * Returns the list of all statements for the corresponding
	 * <code>resource</code> and <code>property</code>.
	 * 
	 * @return The list of all statements
	 */
	public Collection<? extends StatementNode> getChildren() {
		if (children == null) {
			statement = null;
			IStatement[] stmtAry = getStatementIterator().toList().toArray(
					new IStatement[0]);
			// reorder first two items in case the first is rdf:type=owl:Thing
			// and the second is not inferred
			// this way, the expanded view is consistent with the collapsed one
			if (stmtAry.length > 1
					&& RDF.PROPERTY_TYPE.equals(stmtAry[0].getPredicate())
					&& net.enilink.vocab.owl.OWL.TYPE_THING.equals(stmtAry[0]
							.getObject()) && !stmtAry[1].isInferred()) {
				IStatement typeThingStmt = stmtAry[0];
				stmtAry[0] = stmtAry[1];
				stmtAry[1] = typeThingStmt;
			}
			statements = stmtAry;
			children = new ArrayList<PropertyStatementNode>(statements.length);
			for (int i = 0; i < statements.length; i++) {
				children.add(new PropertyStatementNode(this, i, inverse));
			}
			hasMultipleStatements = children.size() > 1;
		}
		return children != null ? children : Collections
				.<StatementNode> emptyList();
	}

	/**
	 * Returns the first statement for the corresponding <code>resource</code>
	 * and <code>property</code>. This method does not initialize the list all
	 * statements if it was not loaded before.
	 * 
	 * @return The first statement
	 */
	public IStatement getStatement() {
		if (hasMultipleStatements == null) {
			if (children != null) {
				children = null;
				getChildren();
			} else {
				IExtendedIterator<IStatement> stmtIt = getStatementIterator();
				if (!stmtIt.hasNext()) {
					statement = inverse ? new Statement(null, property,
							resource) : new Statement(resource, property, null);
					hasMultipleStatements = false;
					return statement;
				}
				statement = stmtIt.next();
				hasMultipleStatements = stmtIt.hasNext();

				// skip first statement if it's rdf:type=owl:Thing and if there
				// are
				// more non-inferred statements
				// this way, the collapsed view does not show owl:Thing as the
				// type
				if (hasMultipleStatements
						&& RDF.PROPERTY_TYPE.equals(statement.getPredicate())
						&& net.enilink.vocab.owl.OWL.TYPE_THING
								.equals(statement.getObject())) {
					IStatement secondStatement = stmtIt.next();
					if (!secondStatement.isInferred()) {
						statement = secondStatement;
					}
				}
				stmtIt.close();
			}
		}

		return children != null && children.size() > 0 ? children.get(0)
				.getStatement() : statement;
	}

	public IProperty getProperty() {
		return property;
	}

	public IResource getResource() {
		return resource;
	}

	protected IExtendedIterator<IStatement> getStatementIterator() {
		if (property == null) {
			return WrappedIterator.<IStatement> create(Collections
					.<IStatement> emptyList().iterator());
		}
		return UniqueExtendedIterator.create(inverse ? resource
				.getInversePropertyStatements(property, true,
						options.includeInferred) : resource
				.getPropertyStatements(property, options.includeInferred));
	}

	/**
	 * Returns <code>true</code> if there are multiple statements for the
	 * corresponding <code>resource</code> and <code>property</code>, else
	 * <code>false</code>.
	 * 
	 * @return <code>true</code> if multiple statements exist, else
	 *         <code>false</code>
	 */
	public boolean hasMultipleStatements() {
		if (hasMultipleStatements == null) {
			getStatement();
		}
		return hasMultipleStatements;
	}

	public boolean isCreateNewStatementOnEdit() {
		return createNewStatementOnEdit;
	}

	public boolean isIncomplete() {
		return property == null
				|| (inverse && getStatement().getSubject() == null)
				|| (!inverse && getStatement().getObject() == null);
	}

	public boolean isInitialized() {
		return hasMultipleStatements != null;
	}

	/**
	 * Removes all cached statements of this property node.
	 */
	public void refreshChildren() {
		statement = null;
		if (children != null) {
			// required to mark statements as already loaded,
			// that next reload fetches all statements and not only the first
			// one
			children = Collections.emptyList();
		}
		hasMultipleStatements = null;
	}

	public void setCreateNewStatementOnEdit(boolean createNewStatementOnEdit) {
		this.createNewStatementOnEdit = createNewStatementOnEdit;
	}

	public void setProperty(IProperty property) {
		if (isIncomplete()) {
			this.property = property;
			this.statement = inverse ? new Statement(null, property, resource)
					: new Statement(resource, property, null);
		} else {
			throw new IllegalArgumentException(
					"Changing the property is only allowed for incomplete statements.");
		}
	}

	public void setResource(IResource resource) {
		this.resource = resource;
		refreshChildren();
	}
}
