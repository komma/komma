package net.enilink.komma.core;

/**
 * Simple factory for in-memory literals.
 * 
 */
public class LiteralFactory implements ILiteralFactory {
	@Override
	public ILiteral createLiteral(String label, URI datatype, String language) {
		return new Literal(label, datatype, language);
	}
}
