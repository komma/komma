package net.enilink.komma.model.sesame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Namespace;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.visitor.IDataAndNamespacesVisitor;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.em.util.ISparqlConstants;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.concepts.Model;

import org.eclipse.core.runtime.content.IContentDescription;

@Iri(MODELS.NAMESPACE + "SerializableModel")
public abstract class SerializableModelSupport implements IModel.Internal,
		Model, Behaviour<IModel> {
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
		if (options == null) {
			options = Collections.emptyMap();
		}
		IContentDescription contentDescription = (IContentDescription) options
				.get(IModel.OPTION_CONTENT_DESCRIPTION);
		if (contentDescription == null) {
			contentDescription = ModelUtil.determineContentDescription(
					getURI(), getModelSet().getURIConverter(), options);
		}
		return contentDescription;
	}

	@Override
	public void load(final InputStream in, final Map<?, ?> options)
			throws IOException {
		final List<INamespace> namespaces = new ArrayList<>();
		final IDataManager dm = ((IModelSet.Internal) getModelSet())
				.getDataManagerFactory().get();
		getModelSet().getDataChangeSupport().setEnabled(dm, false);
		try {
			setModelLoading(true);
			if (in != null && in.available() > 0) {
				dm.getTransaction().begin();

				final AtomicBoolean finished = new AtomicBoolean(false);
				final Queue<IStatement> queue = new LinkedList<IStatement>();
				final IContentDescription[] contentDescription = { determineContentDescription(options) };
				final Throwable[] exception = { null };
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						try {
							IDataAndNamespacesVisitor<Void> visitor = new IDataAndNamespacesVisitor<Void>() {
								@Override
								public Void visitBegin() {
									return null;
								}

								@Override
								public Void visitEnd() {
									return null;
								}

								@Override
								public Void visitStatement(IStatement stmt) {
									synchronized (queue) {
										queue.add(stmt);
										queue.notify();
									}
									return null;
								}

								@Override
								public Void visitNamespace(INamespace namespace) {
									namespaces.add(namespace);
									return null;
								}
							};
							String mimeType = (String) options
									.get(IModel.OPTION_MIME_TYPE);
							if (mimeType == null
									&& contentDescription[0] != null) {
								mimeType = ModelUtil
										.mimeType(contentDescription[0]);
							}
							ModelUtil.readData(in, getURI().toString(),
									mimeType, visitor);
						} catch (RuntimeException e) {
							exception[0] = e;
						} finally {
							finished.set(true);
							synchronized (queue) {
								queue.notify();
							}
						}
					}
				});
				// BlockingIterator ensures that add method does not return
				// until endRDF of the above handler is called
				dm.add(new BlockingIterator<IStatement>(queue, finished),
						getURI());
				if (exception[0] != null) {
					throw exception[0];
				}
				dm.getTransaction().commit();

				// add namespaces as model meta-data
				for (INamespace ns : namespaces) {
					// prevent addition of redundant prefix/uri combinations
					if (ns.getPrefix().length() > 0
							&& !ns.getURI().equals(
									dm.getNamespace(ns.getPrefix()))) {
						net.enilink.komma.model.concepts.Namespace newNs = getEntityManager()
								.create(net.enilink.komma.model.concepts.Namespace.class);
						newNs.setPrefix(ns.getPrefix());
						newNs.setURI(ns.getURI());
						getModelNamespaces().add(newNs);
					}
				}
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
		String mimeType = (String) options.get(IModel.OPTION_MIME_TYPE);
		String charset = null;
		final IContentDescription contentDescription = determineContentDescription(options);
		if (contentDescription != null) {
			if (mimeType == null) {
				mimeType = ModelUtil.mimeType(contentDescription);
			}
			charset = contentDescription.getCharset();
		}

		IDataAndNamespacesVisitor<?> dataVisitor = ModelUtil.writeData(os,
				getURI().toString(), mimeType, charset);
		dataVisitor.visitBegin();

		final IDataManager dm = ((IModelSet.Internal) getModelSet())
				.getDataManagerFactory().get();
		try {
			// only include possibly used namespaces
			Set<URI> readableGraphs = getModuleClosure().getReadableGraphs();
			for (INamespace namespace : getManager().getNamespaces()) {
				if (KommaUtil.isW3cNamespace(namespace.getURI())
						|| readableGraphs.contains(namespace.getURI()
								.trimFragment())) {
					dataVisitor.visitNamespace(namespace);
				}
			}
			// use empty prefix for namespace of this model
			dataVisitor.visitNamespace(new Namespace("", getURI()
					.appendLocalPart("")));

			// expand blank nodes below IRIs up to expandDepth
			// TODO also expand blank nodes below other blank nodes
			int expandDepth = 10;
			StringBuilder template = new StringBuilder();
			StringBuilder projection = new StringBuilder();
			StringBuilder patterns = new StringBuilder();
			StringBuilder filterExpanded = new StringBuilder();
			if (expandDepth > 0) {
				filterExpanded.append("&& not exists {");
				int filterDepth = expandDepth;
				for (int i = 0; i < filterDepth; i++) {
					String prevS = "?someS" + (i - 1);
					if (i > 0) {
						filterExpanded.append("optional {");
					}
					filterExpanded.append("?someS" + i).append(" ?someP" + i);
					filterExpanded.append(" ").append(i == 0 ? "?s" : prevS)
							.append(" . ");
					if (i > 0) {
						filterExpanded.append("filter isBlank(").append(prevS)
								.append(") ");
					}
				}
				for (int i = 1; i < filterDepth; i++) {
					filterExpanded.append("}");
				}
				filterExpanded.append(" filter(");
				for (int i = 0; i < filterDepth; i++) {
					filterExpanded.append("isIRI(?someS" + i + ")");
					if (i < filterDepth - 1) {
						filterExpanded.append(" || ");
					}
				}
				filterExpanded.append(")");
				filterExpanded.append("}");

				for (int i = 0; i < expandDepth; i++) {
					String s = "?o" + i, p = "?p" + (i + 1), o = "?o" + (i + 1);
					template.append(s).append(" " + p + " ").append(o)
							.append(" . ");
					projection.append(p).append(" ").append(o).append(" ");
					patterns.append("optional {").append(s)
							.append(" " + p + " ").append(o);
					patterns.append(" filter isBlank(").append(s).append(") ");
				}
				for (int i = 0; i < expandDepth; i++) {
					patterns.append("}");
				}
			}

			// use sorting to improve readability of serialized output
			IExtendedIterator<IStatement> stmts = dm
					.<IStatement> createQuery(
							ISparqlConstants.PREFIX
									+ "construct { ?s ?p ?o0 . "
									+ template
									+ "} where { "
									+ "{ select distinct ?s ?p ?o0 "
									+ projection
									+ " where { graph ?g {?s ?p ?o0 filter isIRI(?s) "
									+ patterns
									+ "} } order by ?s ?p ?o0 "
									+ projection
									+ "} union " //
									+ "{ select distinct ?s ?p ?o0 "
									+ " where { graph ?g {?s ?p ?o0 filter (isBlank(?s) "
									+ filterExpanded
									+ ")} } order by ?s ?p ?o0 }" //
									+ " }",
							getURI().appendLocalPart("").toString(), false,
							getURI()).setParameter("g", getURI()).evaluate();
			while (stmts.hasNext()) {
				dataVisitor.visitStatement(stmts.next());
			}
		} finally {
			dm.close();
		}
		dataVisitor.visitEnd();
	}
}
