package net.enilink.komma.sesame;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

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
