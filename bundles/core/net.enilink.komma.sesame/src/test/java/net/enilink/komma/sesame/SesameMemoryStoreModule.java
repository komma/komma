package net.enilink.komma.sesame;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.core.KommaException;

/**
 * Configuration module that uses a Sesame {@link MemoryStore} as storage back
 * end.
 */
public class SesameMemoryStoreModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new SesameModule());
	}

	@Singleton
	@Provides
	Repository provideRepository() {
		Repository repository = new SailRepository(new MemoryStore());
		try {
			repository.initialize();
		} catch (RepositoryException e) {
			throw new KommaException(e);
		}

		return repository;
	}
}
