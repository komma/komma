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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.visitor.IDataAndNamespacesVisitor;
import net.enilink.komma.sesame.SesameModule;
import net.enilink.komma.sesame.SesameValueConverter;

/**
 * Writes RDF statements grouped by subject.
 * 
 */
public class OrganizedRDFWriter implements IDataAndNamespacesVisitor<Void> {
	private static final String OBJ = "obj";
	private static final String PRED = "pred";
	private static final String RDF_ = RDF.NAMESPACE + "_";
	private static final String SELECT_ALL_BNODE = "SELECT DISTINCT ?subj "
			+ "WHERE { ?subj a ?type . " + "FILTER ( isBlank(?subj) ) } "
			+ "ORDER BY ?type ?subj";
	private static final String SELECT_ALL_URI = "SELECT DISTINCT ?subj "
			+ "WHERE { ?subj a ?type . " + "FILTER ( isURI(?subj) ) } "
			+ "ORDER BY ?type ?subj";
	private static final String SELECT_FILTERED = "SELECT DISTINCT ?subj "
			+ "WHERE { ?subj ?pred ?obj } " + "ORDER BY ?subj";
	private static final String SUBJ = "subj";

	private Set<IReference> covered = new HashSet<IReference>();

	@Inject(optional = true)
	private IDataManager dm;

	private IDataManagerFactory dmFactory;

	private Set<URI> namespaces = new HashSet<URI>();

	private Set<URI> referenced = new LinkedHashSet<URI>();

	private SesameValueConverter valueConverter;

	private RDFWriter writer;

	public OrganizedRDFWriter(RDFWriter writer) {
		this.writer = writer;
	}

	public void close() throws IOException {
		if (writer instanceof Closeable) {
			((Closeable) writer).close();
		}
	}

	public void print() {
		print(SELECT_ALL_URI, SUBJ);
		print(SELECT_ALL_BNODE, SUBJ);
	}

	public void print(IReference subj) {
		IExtendedIterator<IStatement> stIter = dm.matchAsserted(subj,
				RDF.PROPERTY_TYPE, null);
		try {
			while (stIter.hasNext()) {
				print(stIter.next());
			}
		} finally {
			stIter.close();
		}
		covered.add(subj);
		Set<URI> container = null;
		stIter = dm.matchAsserted(subj, null, null);
		try {
			while (stIter.hasNext()) {
				IStatement next = stIter.next();
				URI pred = next.getPredicate().getURI();
				if (pred.toString().startsWith(RDF_)) {
					if (container == null) {
						container = printContainer(subj);
					}
					if (!container.contains(pred)) {
						print(next);
					}
				} else if (!pred.equals(RDF.PROPERTY_TYPE)) {
					print(next);
				}
			}
		} finally {
			stIter.close();
		}
		if (subj.getURI() != null) {
			namespaces.add(subj.getURI().namespace());
			if (writer instanceof Flushable) {
				try {
					((Flushable) writer).flush();
				} catch (IOException e) {
					throw new KommaException(e);
				}
			}
		}
	}

	public void print(IStatement st) {
		try {
			writer.handleStatement(valueConverter.toSesame(st));
		} catch (RDFHandlerException e) {
			throw new KommaException(e);
		}
		Object obj = st.getObject();
		if (obj instanceof IReference && !covered.contains(obj)) {
			if (((IReference) obj).getURI() == null) {
				print((IReference) obj);
			} else {
				referenced.add(((IReference) obj).getURI());
			}
		}
	}

	public void print(String queryString, String binding) {
		IDataManagerQuery<?> query = dm.createQuery(queryString, null);
		ITupleResult<?> result = (ITupleResult<?>) query.evaluate();
		try {
			while (result.hasNext()) {
				IValue subj = (IValue) ((IBindings<?>) result.next())
						.get(binding);
				if (!covered.contains(subj)) {
					print((IReference) subj);
				}
			}
		} finally {
			result.close();
		}
	}

	public void print(URI pred, IValue obj) {
		IDataManagerQuery<?> query = dm.createQuery(SELECT_FILTERED, null)
				.setParameter(PRED, pred).setParameter(OBJ, obj);
		ITupleResult<?> result = (ITupleResult<?>) query.evaluate();
		try {
			while (result.hasNext()) {
				IValue subj = ((IValue[]) result.next())[0];
				if (!covered.contains(subj)) {
					print((IReference) subj);
				}
			}
		} finally {
			result.close();
		}
	}

	private Set<URI> printContainer(IReference subj) {
		Set<URI> set = new HashSet<URI>();
		int idx = 1;
		URI pred = RDF.NAMESPACE_URI.appendFragment("_" + idx++);
		while (true) {
			IExtendedIterator<IStatement> stIter = dm.matchAsserted(subj, pred,
					null);
			try {
				if (stIter.hasNext()) {
					IStatement st = stIter.next();
					print(new Statement(st.getSubject(), RDF.PROPERTY_LI,
							st.getObject()));
				} else {
					break;
				}
				while (stIter.hasNext()) {
					print(stIter.next());
				}
			} finally {
				stIter.close();
			}
			set.add(pred);
			pred = RDF.NAMESPACE_URI.appendFragment("_" + idx++);
		}
		return set;
	}

	public void printReferenced() {
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

	// public void setBaseURI(String baseUri) {
	// writer.setBaseURI(baseUri);
	// }

	public Void visitBegin() {
		try {
			writer.startRDF();

			if (dm == null) {
				dmFactory = Guice.createInjector(new SesameModule(),
						new AbstractModule() {
							@Override
							protected void configure() {
								Repository repository = new SailRepository(
										new MemoryStore());
								try {
									repository.initialize();
								} catch (Exception e) {
									throw new KommaException(e);
								}
								bind(Repository.class).toInstance(repository);
							}
						}).getInstance(IDataManagerFactory.class);
				dm = dmFactory.get();
			} else {
				for (INamespace ns : dm.getNamespaces()) {
					writer.handleNamespace(ns.getPrefix(), ns.getURI()
							.toString());
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}

		return null;
	}

	public Void visitEnd() {
		try {
			if (covered.isEmpty()) {
				print();
			}
			writer.endRDF();

			if (dmFactory != null) {
				if (dm != null) {
					dm.close();
					dm = null;
				}

				dmFactory.close();
				dmFactory = null;
			}
		} catch (RDFHandlerException e) {
			throw new KommaException(e);
		}
		return null;
	}

	@Override
	public Void visitNamespace(INamespace namespace) {
		try {
			writer.handleNamespace(namespace.getPrefix(), namespace.getURI()
					.toString());
		} catch (RDFHandlerException e) {
			throw new KommaException(e);
		}
		return null;
	}

	@Override
	public Void visitStatement(IStatement st) {
		if (dm == null) {
			print(st);
		} else {
			dm.add(Arrays.asList(st));
		}
		return null;
	}
}
