package net.enilink.komma.model.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.parameterTypes;
import net.enilink.composition.concepts.Message;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.SesameValueConverter;

@Iri(MODELS.NAMESPACE + "SerializableModel")
public abstract class SerializableModelSupport implements IModel, Model,
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

	private RDFFormat determineFormat(Map<?, ?> options) {
		IURIConverter uriConverter = getModelSet().getURIConverter();
		URI normalizedUri = uriConverter.normalize(getURI());

		RDFFormat format = null;

		// simply use the filename to detect the correct RDF format
		String lastSegment = normalizedUri.lastSegment();
		if (lastSegment != null) {
			format = RDFFormat.forFileName(lastSegment);
		}

		// TODO maybe also use the content description to determine the correct
		// RDF format
		// uriConverter.contentDescription(normalizedUri, options);

		return format != null ? format : RDFFormat.RDFXML;
	}

	@parameterTypes(Map.class)
	public void load(Message msg) {
		// determine the RDF format for the source URI and put it into the
		// options map
		Map<?, ?> options = (Map<?, ?>) msg.getParameters()[0];
		if (options == null) {
			options = Collections.emptyMap();
		}
		Map<Object, Object> newOptions = new HashMap<Object, Object>(options);
		newOptions.put(RDFFormat.class.getName(), determineFormat(options));
		msg.getParameters()[0] = newOptions;
		msg.proceed();
	}

	@Override
	public void load(final InputStream in, final Map<?, ?> options)
			throws IOException {
		final IDataManager dm = ((IModelSet.Internal) getModelSet())
				.getDataManagerFactory().get();
		getModelSet().getDataChangeSupport().setEnabled(dm, false);

		try {
			setModelLoading(true);

			if (in != null && in.available() > 0) {
				dm.setModifyContexts(Collections.singleton(getURI()));

				dm.getTransaction().begin();

				final AtomicBoolean finished = new AtomicBoolean(false);
				final Queue<IStatement> queue = new LinkedList<IStatement>();
				// new Job("Load model") {
				// @Override
				// public IStatus run(IProgressMonitor monitor) {
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						RDFFormat format = (RDFFormat) options
								.get(RDFFormat.class.getName());
						RDFParser parser = Rio
								.createParser(format != null ? format
										: determineFormat(options));
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
							}
						});
						try {
							try {
								unitOfWork.begin();
								parser.parse(in, getURI().toString());
							} catch (RDFParseException e) {
								throw new KommaException("Invalid RDF data", e);
							} catch (RDFHandlerException e) {
								throw new KommaException("Loading RDF failed",
										e);
							} catch (IOException e) {
								throw new KommaException(
										"Cannot access RDF data", e);
							} finally {
								finished.set(true);
								synchronized (queue) {
									queue.notify();
								}

								unitOfWork.end();
							}
						} catch (RuntimeException e) {
							throw e;
							// return new Status(IStatus.ERROR,
							// ModelCore.PLUGIN_ID,
							// "Error while loading model", exception);
						}
						// return Status.OK_STATUS;
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

	@parameterTypes(Map.class)
	public void save(Message msg) {
		// determine the RDF format for the source URI and put it into the
		// options map
		Map<?, ?> options = (Map<?, ?>) msg.getParameters()[0];
		if (options == null) {
			options = Collections.emptyMap();
		}
		Map<Object, Object> newOptions = new HashMap<Object, Object>(options);
		newOptions.put(RDFFormat.class.getName(), determineFormat(options));
		msg.getParameters()[0] = newOptions;
		msg.proceed();
	}

	@Override
	public void save(OutputStream os, Map<?, ?> options) throws IOException {
		RDFFormat format = (RDFFormat) options.get(RDFFormat.class.getName());
		if (format == null) {
			format = determineFormat(options);
		}
		RDFWriter writer;
		if (RDFFormat.RDFXML.equals(format)) {
			// use a special pretty writer in case of RDF/XML
			RDFXMLPrettyWriter rdfXmlWriter = new RDFXMLPrettyWriter(
					new OutputStreamWriter(os, "UTF-8"));
			rdfXmlWriter.setBaseURI(getURI().toString());
			writer = rdfXmlWriter;
		} else {
			RDFWriterFactory factory = RDFWriterRegistry.getInstance().get(
					format);
			writer = factory.getWriter(os);
		}

		try {
			final IDataManager dm = ((IModelSet.Internal) getModelSet())
					.getDataManagerFactory().get();
			dm.setReadContexts(Collections.singleton(getURI()));

			writer.startRDF();

			try {
				for (INamespace namespace : dm.getNamespaces()) {
					writer.handleNamespace(namespace.getPrefix(), namespace
							.getURI().toString());
				}

				IExtendedIterator<IStatement> stmts = dm.matchAsserted(null,
						null, null);
				while (stmts.hasNext()) {
					writer.handleStatement(valueConverter.toSesame(stmts.next()));
				}
			} finally {
				dm.close();
			}

			writer.endRDF();
		} catch (RDFHandlerException e) {
			throw new KommaException("Saving RDF failed", e);
		}
	}
}
