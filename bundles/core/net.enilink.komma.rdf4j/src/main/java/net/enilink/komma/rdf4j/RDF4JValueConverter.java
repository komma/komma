package net.enilink.komma.rdf4j;

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
import net.enilink.komma.internal.rdf4j.RDF4JLiteral;
import net.enilink.komma.internal.rdf4j.RDF4JReference;

public class RDF4JValueConverter {
	private static final IRI[] EMPTY_IRIS = {};
	private static final IRI[] NULL_IRI = { null };

	protected ValueFactory valueFactory;
	protected final Map<String, BNode> bnodeMap = new HashMap<>();

	@Inject
	public RDF4JValueConverter(ValueFactory valueFactory) {
		this.valueFactory = valueFactory;
	}

	public Dataset createDataset(IReference[] readContexts,
			IReference[] modifyContexts) {
		SimpleDataset ds = new SimpleDataset();
		for (IRI graph : toRdf4jIRI(readContexts)) {
			ds.addDefaultGraph(graph);
			if (graph != null) {
				ds.addNamedGraph(graph);
			}
		}
		IRI[] rdf4jContexts = toRdf4jIRI(modifyContexts);
		for (IRI graph : rdf4jContexts) {
			ds.addDefaultRemoveGraph(graph);
		}
		if (rdf4jContexts.length > 0) {
			ds.setDefaultInsertGraph(rdf4jContexts[0]);
		}
		return ds;
	}

	public IReference fromRdf4j(Resource resource) {
		return (IReference) fromRdf4j((Value) resource);
	}

	public IValue fromRdf4j(Value value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Resource) {
			return new RDF4JReference((Resource) value);
		}
		return new RDF4JLiteral((Literal) value);
	}

	public BindingSet toRdf4j(IBindings<?> bindings) {
		MapBindingSet bindingSet = new MapBindingSet();
		for (String key : bindings.getKeys()) {
			bindingSet.addBinding(key, toRdf4j((IValue) bindings.get(key)));
		}
		return bindingSet;
	}

	public Statement toRdf4j(IStatement next) {
		return valueFactory.createStatement((Resource) toRdf4j(next.getSubject()),
				toRdf4j(next.getPredicate().getURI()),
				toRdf4j((IValue) next.getObject()),
				toRdf4j(next.getContext()));
	}

	public Resource toRdf4j(IReference reference) {
		if (reference == null) {
			return null;
		}
		if (reference instanceof IReferenceable) {
			reference = ((IReferenceable) reference).getReference();
		}
		if (reference instanceof RDF4JReference) {
			Resource resource = ((RDF4JReference) reference)
					.getRDF4JResource();
			if (resource instanceof BNode
					&& ((BNode) resource).getID().startsWith("new-")) {
				// enforce that newly created RDF4J blank nodes are also
				// correctly converted
				reference = new BlankNode(reference.toString());
			} else {
				return resource;
			}
		}
		URI uri = reference.getURI();
		if (uri != null) {
			return toRdf4j(reference.getURI());
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
							+ valueAsString + "' to RDF4J blank node.");
		}
	}

	public Value toRdf4j(IValue value) {
		if (value == null) {
			return null;
		}
		if (value instanceof IReference) {
			return toRdf4j((IReference) value);
		}
		if (value instanceof ILiteral) {
			if (value instanceof RDF4JLiteral) {
				return ((RDF4JLiteral) value).getRDF4JLiteral();
			}
			ILiteral literal = (ILiteral) value;
			String language = literal.getLanguage();
			if (language != null) {
				return valueFactory.createLiteral(literal.getLabel(), language);
			} else {
				return valueFactory
						.createLiteral(literal.getLabel(),
								(IRI) toRdf4j(literal
										.getDatatype()));
			}
		}
		throw new KommaException("Cannot convert object of type: "
				+ value.getClass().getName());
	}

	public IRI toRdf4j(URI uri) {
		if (uri == null) {
			return null;
		}
		// FIXME: check if simply replacing URI w/ IRI is appropriate
		return valueFactory.createIRI(uri.toString());
	}

	public IRI[] toRdf4jIRI(IReference... references) {
		if (references.length == 0) {
			return EMPTY_IRIS;
		} else if (references.length == 1 && references[0] == null) {
			return NULL_IRI;
		}
		List<IRI> iris = new ArrayList<IRI>(
				references.length);
		for (IReference ref : references) {
			if (ref == null) {
				iris.add(null);
			} else {
				Resource resource = toRdf4j(ref);
				if (resource instanceof IRI) {
					iris.add((IRI) resource);
				}
			}
		}
		return iris.toArray(new IRI[iris.size()]);
	}

	public void reset() {
		synchronized (bnodeMap) {
			bnodeMap.clear();
		}
	}
}
