package net.enilink.komma.model.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.concepts.Model;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.visitor.IDataAndNamespacesVisitor;

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

	private IContentDescription determineContentDescription(Map<?, ?> options)
			throws IOException {
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
		if (contentDescription == null) {
			// use file name with extension as fall back
			URI normalizedUri = uriConverter.normalize(getURI());
			// simply use the filename to detect the correct RDF format
			String lastSegment = normalizedUri.fileExtension();
			if (lastSegment != null) {
				IContentType[] matchingTypes = Platform.getContentTypeManager()
						.findContentTypesFor(lastSegment);
				QualifiedName mimeType = new QualifiedName(ModelCore.PLUGIN_ID,
						"mimeType");
				for (IContentType contentType : matchingTypes) {
					IContentDescription desc = contentType
							.getDefaultDescription();
					if (desc.getProperty(mimeType) != null) {
						contentDescription = desc;
					}
				}
			}
		}
		return null;
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
				final IContentDescription[] contentDescription = { determineContentDescription(options) };
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						try {
							try {
								ModelUtil.readData(in, getURI().toString(),
										contentDescription[0],
										new IDataAndNamespacesVisitor<Void>() {
											@Override
											public Void visitBegin() {
												return null;
											}

											@Override
											public Void visitEnd() {
												return null;
											}

											@Override
											public Void visitStatement(
													IStatement stmt) {
												synchronized (queue) {
													queue.add(stmt);
													queue.notify();
												}
												return null;
											}

											@Override
											public Void visitNamespace(
													INamespace namespace) {
												dm.setNamespace(
														namespace.getPrefix(),
														namespace.getURI());
												return null;
											}
										});
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
		final IContentDescription contentDescription = determineContentDescription(options);
		IDataAndNamespacesVisitor<?> dataVisitor = ModelUtil.writeData(os,
				getURI().toString(), contentDescription);
		dataVisitor.visitBegin();

		final IDataManager dm = ((IModelSet.Internal) getModelSet())
				.getDataManagerFactory().get();
		dm.setReadContexts(Collections.singleton(getURI()));
		try {
			for (INamespace namespace : dm.getNamespaces()) {
				dataVisitor.visitNamespace(namespace);
			}

			IExtendedIterator<IStatement> stmts = dm.matchAsserted(null, null,
					null);
			while (stmts.hasNext()) {
				dataVisitor.visitStatement(stmts.next());
			}
		} finally {
			dm.close();
		}
		dataVisitor.visitEnd();
	}
}
