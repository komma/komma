package net.enilink.komma.sesame;

import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.core.KommaException;

/** Configuration module for unit tests that use Sesame as storage back end. */
public class SesameTestModule extends AbstractModule {
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
		} catch (StoreException e) {
			throw new KommaException(e);
		}

		return repository;
	}
}
