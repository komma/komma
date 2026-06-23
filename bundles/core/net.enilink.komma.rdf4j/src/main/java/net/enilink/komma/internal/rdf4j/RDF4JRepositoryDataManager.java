package net.enilink.komma.internal.rdf4j;

import java.util.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URIs;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.internal.rdf4j.result.RDF4JGraphResult;
import net.enilink.komma.internal.rdf4j.result.RDF4JResult;
import net.enilink.komma.rdf4j.RDF4JValueConverter;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;

public class RDF4JRepositoryDataManager implements IDataManager {
	class ChangeListener implements SailConnectionListener {
		@Override
		public void statementAdded(org.eclipse.rdf4j.model.Statement st, boolean inferred) {
			if (changeSupportEnabled) {
				changeSupport.add(RDF4JRepositoryDataManager.this,
						new Statement(valueConverter.fromRdf4j(st.getSubject()),
								valueConverter.fromRdf4j(st.getPredicate()),
								valueConverter.fromRdf4j(st.getObject()),
								valueConverter.fromRdf4j(st.getContext()),
								inferred));
			}
		}

		@Override
		public void statementRemoved(org.eclipse.rdf4j.model.Statement st, boolean inferred) {
			if (changeSupportEnabled) {
				changeSupport.remove(RDF4JRepositoryDataManager.this,
						new Statement(valueConverter.fromRdf4j(st.getSubject()),
								valueConverter.fromRdf4j(st.getPredicate()),
								valueConverter.fromRdf4j(st.getObject()),
								valueConverter.fromRdf4j(st.getContext()),
								inferred));
			}
		}

		@Override
		public void statementAdded(org.eclipse.rdf4j.model.Statement statement) {
			// ignore
		}

		@Override
		public void statementRemoved(org.eclipse.rdf4j.model.Statement statement) {
			// ignore
		}
	}

	protected static final IReference[] NULL_CTX = {null};

	protected IDataChangeSupport changeSupport;

	protected RepositoryConnection connection;

	@Inject(optional = true)
	protected volatile InferencingCapability inferencing;

	@Inject
	protected Injector injector;

	@Inject
	protected Repository repository;

	protected RDF4JTransaction transaction;

	@Inject
	protected RDF4JValueConverter valueConverter;

	protected SailConnectionListener sailConnectionListener;

	protected boolean changeSupportEnabled;

	@Inject
	public RDF4JRepositoryDataManager(Repository repository, IDataChangeSupport changeSupport) {
		try {
			connection = repository.getConnection();
		} catch (Exception e) {
			throw new KommaException(e);
		}
		this.changeSupport = changeSupport;
		this.transaction = new RDF4JTransaction(this, changeSupport);
		if (connection instanceof SailRepositoryConnection srConnection) {
			if (srConnection.getSailConnection() instanceof NotifyingSailConnection notifyingSailConnection) {
				sailConnectionListener = new ChangeListener();
				notifyingSailConnection.addConnectionListener(sailConnectionListener);
			}
		}
	}


	protected IReference[] addNullContext(boolean includeInferred, IReference[] contexts) {
		if (includeInferred && getInferencing().inDefaultGraph()) {
			for (IReference ctx : contexts) {
				if (ctx == null) {
					return contexts;
				}
			}
			contexts = Arrays.copyOf(contexts, contexts.length + 1);
			contexts[contexts.length - 1] = null;
		}
		return contexts;
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements, IReference[] readContexts,
	                        IReference... addContexts) {
		this.changeSupportEnabled = changeSupport.isEnabled(this);
		if (addContexts.length == 0) {
			addContexts = NULL_CTX;
		}
		IRI[] readCtx = valueConverter.toRdf4jIRI(readContexts);
		IRI[] addCtx = valueConverter.toRdf4jIRI(addContexts);
		try {
			Iterator<? extends IStatement> it = statements.iterator();

			RepositoryConnection conn = getConnection();
			boolean trackChanges = changeSupportEnabled && sailConnectionListener == null;
			boolean verifyChanges = changeSupport.getMode(this) == IDataChangeSupport.Mode.VERIFY_ALL;
			while (it.hasNext()) {
				IStatement stmt = it.next();

				Resource subject = valueConverter.toRdf4j(stmt.getSubject());
				IRI predicate = (IRI) valueConverter.toRdf4j(stmt.getPredicate());
				Value object = valueConverter.toRdf4j((IValue) stmt.getObject());

				if (trackChanges) {
					if (!verifyChanges || !conn.hasStatement(subject, predicate, object, false, readCtx)) {
						for (IReference ctx : addContexts) {
							changeSupport.add(this, new Statement(stmt.getSubject(), stmt
									.getPredicate(), stmt.getObject(),
									ctx, stmt.isInferred()));
						}
					}
				}
				if (!stmt.isInferred()) {
					conn.add(subject, predicate, object, addCtx);
				}
			}
			if (!getTransaction().isActive()) {
				clearNodeMappings();
				if (changeSupportEnabled) {
					changeSupport.commit(this);
				}
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager add(Iterable<? extends IStatement> statements, IReference... addContexts) {
		return add(statements, addContexts, addContexts);
	}

	@Override
	public IDataManager clearNamespaces() {
		try {
			if (changeSupport.isEnabled(this)) {
				for (INamespace namespace : getNamespaces()) {
					changeSupport.removeNamespace(this, namespace.getPrefix(), namespace.getURI());
				}
				changeSupport.commit(this);
			}

			getConnection().clearNamespaces();
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public void close() {
		if (connection == null) {
			return;
		}
		changeSupport.close(this);
		try {
			connection.close();
		} catch (Exception e) {
			throw new KommaException(e);
		} finally {
			connection = null;
		}
	}

	protected Query prepareRdf4jQuery(String query, String baseURI, boolean includeInferred)
			throws MalformedQueryException, RepositoryException {
		return getConnection().prepareQuery(QueryLanguage.SPARQL, query, baseURI);
	}

	protected String ensureBindingsInGraph(String query, IReference[] contexts) {
		if (contexts.length == 0) {
			return query;
		}
		boolean allNull = true;
		for (int i = 0; allNull && i < contexts.length; i++) {
			allNull = contexts[i] == null;
		}
		if (allNull) {
			return query;
		}
		Set<String> variables = new LinkedHashSet<String>();
		String delim = " ,)(;.{}";
		int i = 0;
		int qlen = query.length();
		int open = 0, closed = 0;
		while (i < qlen) {
			int ch = query.charAt(i++);
			if (ch == '{') {
				if (!variables.isEmpty()) {
					// only projection part of select queries is investigated
					break;
				}
				open++;
			} else if (ch == '}') {
				closed++;
			} else if (ch == '?' || ch == '$') { // variable
				int j = i;
				while (j < qlen && delim.indexOf(query.charAt(j)) < 0) {
					j++;
				}
				if (j != i) {
					String varName = query.substring(i - 1, j).trim();
					variables.add(varName);
				}
			}
			if (open > 0 && open == closed) {
				// only first group pattern is investigated
				// ASK { this one }, CONSTRUCT { this one } WHERE { ... }
				break;
			}
		}
		// ensure that at least one statement from the data set mentions a
		// selected variable
		int lastBrace = query.lastIndexOf('}');
		StringBuilder sb = new StringBuilder(query.substring(0, lastBrace));
		int n = 0, g = 0;
		if (!variables.isEmpty()) {
			sb.append("\nfilter (");
			for (Iterator<String> varIt = variables.iterator(); varIt.hasNext(); ) {
				String var = varIt.next();

				sb.append("\n(!bound(").append(var).append(") || isLiteral(")
						.append(var).append(") || exists { graph ?__g")
						.append(g++).append(" {\n");
				sb.append("\t{ ").append(var).append(" ?__p").append(n++)
						.append(" ?__o").append(n++).append(" } UNION ");
				sb.append("{ ?__s").append(n++).append(" ?__p").append(n++)
						.append(" ").append(var).append(" } UNION ");
				sb.append("{ ?__s").append(n++).append(" ").append(var)
						.append(" ?__o").append(n++).append(" }\n");
				sb.append("} })\n");

				if (varIt.hasNext()) {
					sb.append(" && ");
				}
			}
			sb.append(")");
		}
		sb.append(query.substring(lastBrace));
		return sb.toString();
	}

	protected void setDataset(Operation operation, IReference[] readContexts,
	                          IReference... modifyContexts) {
		if (readContexts.length > 0 || modifyContexts.length > 0) {
			operation.setDataset(valueConverter.createDataset(readContexts, modifyContexts));
		}
	}

	@Override
	public <R> IDataManagerQuery<R> createQuery(String query, String baseURI,
	                                            boolean includeInferred, IReference... contexts) {
		contexts = addNullContext(includeInferred, contexts);
		try {
			// query = ensureBindingsInGraph(query, contexts);
			Query rdf4jQuery = prepareRdf4jQuery(query, baseURI, includeInferred);
			setDataset(rdf4jQuery, contexts);
			rdf4jQuery.setIncludeInferred(includeInferred);

			RDF4JQuery<R> result = new RDF4JQuery<R>(rdf4jQuery);
			injector.injectMembers(result);
			return result;
		} catch (RepositoryException e) {
			throw new KommaException(e);
		} catch (MalformedQueryException e) {
			throw new KommaException("Invalid query format", e);
		}
	}

	@Override
	public IDataManagerUpdate createUpdate(final String update, String baseURI,
	                                       final boolean includeInferred, final IReference... contexts) {
		return createUpdate(update, baseURI, includeInferred, contexts,
				contexts);
	}

	@Override
	public IDataManagerUpdate createUpdate(String update, String baseURI,
	                                       boolean includeInferred, IReference[] readContexts,
	                                       IReference... modifyContexts) {
		readContexts = addNullContext(includeInferred, readContexts);
		if (changeSupport.isEnabled(this) && sailConnectionListener == null) {
			RDF4JUpdate result = new RDF4JUpdate(this, update, baseURI,
					includeInferred, readContexts, modifyContexts);
			injector.injectMembers(result);
			return result;
		} else {
			try {
				Update updateOp = getConnection().prepareUpdate(QueryLanguage.SPARQL, update);
				setDataset(updateOp, readContexts, modifyContexts);
				updateOp.setIncludeInferred(includeInferred);
				RDF4JUpdateNative result = new RDF4JUpdateNative(updateOp) {
					@Override
					public void execute() {
						boolean changeSupportEnabled = changeSupport.isEnabled(RDF4JRepositoryDataManager.this);
						RDF4JRepositoryDataManager.this.changeSupportEnabled = changeSupportEnabled;
						super.execute();
						if (changeSupportEnabled) {
							changeSupport.commit(RDF4JRepositoryDataManager.this);
						}
					}
				};
				injector.injectMembers(result);
				return result;
			} catch (Exception e) {
				throw new KommaException(e);
			}
		}

	}

	protected RepositoryConnection getConnection() {
		return connection;
	}

	@Override
	public InferencingCapability getInferencing() {
		if (inferencing == null) {
			inferencing = new InferencingCapability() {
				@Override
				public boolean doesOWL() {
					return false;
				}

				@Override
				public boolean doesRDFS() {
					// assume that RDFS is supported
					return true;
				}

				@Override
				public boolean inDefaultGraph() {
					return true;
				}
			};
		}
		return inferencing;
	}

	@Override
	public net.enilink.komma.core.URI getNamespace(String prefix) {
		try {
			String namespaceURI = getConnection().getNamespace(prefix);
			if (namespaceURI != null) {
				return net.enilink.komma.core.URIs.createURI(namespaceURI);
			}
			return null;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IExtendedIterator<INamespace> getNamespaces() {
		IExtendedIterator<INamespace> result = null;
		try {
			result = new RDF4JResult<>(getConnection().getNamespaces()) {
				@Override
				protected INamespace convert(Namespace element) {
					try {
						return new net.enilink.komma.core.Namespace(element.getPrefix(),
								URIs.createURI(element.getName()));
					} catch (IllegalArgumentException e) {
						return null;
					}
				}
			};
			// "resource leak" warning about result not being closed,
			// which is of course the intention here
			// TODO: change to streams and Java 8 filters
			return result.filterDrop(Objects::isNull);
		} catch (Exception e) {
			if (null != result) {
				result.close();
			}
			throw new KommaException(e);
		}
	}

	@Override
	public ITransaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean hasMatch(IReference subject, IReference predicate,
	                        IValue object, boolean includeInferred, IReference... contexts) {
		contexts = addNullContext(includeInferred, contexts);
		try {
			return getConnection().hasStatement(
					valueConverter.toRdf4j(subject),
					(IRI) valueConverter.toRdf4j(predicate),
					valueConverter.toRdf4j(object), includeInferred,
					valueConverter.toRdf4jIRI(contexts));
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public boolean isOpen() {
		return connection != null;
	}

	@Override
	public IExtendedIterator<IStatement> match(IReference subject, IReference predicate, IValue object,
	                                           boolean includeInferred, IReference... contexts) {
		contexts = addNullContext(includeInferred, contexts);
		try {
			RDF4JGraphResult result = new RDF4JGraphResult(getConnection()
					.getStatements(valueConverter.toRdf4j(subject),
							(IRI) valueConverter.toRdf4j(predicate),
							valueConverter.toRdf4j(object), includeInferred,
							valueConverter.toRdf4jIRI(contexts)));
			injector.injectMembers(result);
			return result;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IReference blankNode() {
		return new RDF4JReference(getConnection().getValueFactory()
				.createBNode());
	}

	@Override
	public IReference blankNode(String id) {
		if (id == null) {
			return blankNode();
		}
		if (id.startsWith("_:")) {
			id = id.substring(2);
		}
		return new RDF4JReference(getConnection().getValueFactory().createBNode(id));
	}

	@Override
	public IDataManager remove(
			Iterable<? extends IStatementPattern> statements,
			IReference... contexts) {
		this.changeSupportEnabled = changeSupport.isEnabled(this);
		if (contexts.length == 0) {
			contexts = NULL_CTX;
		}
		IRI[] removeContexts = valueConverter.toRdf4jIRI(contexts);
		try {
			RepositoryConnection conn = getConnection();
			boolean trackChanges = changeSupportEnabled && sailConnectionListener == null;
			IDataChangeSupport.Mode changeSupportMode = changeSupport.getMode(this);
			for (IStatementPattern stmt : statements) {
				if (stmt instanceof IStatement
						&& ((IStatement) stmt).isInferred()) {
					// special handling for inferred statements
					if (trackChanges) {
						for (IReference ctx : contexts) {
							changeSupport.remove(this, new Statement(stmt.getSubject(), stmt
									.getPredicate(), stmt.getObject(), ctx, true));
						}
					}
				} else {
					Resource subject = valueConverter.toRdf4j(stmt
							.getSubject());
					IRI predicate = (IRI) valueConverter.toRdf4j(stmt
							.getPredicate());
					Value object = valueConverter.toRdf4j((IValue) stmt
							.getObject());
					if (trackChanges) {
						Object stmtObject = stmt.getObject();
						if (changeSupportMode == IDataChangeSupport.Mode.VERIFY_ALL
								// ensure that wild cards (null values) are expanded
								|| changeSupportMode == IDataChangeSupport.Mode.EXPAND_WILDCARDS_ON_REMOVAL
								&& (stmt.getObject() == null || stmt.getPredicate() == null
								|| stmt.getSubject() == null)) {
							// retrieve existing statements from underlying repository
							for (IStatement existing : match(stmt.getSubject(),
									stmt.getPredicate(), (IValue) stmtObject,
									false, contexts)) {
								// pretend that statement was in changeCtx if no
								// context is set
								IReference[] changeContexts = existing.getContext() != null ?
										new IReference[]{existing.getContext()} : contexts;
								for (IReference ctx : changeContexts) {
									changeSupport.remove(this,
											new Statement(existing.getSubject(),
													existing.getPredicate(),
													existing.getObject(), ctx,
													existing.isInferred()));
								}
							}
						} else {
							// simply notify about removal even if some parts (subject, predicate or object) of
							// the statement pattern are null
							for (IReference ctx : contexts) {
								changeSupport.remove(this, new Statement(stmt.getSubject(), stmt
										.getPredicate(), stmt.getObject(), ctx, true));
							}
						}
					}
					conn.remove(subject, predicate, object, removeContexts);
				}
			}
			if (changeSupportEnabled && !getTransaction().isActive()) {
				changeSupport.commit(this);
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager removeNamespace(String prefix) {
		try {
			if (changeSupport.isEnabled(this)) {
				String namespace = getConnection().getNamespace(prefix);
				if (namespace != null) {
					changeSupport.removeNamespace(this, prefix, URIs.createURI(namespace));
				}
			}

			getConnection().removeNamespace(prefix);

			if (changeSupport.isEnabled(this)) {
				changeSupport.commit(this);
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}

	@Override
	public IDataManager setNamespace(String prefix, net.enilink.komma.core.URI uri) {
		try {
			if (changeSupport.isEnabled(this)) {
				net.enilink.komma.core.URI existing = getNamespace(prefix);
				changeSupport.setNamespace(this, prefix, existing, uri);
			}

			getConnection().setNamespace(prefix, uri.toString());

			if (changeSupport.isEnabled(this)) {
				changeSupport.commit(this);
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
		return this;
	}

	/**
	 * Clear cache of generated blank nodes.
	 */
	void clearNodeMappings() {
		valueConverter.reset();
	}
}
