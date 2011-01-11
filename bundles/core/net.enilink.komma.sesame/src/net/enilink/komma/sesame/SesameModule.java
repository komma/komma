package net.enilink.komma.sesame;

import org.openrdf.model.LiteralFactory;
import org.openrdf.model.URIFactory;
import org.openrdf.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;

public class SesameModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(SesameDataManagerFactory.class).in(Singleton.class);
		bind(IDataManagerFactory.class).to(SesameDataManagerFactory.class);
		bind(IDataManager.class).toProvider(SesameDataManagerFactory.class);
	}

	@Provides
	LiteralFactory provideLiteralFactory(Repository repository) {
		return repository.getLiteralFactory();
	}

	@Provides
	URIFactory provideURIFactory(Repository repository) {
		return repository.getURIFactory();
	}
}
