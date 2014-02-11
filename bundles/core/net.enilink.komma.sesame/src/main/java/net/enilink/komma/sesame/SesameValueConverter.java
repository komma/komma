package net.enilink.komma.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.enilink.komma.core.BlankNode;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IReferenceable;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.internal.sesame.SesameLiteral;
import net.enilink.komma.internal.sesame.SesameReference;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Dataset;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.impl.MapBindingSet;

import com.google.inject.Inject;

public class SesameValueConverter {
	private static final org.openrdf.model.URI[] EMPTY_URIS = new org.openrdf.model.URI[0];

	protected ValueFactory valueFactory;
	protected final Map<String, BNode> bnodeMap = new HashMap<>();

	@Inject
	public SesameValueConverter(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public Dataset createDataset(IReference[] readContexts,
			IReference[] modifyContexts) {
		DatasetImpl ds = new DatasetImpl();
		for (org.openrdf.model.URI graph : toSesameURI(readContexts)) {
			ds.addDefaultGraph(graph);
			if (graph != null) {
				ds.addNamedGraph(graph);
			}
		}
		org.openrdf.model.URI[] sesameContexts = toSesameURI(modifyContexts);
		for (org.openrdf.model.URI graph : sesameContexts) {
			ds.addDefaultRemoveGraph(graph);
		}
		if (sesameContexts.length > 0) {
			ds.setDefaultInsertGraph(sesameContexts[0]);
		}
		return ds;
	}

	public IReference fromSesame(Resource resource) {
		return (IReference) fromSesame((Value) resource);
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

	public BindingSet toSesame(IBindings<?> bindings) {
		MapBindingSet bindingSet = new MapBindingSet();
		for (String key : bindings.getKeys()) {
			bindingSet.addBinding(key, toSesame((IValue) bindings.get(key)));
		}
		return bindingSet;
	}

	public Statement toSesame(IStatement next) {
		return new StatementImpl((Resource) toSesame(next.getSubject()),
				toSesame(next.getPredicate().getURI()),
				toSesame((IValue) next.getObject()));
	}

	public Resource toSesame(IReference reference) {
		return (Resource) toSesame((IValue) reference);
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
				Resource resource = ((SesameReference) value)
						.getSesameResource();
				if (resource instanceof BNode
						&& ((BNode) resource).getID().startsWith("new-")) {
					// enforce that newly created Sesame blank nodes are also
					// correctly converted
					value = new BlankNode(value.toString());
				} else {
					return resource;
				}
			}
			URI uri = ((IReference) value).getURI();
			if (uri != null) {
				return toSesame(((IReference) value).getURI());
			} else {
				String valueAsString = ((IReference) value).toString();
				if (valueAsString.startsWith("_:")) {
					String id = valueAsString.substring(2);
					if (id.startsWith("new-")) {
						// convert newly created blank nodes with magic prefix
						// "new-"
						synchronized (bnodeMap) {
							BNode bnode = bnodeMap.get(id);
							if (bnode == null) {
								bnode = valueFactory.createBNode();
								bnodeMap.put(id, bnode);
							}
							return bnode;
						}
					}
					return valueFactory.createBNode(id);
				}
				throw new KommaException(
						"Cannot convert blank node with nominal value '"
								+ valueAsString + "' to Sesame blank node.");
			}
		}
		if (value instanceof ILiteral) {
			if (value instanceof SesameLiteral) {
				return ((SesameLiteral) value).getSesameLiteral();
			}
			ILiteral literal = (ILiteral) value;
			String language = literal.getLanguage();
			if (language != null) {
				return valueFactory.createLiteral(literal.getLabel(), language);
			} else {
				return valueFactory
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
		return valueFactory.createURI(uri.toString());
	}

	public org.openrdf.model.URI[] toSesameURI(IReference... references) {
		if (references.length == 0) {
			return EMPTY_URIS;
		}
		List<org.openrdf.model.URI> uris = new ArrayList<org.openrdf.model.URI>(
				references.length);
		for (IReference ref : references) {
			if (ref == null) {
				uris.add(null);
			} else {
				Resource resource = toSesame(ref);
				if (resource instanceof org.openrdf.model.URI) {
					uris.add((org.openrdf.model.URI) resource);
				}
			}
		}
		return uris.toArray(new org.openrdf.model.URI[uris.size()]);
	}

	public void reset() {
		synchronized (bnodeMap) {
			bnodeMap.clear();
		}
	}
}
