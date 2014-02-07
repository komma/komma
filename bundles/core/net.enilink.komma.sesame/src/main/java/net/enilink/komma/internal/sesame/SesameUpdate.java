package net.enilink.komma.internal.sesame;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LinkedHashBindings;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerUpdate;
import net.enilink.komma.sesame.SesameValueConverter;

import org.openrdf.model.ValueFactory;
import org.openrdf.query.Dataset;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UnsupportedQueryLanguageException;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.QueryParserUtil;

import com.google.inject.Inject;

/**
 * Implements {@link IDataManagerUpdate} for {@link SesameRepositoryDataManager}
 * .
 */
public class SesameUpdate implements IDataManagerUpdate {
	@Inject
	SesameValueConverter valueConverter;

	@Inject
	ValueFactory vf;

	final IDataManager dm;
	final ParsedUpdate parsedUpdate;
	final String baseURI;
	final boolean includeInferred;
	final LinkedHashBindings<IValue> bindings = new LinkedHashBindings<>();
	final IReference[] readContexts;
	final IReference[] modifyContexts;

	public SesameUpdate(IDataManager dm, String update, String baseURI,
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
		}
	}

	@Override
	public IDataManagerUpdate setParameter(String name, IValue value) {
		bindings.put(name, value);
		return this;
	}

}
