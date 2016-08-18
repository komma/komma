package net.enilink.komma.internal.rdf4j;

import org.eclipse.rdf4j.model.Literal;

import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.Literals;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

public class RDF4JLiteral implements ILiteral {
	protected URI datatype;
	protected Literal literal;

	public RDF4JLiteral(Literal literal) {
		this.literal = literal;
	}

	@Override
	public URI getDatatype() {
		if (datatype == null) {
			if (literal.getDatatype() != null) {
				datatype = URIs.createURI(literal.getDatatype().toString());
			} else {
				datatype = literal.getLanguage() == null ? net.enilink.komma.core.Literal.TYPE_STRING
						: net.enilink.komma.core.Literal.TYPE_LANGSTRING;
			}
		}
		return datatype;
	}

	@Override
	public String getLabel() {
		return literal.getLabel();
	}

	@Override
	public String getLanguage() {
		return literal.getLanguage().orElse(null);
	}

	public Literal getRDF4JLiteral() {
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
