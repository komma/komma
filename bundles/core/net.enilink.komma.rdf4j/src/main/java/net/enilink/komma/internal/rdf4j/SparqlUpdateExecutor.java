/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package net.enilink.komma.internal.rdf4j;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.Add;
import org.eclipse.rdf4j.query.algebra.Clear;
import org.eclipse.rdf4j.query.algebra.Copy;
import org.eclipse.rdf4j.query.algebra.Create;
import org.eclipse.rdf4j.query.algebra.DeleteData;
import org.eclipse.rdf4j.query.algebra.InsertData;
import org.eclipse.rdf4j.query.algebra.Load;
import org.eclipse.rdf4j.query.algebra.Modify;
import org.eclipse.rdf4j.query.algebra.Move;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.queryrender.sparql.SparqlTupleExprRenderer;
import org.eclipse.rdf4j.repository.sail.helpers.SPARQLUpdateDataBlockParser;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IStatementPattern;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LinkedHashBindings;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.rdf4j.RDF4JValueConverter;

public class SparqlUpdateExecutor {
	private final Logger logger = LoggerFactory.getLogger(SparqlUpdateExecutor.class);

	private final IDataManager dm;

	private final RDF4JValueConverter valueConverter;

	private final ValueFactory vf;

	static final Collection<IStatementPattern> ANY_STATEMENT = Collections
			.<IStatementPattern> singleton(new net.enilink.komma.core.StatementPattern(null, null, null));

	static class UpdateContext {
		final UpdateExpr updateExpr;
		final String baseURI;
		final IReference[] readContexts;
		final IReference[] modifyContexts;
		final IBindings<IValue> bindings;
		final boolean includeInferred;

		UpdateContext(UpdateExpr updateExpr, String baseURI, IReference[] readContexts, IReference[] modifyContexts,
				IBindings<IValue> bindings, boolean includeInferred) {
			this.updateExpr = updateExpr;
			this.baseURI = baseURI;
			this.readContexts = readContexts;
			this.modifyContexts = modifyContexts;
			this.bindings = bindings;
			this.includeInferred = includeInferred;
		}
	}

	public SparqlUpdateExecutor(IDataManager dm, RDF4JValueConverter valueConverter, ValueFactory vf) {
		this.dm = dm;
		this.valueConverter = valueConverter;
		this.vf = vf;
	}

	public void executeUpdate(UpdateExpr updateExpr, String baseURI, IReference[] readContexts,
			IReference[] modifyContexts, IBindings<IValue> bindings, boolean includeInferred) throws KommaException {
		UpdateContext uc = new UpdateContext(updateExpr, baseURI, readContexts, modifyContexts, bindings,
				includeInferred);
		logger.trace("Incoming update expression:\n{}", uc);

		if (updateExpr instanceof Load) {
			executeLoad((Load) updateExpr, uc);
		} else if (updateExpr instanceof Modify) {
			executeModify((Modify) updateExpr, uc);
		} else if (updateExpr instanceof InsertData) {
			executeInsertData((InsertData) updateExpr, uc);
		} else if (updateExpr instanceof DeleteData) {
			executeDeleteData((DeleteData) updateExpr, uc);
		} else if (updateExpr instanceof Clear) {
			executeClear((Clear) updateExpr, uc);
		} else if (updateExpr instanceof Create) {
			executeCreate((Create) updateExpr, uc);
		} else if (updateExpr instanceof Copy) {
			executeCopy((Copy) updateExpr, uc);
		} else if (updateExpr instanceof Add) {
			executeAdd((Add) updateExpr, uc);
		} else if (updateExpr instanceof Move) {
			executeMove((Move) updateExpr, uc);
		}
	}

	protected void executeLoad(Load load, UpdateContext uc) throws KommaException {
		Value source = load.getSource().getValue();
		Value graph = load.getGraph() != null ? load.getGraph().getValue() : null;
		// TODO load source into graph
	}

	protected void executeCreate(Create create, UpdateContext uc) throws KommaException {
		// check if named graph exists, if so, we have to return an error.
		// Otherwise, we simply do nothing.
		Value graphValue = create.getGraph().getValue();
		if (graphValue instanceof Resource) {
			Resource namedGraph = (Resource) graphValue;
			IDataManagerQuery<?> contextQuery = dm.createQuery("ask { graph ?g { ?s ?p ?o } }", null, false);
			try (IExtendedIterator<?> result = contextQuery.evaluate()) {
				if (Boolean.TRUE.equals(result.next())) {
					throw new KommaException("Named graph " + namedGraph + " already exists. ");
				}
			}
		}
	}

	protected void executeCopy(Copy copy, UpdateContext uc) throws KommaException {
		ValueConstant sourceGraph = copy.getSourceGraph();
		ValueConstant destinationGraph = copy.getDestinationGraph();

		Resource source = sourceGraph != null ? (Resource) sourceGraph.getValue() : null;
		Resource destination = destinationGraph != null ? (Resource) destinationGraph.getValue() : null;

		if (source == null && destination == null || (source != null && source.equals(destination))) {
			// source and destination are the same, copy is a null-operation.
			return;
		}

		// clear destination
		dm.remove(ANY_STATEMENT, valueConverter.fromRdf4j(destination));

		// get all statements from source and add them to destination
		try (IExtendedIterator<IStatement> stmts = dm.match(null, null, null, uc.includeInferred,
				valueConverter.fromRdf4j(source))) {
			dm.add(stmts, valueConverter.fromRdf4j(destination));
		}
	}

	protected void executeAdd(Add add, UpdateContext uc) throws KommaException {
		ValueConstant sourceGraph = add.getSourceGraph();
		ValueConstant destinationGraph = add.getDestinationGraph();

		Resource source = sourceGraph != null ? (Resource) sourceGraph.getValue() : null;
		Resource destination = destinationGraph != null ? (Resource) destinationGraph.getValue() : null;

		if (source == null && destination == null || (source != null && source.equals(destination))) {
			// source and destination are the same, copy is a null-operation.
			return;
		}

		// get all statements from source and add them to destination
		try (IExtendedIterator<IStatement> stmts = dm.match(null, null, null, uc.includeInferred,
				valueConverter.fromRdf4j(source))) {
			dm.add(stmts, valueConverter.fromRdf4j(destination));
		}
	}

	protected void executeMove(Move move, UpdateContext uc) throws KommaException {
		ValueConstant sourceGraph = move.getSourceGraph();
		ValueConstant destinationGraph = move.getDestinationGraph();

		Resource source = sourceGraph != null ? (Resource) sourceGraph.getValue() : null;
		Resource destination = destinationGraph != null ? (Resource) destinationGraph.getValue() : null;

		if (source == null && destination == null || (source != null && source.equals(destination))) {
			// source and destination are the same, move is a null-operation.
			return;
		}

		// clear destination
		dm.remove(ANY_STATEMENT, valueConverter.fromRdf4j(destination));

		// remove all statements from source and add them to destination
		try (IExtendedIterator<IStatement> stmts = dm.match(null, null, null, uc.includeInferred,
				valueConverter.fromRdf4j(source))) {
			dm.add(stmts, valueConverter.fromRdf4j(destination));
			dm.remove(stmts, valueConverter.fromRdf4j(source));
		}
	}

	protected void executeClear(Clear clearExpr, UpdateContext uc) throws KommaException {
		try {
			ValueConstant graph = clearExpr.getGraph();
			if (graph != null) {
				Resource context = (Resource) graph.getValue();
				dm.remove(ANY_STATEMENT, valueConverter.fromRdf4j(context));
			} else {
				Scope scope = clearExpr.getScope();
				if (Scope.NAMED_CONTEXTS.equals(scope)) {
					IDataManagerQuery<?> contextQuery = dm
							.createQuery("select distinct ?g where { graph ?g { ?s ?p ?o } }", null, false);
					for (IReference ctx : contextQuery.evaluate().mapWith(new IMap<Object, IReference>() {
						@Override
						public IReference map(Object value) {
							return valueConverter.fromRdf4j((Resource) ((IBindings<?>) value).get("g"));
						}
					}).toList()) {
						dm.remove(ANY_STATEMENT, ctx);
					}
				} else if (Scope.DEFAULT_CONTEXTS.equals(scope)) {
					dm.remove(ANY_STATEMENT, (IReference) null);
				} else {
					dm.remove(ANY_STATEMENT);
				}
			}
		} catch (KommaException e) {
			if (!clearExpr.isSilent()) {
				throw e;
			}
		}
	}

	protected List<IStatement> dataBlockToStatements(String dataBlock, boolean allowBNodes, UpdateContext uc)
			throws KommaException {
		final List<IStatement> stmts = new ArrayList<>();
		SPARQLUpdateDataBlockParser parser = new SPARQLUpdateDataBlockParser(vf);
		// blank nodes are OK w/ INSERT DATA but not allowed w/ DELETE DATA
		parser.setAllowBlankNodes(allowBNodes);
		parser.setRDFHandler(new AbstractRDFHandler() {
			@Override
			public void handleStatement(Statement stmt) throws RDFHandlerException {
				stmts.add(
						new net.enilink.komma.core.Statement((IReference) valueConverter.fromRdf4j(stmt.getSubject()),
								(IReference) valueConverter.fromRdf4j(stmt.getPredicate()),
								valueConverter.fromRdf4j(stmt.getObject()),
								(IReference) valueConverter.fromRdf4j(stmt.getContext())));
			}
		});
		try {
			parser.parse(new StringReader(dataBlock), uc.baseURI);
		} catch (RDFParseException rpe) {
			throw new KommaException(rpe);
		} catch (RDFHandlerException rhe) {
			throw new KommaException(rhe);
		} catch (IOException ioe) {
			throw new KommaException(ioe);
		}
		return stmts;
	}

	protected void executeInsertData(InsertData insertDataExpr, UpdateContext uc) throws KommaException {
		dm.add(dataBlockToStatements(insertDataExpr.getDataBlock(), true, uc), uc.modifyContexts);
	}

	protected void executeDeleteData(DeleteData deleteDataExpr, UpdateContext uc) throws KommaException {
		dm.remove(dataBlockToStatements(deleteDataExpr.getDataBlock(), false, uc), uc.modifyContexts);
	}

	protected IExtendedIterator<?> evaluateSparql(TupleExpr tupleExpr, UpdateContext uc) {
		try {
			SparqlTupleExprRenderer renderer = new SparqlTupleExprRenderer();
			String sparql = renderer.render(tupleExpr);
			return dm.createQuery(sparql, uc.baseURI, uc.includeInferred, uc.readContexts).evaluate();
		} catch (KommaException e) {
			throw e;
		} catch (Exception e) {
			throw new KommaException(e);
		}

	}

	protected void executeModify(Modify modify, UpdateContext uc) throws KommaException {
		TupleExpr whereClause = modify.getWhereExpr();
		if (!(whereClause instanceof QueryRoot)) {
			whereClause = new QueryRoot(whereClause);
		}
		try (IExtendedIterator<IBindings<IValue>> sourceBindings = evaluateWhereClause(whereClause, uc)) {
			for (IBindings<IValue> bs : sourceBindings) {
				deleteBoundTriples(bs, modify.getDeleteExpr(), uc);
				insertBoundTriples(bs, modify.getInsertExpr(), uc);
			}
		}
	}

	private IExtendedIterator<IBindings<IValue>> evaluateWhereClause(final TupleExpr whereClause,
			final UpdateContext uc) throws KommaException {
		return evaluateSparql(whereClause, uc).mapWith(new IMap<Object, IBindings<IValue>>() {
			@Override
			public IBindings<IValue> map(Object value) {
				@SuppressWarnings("unchecked")
				IBindings<IValue> sourceBindings = (IBindings<IValue>) value;
				if (whereClause instanceof SingletonSet && sourceBindings.getKeys().isEmpty() && uc.bindings != null) {
					// in the case of an empty WHERE clause, we use the
					// supplied bindings to produce triples to
					// DELETE/INSERT
					return uc.bindings;
				} else {
					// check if any supplied bindings do not occur in
					// the bindingset produced by the WHERE clause. If
					// so, merge.
					Set<String> uniqueBindings = new HashSet<String>(uc.bindings.getKeys());
					uniqueBindings.removeAll(sourceBindings.getKeys());
					if (uniqueBindings.size() > 0) {
						LinkedHashBindings<IValue> mergedSet = new LinkedHashBindings<IValue>();
						for (String bindingName : sourceBindings.getKeys()) {
							mergedSet.put(bindingName, sourceBindings.get(bindingName));
						}
						for (String bindingName : uniqueBindings) {
							mergedSet.put(bindingName, uc.bindings.get(bindingName));
						}
						return mergedSet;
					}
					return sourceBindings;
				}
			}
		});

	}

	private void deleteBoundTriples(IBindings<IValue> whereBinding, TupleExpr deleteClause, UpdateContext uc)
			throws KommaException {
		if (deleteClause != null) {
			List<StatementPattern> deletePatterns = StatementPatternCollector.process(deleteClause);
			for (StatementPattern deletePattern : deletePatterns) {
				IReference subject = (IReference) getValueForVar(deletePattern.getSubjectVar(), whereBinding);
				IReference predicate = (IReference) getValueForVar(deletePattern.getPredicateVar(), whereBinding);
				IValue object = getValueForVar(deletePattern.getObjectVar(), whereBinding);

				IReference context = null;
				if (deletePattern.getContextVar() != null) {
					context = (IReference) getValueForVar(deletePattern.getContextVar(), whereBinding);
				}

				if (subject == null || predicate == null || object == null) {
					// skip removal of triple if any variable is unbound (may
					// happen with optional patterns)
					// See SES-1047.
					continue;
				}

				net.enilink.komma.core.StatementPattern stmt = new net.enilink.komma.core.StatementPattern(subject,
						predicate, object);
				if (context != null) {
					dm.remove(Collections.singleton(stmt), context);
				} else {
					dm.remove(Collections.singleton(stmt), uc.modifyContexts);
				}
			}
		}
	}

	private void insertBoundTriples(IBindings<IValue> whereBinding, TupleExpr insertClause, UpdateContext uc)
			throws KommaException {
		if (insertClause != null) {
			List<StatementPattern> insertPatterns = StatementPatternCollector.process(insertClause);
			// bnodes in the insert pattern are locally scoped for each
			// individual source binding.
			Map<String, IReference> bnodeMapping = new HashMap<>();
			for (StatementPattern insertPattern : insertPatterns) {
				net.enilink.komma.core.Statement toBeInserted = createStatementFromPattern(insertPattern, whereBinding,
						bnodeMapping);
				if (toBeInserted != null) {
					IReference[] with = uc.modifyContexts;
					if (with == null && toBeInserted.getContext() == null) {
						dm.add(toBeInserted);
					} else if (toBeInserted.getContext() == null) {
						dm.add(toBeInserted, with);
					} else {
						dm.add(toBeInserted, toBeInserted.getContext());
					}
				}
			}
		}
	}

	private net.enilink.komma.core.Statement createStatementFromPattern(StatementPattern pattern,
			IBindings<IValue> sourceBindings, Map<String, IReference> bnodeMapping) throws KommaException {
		IReference subject = null;
		IReference predicate = null;
		IValue object = null;
		IReference context = null;

		if (pattern.getSubjectVar().hasValue()) {
			subject = valueConverter.fromRdf4j((Resource) pattern.getSubjectVar().getValue());
		} else {
			subject = (IReference) sourceBindings.get(pattern.getSubjectVar().getName());
			if (subject == null && pattern.getSubjectVar().isAnonymous()) {
				subject = bnodeMapping.get(pattern.getSubjectVar().getName());
				if (subject == null) {
					subject = dm.blankNode();
					bnodeMapping.put(pattern.getSubjectVar().getName(), subject);
				}
			}
		}

		if (pattern.getPredicateVar().hasValue()) {
			predicate = valueConverter.fromRdf4j((Resource) pattern.getPredicateVar().getValue());
		} else {
			predicate = (IReference) sourceBindings.get(pattern.getPredicateVar().getName());
		}

		if (pattern.getObjectVar().hasValue()) {
			object = valueConverter.fromRdf4j(pattern.getObjectVar().getValue());
		} else {
			object = sourceBindings.get(pattern.getObjectVar().getName());
			if (object == null && pattern.getObjectVar().isAnonymous()) {
				object = bnodeMapping.get(pattern.getSubjectVar().getName());
				if (object == null) {
					object = dm.blankNode();
					bnodeMapping.put(pattern.getObjectVar().getName(), (IReference) object);
				}
			}
		}

		if (pattern.getContextVar() != null) {
			if (pattern.getContextVar().hasValue()) {
				context = valueConverter.fromRdf4j((Resource) pattern.getContextVar().getValue());
			} else {
				context = (IReference) sourceBindings.get(pattern.getContextVar().getName());
			}
		}

		net.enilink.komma.core.Statement st = null;
		if (subject != null && predicate != null && object != null) {
			if (context != null) {
				st = new net.enilink.komma.core.Statement(subject, predicate, object, context);
			} else {
				st = new net.enilink.komma.core.Statement(subject, predicate, object);
			}
		}
		return st;
	}

	private IValue getValueForVar(Var var, IBindings<IValue> bindings) throws KommaException {
		if (var.hasValue()) {
			return valueConverter.fromRdf4j(var.getValue());
		}
		return bindings.get(var.getName());
	}
}
