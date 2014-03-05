package net.enilink.komma.core;

/**
 * Simple factory for in-memory literals.
 * 
 */
public class LiteralFactory implements ILiteralFactory {
	@Override
	public ILiteral createLiteral(String label,
			net.enilink.komma.core.URI datatype, String language) {
		if (datatype != null) {
			// let datatype take precedence if set, cannot set both
			return new Literal(label, datatype);
		} else {
			return new Literal(label, language);
		}
	}
}
