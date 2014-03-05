package net.enilink.komma.core;

/**
 * Helper methods to work with {@link ILiteral} instances.
 * 
 */
public class Literals {
	/**
	 * Indicates whether two literals are "equal to".
	 * 
	 * @param literal
	 *            A literal instance
	 * @param other
	 *            Another literal with which to compare
	 * @return <code>true</code> if the literal is the same as the
	 *         <code>other</code> literal; <code>false</code> otherwise
	 */
	public static boolean equals(ILiteral literal, ILiteral other) {
		URI datatype = literal.getDatatype();
		URI otherDatatype = other.getDatatype();
		if (datatype == null) {
			if (otherDatatype != null) {
				return false;
			}
		} else if (!datatype.equals(otherDatatype))
			return false;
		String language = literal.getLanguage();
		String otherLanguage = other.getLanguage();
		if (language == null) {
			if (otherLanguage != null) {
				return false;
			}
		} else if (!language.equals(otherLanguage)) {
			return false;
		}
		return literal.getLabel().equals(other.getLabel());
	}

	/**
	 * Indicates whether some literal and some other object are "equal to".
	 * 
	 * @param literal
	 *            A literal instance
	 * @param obj
	 *            Another object with which to compare
	 * @return <code>true</code> if the literal is the same as the obj argument;
	 *         <code>false</code> otherwise
	 */
	public static boolean equals(ILiteral literal, Object obj) {
		if (literal == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof ILiteral)) {
			return false;
		}
		return equals(literal, (ILiteral) obj);
	}

	/**
	 * Compute the {@link Object#hashCode()} for a given literal.
	 * 
	 * @param literal
	 *            The literal
	 * @return The hash code of the given literal
	 */
	public static int hashCode(ILiteral literal) {
		final int prime = 31;
		int result = 1;
		URI datatype = literal.getDatatype();
		result = prime * result
				+ ((datatype == null) ? 0 : datatype.hashCode());
		String language = literal.getLanguage();
		result = prime * result
				+ ((language == null) ? 0 : language.hashCode());
		result = prime * result + literal.getLabel().hashCode();
		return result;
	}

	/**
	 * Build a Turtle-compatible string representation for a given literal.
	 * 
	 * @param literal
	 *            The literal
	 * @return A string representation for the given literal
	 */
	public static String toString(ILiteral literal) {
		StringBuilder result = new StringBuilder("\"").append(
				literal.getLabel()).append("\"");
		URI datatype = literal.getDatatype();
		String language = literal.getLanguage();
		if (datatype != null) {
			result.append("^^<").append(datatype.toString()).append(">");
		} else if (language != null) {
			result.append("@").append(language);
		}
		return result.toString();
	}

	private Literals() {
	}
}
