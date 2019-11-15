/*******************************************************************************
 * Copyright (c) 2014 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.edit.refactor;

import java.util.Collection;

import net.enilink.komma.core.IStatement;
import net.enilink.komma.model.IModel;

public class Change {

	protected IModel model;
	protected Collection<StatementChange> statementChanges;

	public Change(IModel model, Collection<StatementChange> statementChanges) {
		this.model = model;
		this.statementChanges = statementChanges;
	}

	public IModel getModel() {
		return model;
	}

	public Collection<StatementChange> getStatementChanges() {
		return statementChanges;
	}

	public static class StatementChange {
		public static enum Type {
			ADD, REMOVE
		};

		IStatement statement;
		Type type;

		public StatementChange(IStatement statement, Type type) {
			this.statement = statement;
			this.type = type;
		}

		public IStatement getStatement() {
			return statement;
		}

		public Type getType() {
			return type;
		}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append(type == Type.ADD ? "ADD" : "REMOVE").append(" subj=")
					.append(statement.getSubject()).append(" pred=")
					.append(statement.getPredicate()).append(" obj=")
					.append(statement.getObject());
			return buf.toString();
		}
	};
}
