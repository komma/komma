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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
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

	private RDFFormat determineFormat(Map<?, ?> options) throws IOException {
		RDFFormat format = (RDFFormat) options.get(RDFFormat.class);
		if (format == null) {
			// format was not directly specified in options
			IURIConverter uriConverter = getModelSet().getURIConverter();
			IContentDescription contentDescription = (IContentDescription) options
					.get(IContentDescription.class);
			if (contentDescription == null) {
				IContentType contentType = (IContentType) options
						.get(IContentType.class);
				if (contentType == null) {
					String contentTypeId = (String) uriConverter
							.contentDescription(getURI(), options).get(
									IContentHandler.CONTENT_TYPE_PROPERTY);
					if (contentTypeId != null) {
						contentType = Platform.getContentTypeManager()
								.getContentType(contentTypeId);
						if (contentType != null) {
							contentDescription = contentType
									.getDefaultDescription();
						}
					}
				}
			}
			if (contentDescription != null) {
				// try to use the content description for determining the RDF
				// format
				String mimeType = (String) contentDescription
						.getProperty(new QualifiedName(ModelCore.PLUGIN_ID,
								"mimeType"));
				format = RDFFormat.forMIMEType(mimeType);
			}
			if (format == null) {
				// use file name with extension as fall back
				URI normalizedUri = uriConverter.normalize(getURI());
				// simply use the filename to detect the correct RDF format
				String lastSegment = normalizedUri.lastSegment();
				if (lastSegment != null) {
					format = RDFFormat.forFileName(lastSegment);
				}
			}
		}
		return format != null ? format : RDFFormat.RDFXML;
	}

	@Override
	public void load(final InputStream in, final Map<?, ?> options)
			throws IOException {
		final SesameValueConverter valueConverter = new SesameValueConverter(
				new ValueFactoryImpl());
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
				final RDFFormat[] format = { determineFormat(options) };
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						RDFParser parser = Rio.createParser(format[0]);
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

	@Override
	public void save(OutputStream os, Map<?, ?> options) throws IOException {
		RDFFormat format = (RDFFormat) determineFormat(options);
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
			final SesameValueConverter valueConverter = new SesameValueConverter(
					new ValueFactoryImpl());
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
