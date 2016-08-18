package net.enilink.komma.internal.rdf4j;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.algebra.UpdateExpr;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;

import com.google.inject.Inject;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LinkedHashBindings;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.rdf4j.RDF4JValueConverter;

/**
 * Implements {@link IDataManagerUpdate} for {@link RDF4JRepositoryDataManager}
 * .
 */
public class RDF4JUpdate implements IDataManagerUpdate {
	@Inject
	RDF4JValueConverter valueConverter;

	@Inject
	ValueFactory vf;

	final IDataManager dm;
	final ParsedUpdate parsedUpdate;
	final String baseURI;
	final boolean includeInferred;
	final LinkedHashBindings<IValue> bindings = new LinkedHashBindings<>();
	final IReference[] readContexts;
	final IReference[] modifyContexts;

	public RDF4JUpdate(IDataManager dm, String update, String baseURI,
			boolean includeInferred, IReference[] readContexts,
			IReference[] modifyContexts) {
		this.dm = dm;
		try {
			this.parsedUpdate = QueryParserUtil.parseUpdate(
					QueryLanguage.SPARQL, update, baseURI);
		} catch (MalformedQueryException e) {
			throw new KommaException(e);
		} catch (UnsupportedQueryLanguageException e) {
			throw new KommaException(e);
		}
		this.baseURI = baseURI;
		this.includeInferred = includeInferred;
		this.readContexts = readContexts;
		this.modifyContexts = modifyContexts;
	}

	@Override
	public void execute() {
		SparqlUpdateExecutor updateExecutor = new SparqlUpdateExecutor(dm,
				valueConverter, vf);
		boolean localTransaction = !dm.getTransaction().isActive();
		try {
			if (localTransaction) {
				dm.getTransaction().begin();
			}
			for (UpdateExpr updateExpr : parsedUpdate.getUpdateExprs()) {
				// TODO use and filter dataset
				Dataset dataset = parsedUpdate.getDatasetMapping().get(
						updateExpr);
				updateExecutor.executeUpdate(updateExpr, baseURI, readContexts,
						modifyContexts, bindings, includeInferred);
			}
			if (localTransaction) {
				dm.getTransaction().commit();
			}
		} catch (RuntimeException e) {
			if (localTransaction && dm.getTransaction().isActive()) {
				dm.getTransaction().rollback();
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public IDataManagerUpdate setParameter(String name, IValue value) {
		bindings.put(name, value);
		return this;
	}

}
