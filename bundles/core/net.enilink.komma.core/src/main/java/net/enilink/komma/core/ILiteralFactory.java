package net.enilink.komma.core;

/**
 * Factory interface for creating {@link ILiteral} instances.
 *
 */
public interface ILiteralFactory {
	/**
	 * Creates a literal.
	 * 
	 * @param label The literal's label
	 * @param datatype The literal's datatype or <code>null</code>
	 * @param language The literal's language or <code>null</code>
	 * 
	 * @return A literal with the given label, data type and language
	 */
	ILiteral createLiteral(String label, URI datatype, String language);
}
