package net.enilink.komma.internal.sesame;

import org.openrdf.model.Literal;

import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class SesameLiteral implements ILiteral {
	protected URI datatype;
	protected Literal literal;

	public SesameLiteral(Literal literal) {
		this.literal = literal;
	}

	@Override
	public URI getDatatype() {
		if (datatype != null) {
			return datatype;
		}
		if (literal.getDatatype() != null) {
			return datatype = URIImpl.createURI(literal.getDatatype()
					.toString());
		}
		return null;
	}

	@Override
	public String getLabel() {
		return literal.getLabel();
	}

	@Override
	public String getLanguage() {
		return literal.getLanguage();
	}

	public Literal getSesameLiteral() {
		return literal;
	}

	@Override
	public Object getInstanceValue() {
		return getLabel();
	}

	@Override
	public String toString() {
		return literal.toString();
	}
}
