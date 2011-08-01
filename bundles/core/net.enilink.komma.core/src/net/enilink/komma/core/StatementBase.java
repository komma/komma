/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.core;

public abstract class StatementBase implements IStatementPattern {

	private final boolean contextEquals(IStatementPattern other) {
		if (getContext() == null) {
			return other.getContext() == null;
		} else {
			return getContext().equals(other.getContext());
		}
	}

	/**
	 * Compares a statement object to another statement object.
	 * 
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link Statement} and if their subjects, predicates and objects
	 *         are equal.
	 */
	public final boolean equals(IStatementPattern other) {
		return this == other || spoEquals(other) && contextEquals(other);
	}

	/**
	 * Compares a statement object to another object.
	 * 
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link Statement} and if their subjects, predicates, objects, and
	 *         contexts are equal.
	 */
	@Override
	public final boolean equals(Object other) {
		if (other instanceof IStatementPattern) {
			return equals((IStatementPattern) other);
		}

		return false;
	}

	/**
	 * Compares a statement object to another statement object.
	 * 
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link Statement} and if their subjects, predicates and objects
	 *         are equal.
	 */
	public final boolean equalsIgnoreContext(IStatementPattern other) {
		return this == other || spoEquals(other);
	}

	/**
	 * Compares a statement object to another object.
	 * 
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link Statement} and if their subjects, predicates and objects
	 *         are equal.
	 */
	public final boolean equalsIgnoreContext(Object other) {
		if (other instanceof Statement) {
			return equalsIgnoreContext((Statement) other);
		}

		return false;
	}

	/**
	 * The hash code of a statement is defined as:
	 * 
	 * <tt>961 * subject.hashCode() + 31 * predicate.hashCode() + object.hashCode() + 29791 * context.hashCode()</tt>
	 * This is similar to how {@link String#hashCode String.hashCode()} is
	 * defined.
	 * 
	 * @return A hash code for the statement.
	 */
	@Override
	public final int hashCode() {
		final int prime = 31;
		IReference ctx = getContext();
		int result = (ctx == null) ? 0 : ctx.hashCode();
		IReference subject = getSubject();
		if (subject != null) {
			result = prime * result + subject.hashCode();
		}
		IReference predicate = getPredicate();
		if (predicate != null) {
			result = prime * result + predicate.hashCode();
		}
		Object object = getObject();
		if (object != null) {
			result = prime * result + object.hashCode();
		}
		return result;
	}

	@Override
	public boolean matches(IStatementPattern pattern) {
		if (!matchesIgnoreContext(pattern)) {
			return false;
		}

		Object pContext = pattern.getContext();
		if (pContext != null && !pContext.equals(getContext())) {
			return false;
		}

		return true;
	}

	@Override
	public boolean matchesIgnoreContext(IStatementPattern pattern) {
		Object pObject = pattern.getObject();
		if (pObject != null && !pObject.equals(getObject())) {
			return false;
		}

		Object pSubject = pattern.getSubject();
		if (pSubject != null && !pSubject.equals(getSubject())) {
			return false;
		}

		Object pPredicate = pattern.getPredicate();
		if (pPredicate != null && !pPredicate.equals(getPredicate())) {
			return false;
		}

		return true;
	}

	private final boolean spoEquals(IStatementPattern other) {
		// The object is potentially the cheapest to check so we start with
		// that. The number of different predicates in sets of statements is
		// commonly the smallest, so predicate equality is checked last.
		Object object = getObject();
		Object otherObject = other.getObject();
		if (object != otherObject || object != null
				&& !object.equals(otherObject)) {
			return false;
		}

		Object subject = getSubject();
		Object otherSubject = other.getSubject();
		if (subject != otherSubject || subject != null
				&& !subject.equals(otherSubject)) {
			return false;
		}

		Object predicate = getPredicate();
		Object otherPredicate = other.getPredicate();
		if (predicate != otherPredicate || predicate != null
				&& !predicate.equals(otherPredicate)) {
			return false;
		}

		return true;
	}

	/**
	 * Gives a String-representation of this Statement that can be used for
	 * debugging.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(256).append("(")
				.append(getSubject()).append(", ").append(getPredicate())
				.append(", ").append(getObject()).append(")");

		IReference ctx = getContext();
		if (ctx != null) {
			sb.append(" [").append(ctx).append("]");
		}

		return sb.toString();
	}
}
