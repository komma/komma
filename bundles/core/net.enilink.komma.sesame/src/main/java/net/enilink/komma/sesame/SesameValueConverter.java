package net.enilink.komma.sesame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

import com.google.inject.Inject;

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

public class SesameValueConverter {
	private static final IRI[] EMPTY_URIS = {};
	private static final IRI[] NULL_URI = { null };

	protected ValueFactory valueFactory;
	protected final Map<String, BNode> bnodeMap = new HashMap<>();

	@Inject
	public SesameValueConverter(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public Dataset createDataset(IReference[] readContexts,
			IReference[] modifyContexts) {
		SimpleDataset ds = new SimpleDataset();
		for (IRI graph : toSesameURI(readContexts)) {
			ds.addDefaultGraph(graph);
			if (graph != null) {
				ds.addNamedGraph(graph);
			}
		}
		IRI[] sesameContexts = toSesameURI(modifyContexts);
		for (IRI graph : sesameContexts) {
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
		return valueFactory.createStatement((Resource) toSesame(next.getSubject()),
				toSesame(next.getPredicate().getURI()),
				toSesame((IValue) next.getObject()));
	}

	public Resource toSesame(IReference reference) {
		if (reference == null) {
			return null;
		}
		if (reference instanceof IReferenceable) {
			reference = ((IReferenceable) reference).getReference();
		}
		if (reference instanceof SesameReference) {
			Resource resource = ((SesameReference) reference)
					.getSesameResource();
			if (resource instanceof BNode
					&& ((BNode) resource).getID().startsWith("new-")) {
				// enforce that newly created Sesame blank nodes are also
				// correctly converted
				reference = new BlankNode(reference.toString());
			} else {
				return resource;
			}
		}
		URI uri = reference.getURI();
		if (uri != null) {
			return toSesame(reference.getURI());
		} else {
			String valueAsString = reference.toString();
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

	public Value toSesame(IValue value) {
		if (value == null) {
			return null;
		}
		if (value instanceof IReference) {
			return toSesame((IReference) value);
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
								(IRI) toSesame(literal
										.getDatatype()));
			}
		}
		throw new KommaException("Cannot convert object of type: "
				+ value.getClass().getName());
	}

	public IRI toSesame(URI uri) {
		if (uri == null) {
			return null;
		}
		// FIXME: check if simply replacing URI w/ IRI is applicable
		return valueFactory.createIRI(uri.toString());
	}

	public IRI[] toSesameURI(IReference... references) {
		if (references.length == 0) {
			return EMPTY_URIS;
		} else if (references.length == 1 && references[0] == null) {
			return NULL_URI;
		}
		List<IRI> uris = new ArrayList<IRI>(
				references.length);
		for (IReference ref : references) {
			if (ref == null) {
				uris.add(null);
			} else {
				Resource resource = toSesame(ref);
				if (resource instanceof IRI) {
					uris.add((IRI) resource);
				}
			}
		}
		return uris.toArray(new IRI[uris.size()]);
	}

	public void reset() {
		synchronized (bnodeMap) {
			bnodeMap.clear();
		}
	}
}
