package net.enilink.komma.core;

/**
 * Helper methods to work with {@link IStatement} and {@link IStatementPattern}
 * instances.
 * 
 */
public class Statements {
	private static boolean contextEquals(IStatementPattern stmt,
			IStatementPattern other) {
		if (stmt.getContext() == null) {
			return other.getContext() == null;
		} else {
			return stmt.getContext().equals(other.getContext());
		}
	}

	/**
	 * Compares a statement object to another statement object.
	 * 
	 * @param stmt
	 *            The reference statement or statement pattern
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link IStatementPattern} and if their subjects, predicates and
	 *         objects are equal.
	 */
	public static boolean equals(IStatementPattern stmt, IStatementPattern other) {
		return stmt == other || stmt != null && other != null
				&& spoEquals(stmt, other) && contextEquals(stmt, other);
	}

	/**
	 * Compares a statement object to another object.
	 * 
	 * @param stmt
	 *            The reference statement or statement pattern
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link Statement} and if their subjects, predicates, objects, and
	 *         contexts are equal.
	 */
	public static boolean equals(IStatementPattern stmt, Object other) {
		if (other instanceof IStatementPattern) {
			return equals(stmt, (IStatementPattern) other);
		}
		return false;
	}

	/**
	 * Compares a statement object to another statement object.
	 * 
	 * @param stmt
	 *            The reference statement or statement pattern
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link IStatementPattern} and if their subjects, predicates and
	 *         objects are equal.
	 */
	public static boolean equalsIgnoreContext(IStatementPattern stmt,
			IStatementPattern other) {
		return stmt == other || stmt != null && other != null
				&& spoEquals(stmt, other);
	}

	/**
	 * Compares a statement object to another object.
	 * 
	 * @param stmt
	 *            The reference statement or statement pattern
	 * @param other
	 *            The object to compare this statement to.
	 * @return <tt>true</tt> if the other object is an instance of
	 *         {@link IStatementPattern} and if their subjects, predicates and
	 *         objects are equal.
	 */
	public static boolean equalsIgnoreContext(IStatementPattern stmt,
			Object other) {
		if (other instanceof IStatementPattern) {
			return equalsIgnoreContext(stmt, (IStatementPattern) other);
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
	public static int hashCode(IStatementPattern stmt) {
		final int prime = 31;
		IReference ctx = stmt.getContext();
		int result = (ctx == null) ? 0 : ctx.hashCode();
		IReference subject = stmt.getSubject();
		if (subject != null) {
			result = prime * result + subject.hashCode();
		}
		IReference predicate = stmt.getPredicate();
		if (predicate != null) {
			result = prime * result + predicate.hashCode();
		}
		Object object = stmt.getObject();
		if (object != null) {
			result = prime * result + object.hashCode();
		}
		return result;
	}

	/**
	 * Matches a pattern or statement against another pattern.
	 * 
	 * @param pattern
	 *            The pattern used for matching.
	 * @return <tt>true</tt> if the pattern or statement matches the other
	 *         pattern.
	 */
	public static boolean matches(IStatementPattern stmt,
			IStatementPattern pattern) {
		if (!matchesIgnoreContext(stmt, pattern)) {
			return false;
		}
		if (stmt != null && pattern != null) {
			Object context = pattern.getContext();
			if (context != null && !context.equals(pattern.getContext())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Matches a pattern or statement against another pattern and ignores the
	 * context.
	 * 
	 * @param pattern
	 *            The pattern used for matching.
	 * @return <tt>true</tt> if the pattern or statement matches the other
	 *         pattern while ignoring the context.
	 */
	public static boolean matchesIgnoreContext(IStatementPattern stmt,
			IStatementPattern pattern) {
		if (stmt == pattern) {
			return true;
		}
		if (stmt == null || pattern == null) {
			return false;
		}
		Object o = pattern.getObject();
		if (o != null && !o.equals(pattern.getObject())) {
			return false;
		}
		Object s = pattern.getSubject();
		if (s != null && !s.equals(pattern.getSubject())) {
			return false;
		}
		Object p = pattern.getPredicate();
		if (p != null && !p.equals(pattern.getPredicate())) {
			return false;
		}
		return true;
	}

	private static boolean spoEquals(IStatementPattern stmt,
			IStatementPattern other) {
		// The object is potentially the cheapest to check so we start with
		// that. The number of different predicates in sets of statements is
		// commonly the smallest, so predicate equality is checked last.
		Object object = stmt.getObject();
		Object otherObject = other.getObject();
		if (object != otherObject
				&& (object == null || !object.equals(otherObject))) {
			return false;
		}

		Object subject = stmt.getSubject();
		Object otherSubject = other.getSubject();
		if (subject != otherSubject
				&& (subject == null || !subject.equals(otherSubject))) {
			return false;
		}

		Object predicate = stmt.getPredicate();
		Object otherPredicate = other.getPredicate();
		if (predicate != otherPredicate
				&& (predicate == null || !predicate.equals(otherPredicate))) {
			return false;
		}
		return true;
	}

	/**
	 * Gives a String-representation of this Statement that can be used for
	 * debugging.
	 */
	public static String toString(IStatementPattern stmt) {
		StringBuilder sb = new StringBuilder(256).append("(")
				.append(stmt.getSubject()).append(", ")
				.append(stmt.getPredicate()).append(", ")
				.append(stmt.getObject()).append(")");
		IReference ctx = stmt.getContext();
		if (ctx != null) {
			sb.append(" [").append(ctx).append("]");
		}
		return sb.toString();
	}

	private Statements() {
	}
}
