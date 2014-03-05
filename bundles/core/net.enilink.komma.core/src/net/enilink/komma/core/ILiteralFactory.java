package net.enilink.komma.core;

public interface ILiteralFactory {
	ILiteral createLiteral(String label, URI datatype, String language);
}
