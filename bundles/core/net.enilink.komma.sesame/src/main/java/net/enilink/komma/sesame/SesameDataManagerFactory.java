package net.enilink.komma.sesame;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.internal.sesame.SesameRepositoryDataManager;
import net.enilink.komma.core.KommaException;

public class SesameDataManagerFactory implements IDataManagerFactory {
	@Inject
	protected Injector injector;

	@Inject
	protected Repository repository;

	@Override
	public IDataManager get() {
		return injector.getInstance(SesameRepositoryDataManager.class);
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