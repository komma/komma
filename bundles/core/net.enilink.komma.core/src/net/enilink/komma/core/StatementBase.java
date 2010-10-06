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

public abstract class StatementBase implements IStatement {

	private final boolean contextEquals(IStatement other) {
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
	public final boolean equals(IStatement other) {
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
		if (other instanceof IStatement) {
			return equals((IStatement) other);
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
	public final boolean equalsIgnoreContext(IStatement other) {
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
		result = prime * result + getSubject().hashCode();
		result = prime * result + getPredicate().hashCode();
		result = prime * result + getObject().hashCode();
		return result;
	}

	private final boolean spoEquals(IStatement other) {
		// The object is potentially the cheapest to check so we start with
		// that. The number of different predicates in sets of statements is
		// commonly the smallest, so predicate equality is checked last.
		return getObject().equals(other.getObject())
				&& getSubject().equals(other.getSubject())
				&& getPredicate().equals(other.getPredicate());
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
