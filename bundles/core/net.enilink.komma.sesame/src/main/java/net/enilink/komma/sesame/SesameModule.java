package net.enilink.komma.sesame;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.dm.IDataManagerFactory;

public class SesameModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(SesameDataManagerFactory.class).in(Singleton.class);
		bind(IDataManagerFactory.class).to(SesameDataManagerFactory.class);
	}

	@Provides
	ValueFactory provideValueFactory(Repository repository) {
		return repository.getValueFactory();
	}
}
