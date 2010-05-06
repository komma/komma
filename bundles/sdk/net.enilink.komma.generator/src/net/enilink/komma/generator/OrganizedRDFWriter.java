/*
 * Copyright James Leigh (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.generator;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.model.BNode;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;

/**
 * Writes RDF statements grouped by subject.
 * 
 * @author James Leigh
 * 
 */
public class OrganizedRDFWriter implements RDFWriter {
	private static final String RDF_ = RDF.NAMESPACE + "_";
	private static final String SELECT_ALL_URI = "SELECT DISTINCT ?subj "
			+ "WHERE { ?subj a ?type . " + "FILTER ( isURI(?subj) ) } "
			+ "ORDER BY ?type ?subj";
	private static final String SELECT_ALL_BNODE = "SELECT DISTINCT ?subj "
			+ "WHERE { ?subj a ?type . " + "FILTER ( isBlank(?subj) ) } "
			+ "ORDER BY ?type ?subj";
	private static final String SELECT_FILTERED = "SELECT DISTINCT ?subj "
			+ "WHERE { ?subj ?pred ?obj } " + "ORDER BY ?subj";
	private static final QueryLanguage SPARQL = QueryLanguage.SPARQL;
	private static final String SUBJ = "subj";
	private static final String PRED = "pred";
	private static final String OBJ = "obj";
	private RepositoryConnection con;
	private Repository repository;
	private RDFWriter writer;
	private Set<Resource> covered = new HashSet<Resource>();
	private Set<String> namespaces = new HashSet<String>();
	private Set<URI> referenced = new LinkedHashSet<URI>();

	public OrganizedRDFWriter(RDFWriter writer) {
		this.writer = writer;
	}

	public void setConnection(RepositoryConnection con) {
		this.con = con;
	}

	public void startRDF() throws RDFHandlerException {
		writer.startRDF();
		try {
			if (con == null) {
				repository = new SailRepository(new MemoryStore());
				repository.initialize();
				con = repository.getConnection();
			} else {
				Result<? extends Namespace> nsIter = null;
				try {
					nsIter = con.getNamespaces();
					while (nsIter.hasNext()) {
						Namespace ns = nsIter.next();
						writer.handleNamespace(ns.getPrefix(), ns.getName());
					}
				} finally {
					if (nsIter != null) {
						nsIter.close();
					}
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RDFHandlerException(e);
		}
	}

	public void endRDF() throws RDFHandlerException {
		if (covered.isEmpty()) {
			print();
		}
		writer.endRDF();
		try {
			if (repository != null) {
				con.close();
				con = null;
				repository.shutDown();
				repository = null;
			}
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void close() throws IOException {
		if (writer instanceof Closeable) {
			((Closeable) writer).close();
		}
	}

	public RDFFormat getRDFFormat() {
		return writer.getRDFFormat();
	}

	public void handleComment(String comment) throws RDFHandlerException {
		writer.handleComment(comment);
	}

	public void handleNamespace(String prefix, String uri)
			throws RDFHandlerException {
		writer.handleNamespace(prefix, uri);
	}

	public void handleStatement(Statement st) throws RDFHandlerException {
		try {
			if (repository == null) {
				print(st);
			} else {
				con.add(st);
			}
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void print() throws RDFHandlerException {
		print(SPARQL, SELECT_ALL_URI, SUBJ);
		print(SPARQL, SELECT_ALL_BNODE, SUBJ);
	}

	public void print(QueryLanguage ql, String queryString, String binding)
			throws RDFHandlerException {
		try {
			TupleQuery query = con.prepareTupleQuery(ql, queryString);
			TupleResult result = query.evaluate();
			try {
				while (result.hasNext()) {
					Value subj = result.next().getValue(binding);
					if (!covered.contains(subj)) {
						print((Resource) subj);
					}
				}
			} finally {
				result.close();
			}
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void print(URI pred, Value obj) throws RDFHandlerException {
		try {
			TupleQuery query = con.prepareTupleQuery(SPARQL, SELECT_FILTERED);
			query.setBinding(PRED, pred);
			query.setBinding(OBJ, obj);
			TupleResult result = query.evaluate();
			try {
				while (result.hasNext()) {
					Value subj = result.next().getValue(SUBJ);
					if (!covered.contains(subj)) {
						print((Resource) subj);
					}
				}
			} finally {
				result.close();
			}
		} catch (MalformedQueryException e) {
			throw new AssertionError(e);
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void print(Resource subj) throws RDFHandlerException {
		Result<? extends Statement> stIter;
		try {
			stIter = con.match(subj, RDF.TYPE, null, false);
			try {
				while (stIter.hasNext()) {
					print(stIter.next());
				}
			} finally {
				stIter.close();
			}
			covered.add(subj);
			Set<URI> container = null;
			stIter = con.match(subj, null, null, false);
			try {
				while (stIter.hasNext()) {
					Statement next = stIter.next();
					URI pred = next.getPredicate();
					if (pred.toString().startsWith(RDF_)) {
						if (container == null) {
							container = printContainer(subj);
						}
						if (!container.contains(pred)) {
							print(next);
						}
					} else if (!pred.equals(RDF.TYPE)) {
						print(next);
					}
				}
			} finally {
				stIter.close();
			}
			if (subj instanceof URI) {
				namespaces.add(((URI) subj).getNamespace());
				if (writer instanceof Flushable) {
					((Flushable) writer).flush();
				}
			}
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	public void print(Statement st) throws RDFHandlerException {
		writer.handleStatement(st);
		Value obj = st.getObject();
		if (obj instanceof BNode && !covered.contains(obj)) {
			print((BNode) obj);
		} else if (obj instanceof URI && !covered.contains(obj)) {
			referenced.add((URI) obj);
		}
	}

	public void printReferenced() throws RDFHandlerException {
		referenced.removeAll(covered);
		List<URI> list = new ArrayList<URI>(referenced);
		referenced.clear();
		for (URI subj : list) {
			print(subj);
		}
		if (!referenced.isEmpty()) {
			printReferenced();
		}
	}

	private Set<URI> printContainer(Resource subj) throws RDFHandlerException {
		Result<? extends Statement> stIter;
		Set<URI> set = new HashSet<URI>();
		try {
			ValueFactory vf = con.getValueFactory();
			int idx = 1;
			URI pred = vf.createURI(RDF.NAMESPACE, "_" + idx++);
			while (con.hasMatch(subj, pred, null, false)) {
				stIter = con.match(subj, pred, null, false);
				try {
					if (stIter.hasNext()) {
						Statement st = stIter.next();
						print(vf.createStatement(st.getSubject(), RDF.LI, st
								.getObject()));
					}
					while (stIter.hasNext()) {
						print(stIter.next());
					}
				} finally {
					stIter.close();
				}
				set.add(pred);
				pred = vf.createURI(RDF.NAMESPACE, "_" + idx++);
			}
			return set;
		} catch (StoreException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void setBaseURI(String baseUri) {
		writer.setBaseURI(baseUri);
	}
}
