package net.enilink.komma.internal.sesame;

import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.Literals;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

import org.openrdf.model.Literal;

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
			return datatype = URIs.createURI(literal.getDatatype()
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
	public boolean equals(Object obj) {
		return Literals.equals(this, obj);
	}

	@Override
	public int hashCode() {
		return Literals.hashCode(this);
	}

	@Override
	public String toString() {
		return Literals.toString(this);
	}
}