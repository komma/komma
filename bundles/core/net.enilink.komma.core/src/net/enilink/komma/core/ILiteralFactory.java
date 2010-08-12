package net.enilink.komma.core;


public interface ILiteralFactory {
	ILiteral createLiteral(String label, URI datatype, String language);

	ILiteral createLiteral(Object value, String label, URI datatype,
			String language);
}
