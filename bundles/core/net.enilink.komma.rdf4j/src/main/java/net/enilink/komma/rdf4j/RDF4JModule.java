package net.enilink.komma.rdf4j;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.dm.IDataManagerFactory;

public class RDF4JModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(RDF4JDataManagerFactory.class).in(Singleton.class);
		bind(IDataManagerFactory.class).to(RDF4JDataManagerFactory.class);
	}

	@Provides
	ValueFactory provideValueFactory(Repository repository) {
		return repository.getValueFactory();
	}
}
