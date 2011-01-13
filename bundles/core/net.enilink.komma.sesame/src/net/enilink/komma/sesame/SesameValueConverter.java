package net.enilink.komma.sesame;

import org.openrdf.model.Literal;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;

import com.google.inject.Inject;

import net.enilink.komma.internal.sesame.SesameLiteral;
import net.enilink.komma.internal.sesame.SesameReference;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;

public class SesameValueConverter {
	protected LiteralFactory literalFactory;

	protected URIFactory uriFactory;

	@Inject
	public SesameValueConverter(URIFactory uriFactory,
			LiteralFactory literalFactory) {
		this.uriFactory = uriFactory;
		this.literalFactory = literalFactory;
	}

	public IValue fromSesame(Value value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Resource) {
			return new SesameReference((Resource) value);
		}
		return new SesameLiteral((Literal) value);
	}

	public Statement toSesame(IStatement next) {
		return new StatementImpl((Resource) toSesame(next.getSubject()),
				toSesame(next.getPredicate().getURI()),
				toSesame((IValue) next.getObject()));
	}

	public Value toSesame(IValue value) {
		if (value == null) {
			return null;
		}
		if (value instanceof IReferenceable) {
			value = ((IReferenceable) value).getReference();
		}
		if (value instanceof IReference) {
			if (value instanceof SesameReference) {
				return ((SesameReference) value).getSesameResource();
			}
			URI uri = ((IReference) value).getURI();
			if (uri != null) {
				return toSesame(((IReference) value).getURI());
			} else {
				// TODO support conversion of blank nodes
				throw new KommaException(
						"Cannot convert foreign blank nodes to Sesame blank nodes.");
			}
		}
		if (value instanceof ILiteral) {
			if (value instanceof SesameLiteral) {
				return ((SesameLiteral) value).getSesameLiteral();
			}
			ILiteral literal = (ILiteral) value;
			String language = literal.getLanguage();
			if (language != null) {
				return literalFactory.createLiteral(literal.getLabel(),
						language);
			} else {
				return literalFactory
						.createLiteral(literal.getLabel(),
								(org.openrdf.model.URI) toSesame(literal
										.getDatatype()));
			}
		}
		throw new KommaException("Cannot convert object of type: "
				+ value.getClass().getName());
	}

	public org.openrdf.model.URI toSesame(URI uri) {
		if (uri == null) {
			return null;
		}
		return uriFactory.createURI(uri.toString());

	}
}
