/*******************************************************************************
 * Copyright (c) 2022 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.rdf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.core.BlankNode;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Namespace;
import net.enilink.komma.core.Statement;
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
import org.eclipse.rdf4j.rio.Rio;

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

	static class ReconstructNodeIds implements IMap<IStatement, IStatement> {
		final Map<String, IReference> bnodeMap = new HashMap<>();
		static final Pattern idPattern = Pattern.compile("^_:n([0-9a-z]{1,13})$");
		String prefix = BlankNode.generateId();
		long maxNodeId = 0;

		@Override
		public IStatement map(IStatement stmt) {
			return new Statement(convert(stmt.getSubject()),
					convert(stmt.getPredicate()),
					convert((IValue) stmt.getObject()));
		}

		@SuppressWarnings("unchecked")
		<V> V convert(V value) {
			if (value instanceof IReference
					&& ((IReference) value).getURI() == null) {
				String valueAsString = value.toString();
				Matcher m = idPattern.matcher(valueAsString);
				if (m.matches()) {
					String idStr = m.group(1);
					IReference bnode = bnodeMap.get(idStr);
					if (bnode == null) {
						long id = Long.parseLong(idStr, 36);
						maxNodeId = Math.max(maxNodeId, id);
						String newId = prefix + "__" + shortIdToSuffix(idStr);
						bnode = new BlankNode(newId);
						bnodeMap.put(idStr, bnode);
					}
					return (V) bnode;
				} else {
					return (V) new BlankNode("_:new-"
							+ valueAsString.substring(2));
				}
			}
			return value;
		}

		static String shortIdToSuffix(String shortId) {
			return ("0000000000000" + shortId).substring(shortId.length());
		}
	}

	@Override
	public void load(final InputStream in, final Map<?, ?> options)
			throws IOException {
		final List<INamespace> namespaces = new ArrayList<>();
		final IDataManager dm = ((IModelSet.Internal) getModelSet())
				.getDataManagerFactory().get();
		final ReconstructNodeIds nodeIdMapper = getModelSet().isPersistent() ? null
				: new ReconstructNodeIds();
		getModelSet().getDataChangeSupport().setEnabled(dm, false);
		try {
			setModelLoading(true);
			if (in != null && in.available() > 0) {
				dm.getTransaction().begin();

				final AtomicBoolean finished = new AtomicBoolean(false);
				final Queue<IStatement> queue = new LinkedList<IStatement>();
				final Throwable[] exception = { null };
				Executors.newSingleThreadExecutor().execute(new Runnable() {
					@Override
					public void run() {
						try {
							IDataAndNamespacesVisitor<Void> visitor = new IDataAndNamespacesVisitor<>() {
								@Override
								public Void visitStatement(IStatement stmt) {
									if (nodeIdMapper != null) {
										stmt = nodeIdMapper.map(stmt);
									}
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
							if (mimeType == null) {
								// determine mimeType from registered content types
								IContentDescription contentDescription = determineContentDescription(options);
								mimeType = ModelUtil.mimeType(contentDescription);
							}
							if (mimeType == null) {
								// determine mimeType from file extension
								mimeType = Optional.ofNullable(getURI().fileExtension())
									.flatMap(ext -> Rio.getParserFormatForFileName("test." + ext))
									.map(format -> format.getDefaultMIMEType())
									.orElse(null);
							}
							ModelUtil.readData(in, getURI().toString(), mimeType, nodeIdMapper != null, visitor);
						} catch (IOException e) {
							exception[0] = e;
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
				// update maximal node ID
				if (nodeIdMapper != null) {
					maxNodeId(Math.max(maxNodeId(), nodeIdMapper.maxNodeId));
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

	static class ShortenNodeIds implements IMap<IStatement, IStatement> {
		final Map<String, IReference> bnodeMap = new HashMap<>();
		final Map<String, IReference> usedShortIds = new HashMap<>();
		final Map<String, Integer> seenPrefixes = new HashMap<>();
		static final Pattern idPattern = Pattern.compile("^(.+)__0*([0-9a-z]+)$");
		long nextNodeId;

		ShortenNodeIds(long nextNodeId) {
			this.nextNodeId = nextNodeId;
		}

		@Override
		public IStatement map(IStatement stmt) {
			return new Statement(convert(stmt.getSubject()),
					convert(stmt.getPredicate()),
					convert((IValue) stmt.getObject()));
		}

		@SuppressWarnings("unchecked")
		<V> V convert(V value) {
			if (value instanceof IReference	&& ((IReference) value).getURI() == null) {
				String valueAsString = value.toString();
				if (valueAsString.startsWith("_:")) {
					String id = valueAsString.substring(2);
					IReference bnode = bnodeMap.get(id);
					if (bnode == null) {
						Matcher m = idPattern.matcher(id);
						String shortId;
						if (m.matches()) {
							String prefix = m.group(1);
							int prefixId = seenPrefixes.computeIfAbsent(prefix, key -> seenPrefixes.size() + 1);

							shortId = m.group(2);
							if (! usedShortIds.computeIfAbsent(shortId, key -> (IReference)value).equals(value)) {
								// another bnode exists that uses the same short ID
								// ensure that shortId is unique by appending prefixId
								shortId += "-" + prefixId;
							}
						} else {
							shortId = Long.toString(nextNodeId++, 36);
							if (! usedShortIds.computeIfAbsent(shortId, key -> (IReference)value).equals(value)) {
								// another bnode exists that uses the same short ID, this could happen if nextNodeId is not correctly initialized
								// ensure that shortId is unique by appending the fixed suffix "-0" as prefix IDs start with 1
								shortId += "-0";
							}
						}
						String newId = "_:n" + shortId;
						bnode = new BlankNode(newId);
						bnodeMap.put(id, bnode);
					}
					return (V) bnode;
				}
			}
			return value;
		}
	}

	@Override
	public void save(OutputStream os, Map<?, ?> options) throws IOException {
		String mimeType = (String) options.get(IModel.OPTION_MIME_TYPE);
		String charset = null;
		if (mimeType == null) {
			final IContentDescription contentDescription = determineContentDescription(options);
			if (contentDescription != null) {
				if (mimeType == null) {
					mimeType = ModelUtil.mimeType(contentDescription);
				}
				charset = contentDescription.getCharset();
			}
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
						|| !namespace.isDerived()
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
			// TODO determine expandDepth depending on the size of the data set
			int expandDepth = getModelSet().isPersistent() ? 0 : 6;
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

			ShortenNodeIds idMapper = new ShortenNodeIds(maxNodeId() + 1);
			// use sorting to improve readability of serialized output
			// store URI descriptions first
			String query = ISparqlConstants.PREFIX + "construct { ?s ?p ?o0 . "
					+ template
					+ "} where { " //
					+ "graph ?g { " //
					+ "{ ?s ?p ?o0 filter isIRI(?s) " + patterns
					+ " bind (1 as ?type) } union " //
					+ "{ ?s ?p ?o0 filter (isBlank(?s) " //
					+ filterExpanded + ")  bind (2 as ?type) }" //
					+ " }} order by ?type ?s ?p ?o0 " + projection;
			IExtendedIterator<IStatement> stmts = dm
					.<IStatement> createQuery(query,
							getURI().trimFragment().toString(), false, getURI())
					.setParameter("g", getURI()).evaluate().mapWith(idMapper);
			while (stmts.hasNext()) {
				dataVisitor.visitStatement(stmts.next());
			}
		} finally {
			dm.close();
		}
		dataVisitor.visitEnd();
	}

	@Iri(MODELS.NAMESPACE + "maxNodeId")
	public abstract long maxNodeId();

	public abstract void maxNodeId(long maxNodeId);
}
