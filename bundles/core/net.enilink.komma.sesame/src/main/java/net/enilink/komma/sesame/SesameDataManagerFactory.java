package net.enilink.komma.sesame;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.SparqlStandardDialect;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.internal.sesame.SesameRepositoryDataManager;

public class SesameDataManagerFactory implements IDataManagerFactory {
	@Inject
	protected Injector injector;

	@Inject
	protected Repository repository;

	@Inject(optional = true)
	protected IDialect dialect;

	@Override
	public IDataManager get() {
		return injector.getInstance(SesameRepositoryDataManager.class);
	}

	@Override
	public IDialect getDialect() {
		if (dialect == null) {
			dialect = createDialect();
		}
		return dialect;
	}

	protected IDialect createDialect() {
		return new SparqlStandardDialect();
	}

	@Override
	public void close() {
		if (repository != null) {
			try {
				repository.shutDown();
				repository = null;
			} catch (RepositoryException e) {
				throw new KommaException(e);
			}
		}
	}
}
