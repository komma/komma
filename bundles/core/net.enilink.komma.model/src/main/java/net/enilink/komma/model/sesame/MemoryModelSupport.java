package net.enilink.komma.model.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.SesameValueConverter;

@Iri(MODELS.NAMESPACE + "MemoryModel")
public abstract class MemoryModelSupport implements IModel, Model,
		Behaviour<IModel> {
	/**
	 * Iterator where <code>hasNext()</code> blocks until element gets available
	 * in <code>queue</code>.
	 */
	class BlockingIterator<T> implements Iterator<T>, Iterable<T> {
		Queue<T> queue;
		AtomicBoolean finished;

		T next;
		boolean nextComputed = false;

		public BlockingIterator(Queue<T> queue, AtomicBoolean finished) {
			this.queue = queue;
			this.finished = finished;
		}

		@Override
		public Iterator<T> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			if (!nextComputed) {
				synchronized (queue) {
					try {
						while (!finished.get() && queue.isEmpty()) {
							queue.wait();
						}
						next = queue.isEmpty() ? null : queue.remove();
					} catch (InterruptedException e) {
						next = null;
					}
				}
				nextComputed = true;
			}
			return next != null;
		}

		@Override
		public T next() {
			nextComputed = false;
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	@Inject
	SesameValueConverter valueConverter;

	@Inject
	IUnitOfWork unitOfWork;

	@Override
	public void load(final InputStream in, Map<?, ?> options)
			throws IOException {
		final IDataManager dm = ((IModelSet.Internal) getModelSet())
				.getDataManagerFactory().get();
		try {
			setModelLoading(true);

			if (in != null && in.available() > 0) {
				dm.setAddContexts(Collections.singleton(getURI()));

				dm.getTransaction().begin();

				final AtomicBoolean finished = new AtomicBoolean(false);
				final Queue<IStatement> queue = new LinkedList<IStatement>();
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						RDFParser parser = Rio.createParser(RDFFormat.RDFXML);
						parser.setRDFHandler(new RDFHandlerBase() {
							@Override
							public void handleStatement(Statement stmt)
									throws RDFHandlerException {
								synchronized (queue) {
									queue.add(new net.enilink.komma.core.Statement(
											(IReference) valueConverter
													.fromSesame(stmt
															.getSubject()),
											(IReference) valueConverter
													.fromSesame(stmt
															.getPredicate()),
											valueConverter.fromSesame(stmt
													.getObject())));
									queue.notify();
								}
							}

							@Override
							public void handleNamespace(String prefix,
									String uri) throws RDFHandlerException {
								dm.setNamespace(prefix, URIImpl.createURI(uri));
							}

							@Override
							public void endRDF() throws RDFHandlerException {
								finished.set(true);
								synchronized (queue) {
									queue.notify();
								}
							}
						});
						try {
							unitOfWork.begin();
							parser.parse(in, getURI().toString());
						} catch (RDFParseException e) {
							throw new KommaException("Invalid RDF data", e);
						} catch (RDFHandlerException e) {
							throw new KommaException("Loading RDF failed", e);
						} catch (IOException e) {
							throw new KommaException("Cannot access RDF data",
									e);
						} finally {
							unitOfWork.end();
						}
					}
				});

				// BlockingIterator ensures that add method does not return
				// until endRDF of the above handler is called
				dm.add(new BlockingIterator<IStatement>(queue, finished));
				dm.getTransaction().commit();
			}
		} catch (Throwable e) {
			if (e instanceof KommaException) {
				throw (KommaException) e;
			}
			throw new KommaException("Unable to load model", e);
		} finally {
			setModelLoading(false);
			setModified(false);

			if (dm.getTransaction().isActive()) {
				dm.getTransaction().rollback();
			}
			dm.close();
		}

		setModelLoaded(true);
	}

	@Override
	public void save(OutputStream os, Map<?, ?> options) throws IOException {
		RDFXMLWriter rdfWriter = new RDFXMLWriter(new OutputStreamWriter(os,
				"UTF-8"));
		try {
			org.openrdf.model.URI modelUri = URIUtil.toSesameUri(getURI());
			rdfWriter.setBaseURI(modelUri.toString());

			rdfWriter.startRDF();

			try {
				for (INamespace namespace : getManager().getNamespaces()) {
					rdfWriter.handleNamespace(namespace.getPrefix(), namespace
							.getURI().toString());
				}

				IExtendedIterator<IStatement> stmts = getManager()
						.createQuery("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}")
						.setIncludeInferred(false)
						.evaluateRestricted(IStatement.class);
				while (stmts.hasNext()) {
					rdfWriter.handleStatement(toSesame(stmts.next()));
				}
			} finally {
				// if (conn != null) {
				// try {
				// conn.close();
				// } catch (StoreException e) {
				// KommaCore.log(e);
				// }
				// }
			}

			rdfWriter.endRDF();
		} catch (RDFHandlerException e) {
			throw new KommaException("Saving RDF failed", e);
		}
	}

	private Statement toSesame(IStatement next) {
		return new StatementImpl((Resource) valueConverter.toSesame(next
				.getSubject()), valueConverter.toSesame(next.getPredicate()
				.getURI()), valueConverter.toSesame((IValue) next.getObject()));
	}
}