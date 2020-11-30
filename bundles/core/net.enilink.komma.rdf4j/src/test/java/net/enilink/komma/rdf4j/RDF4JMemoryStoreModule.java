package net.enilink.komma.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.core.KommaException;
import net.enilink.komma.rdf4j.RDF4JModule;

/**
 * Configuration module that uses a RDF4J {@link MemoryStore} as storage back
 * end.
 */
public class RDF4JMemoryStoreModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new RDF4JModule());
	}

	@Singleton
	@Provides
	Repository provideRepository() {
		Repository repository = new SailRepository(new MemoryStore());
		try {
			repository.init();
		} catch (RepositoryException e) {
			throw new KommaException(e);
		}

		return repository;
	}
}
