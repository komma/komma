package net.enilink.komma.em.rdf4j;

import org.eclipse.rdf4j.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.em.CacheModule;
import net.enilink.komma.em.CachingEntityManagerModule;
import net.enilink.komma.em.EntityManagerFactoryModule;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.rdf4j.RDF4JModule;

/**
 * A Guice module that configures an {@link IEntityManager} wrapping an
 * {@link Repository RDF4J repository}.
 */
public class RDF4JEntityManagerModule extends AbstractModule {

	private Repository repository;
	private KommaModule kommaModule;

	public RDF4JEntityManagerModule(Repository repository, KommaModule module) {
		this.repository = repository;
		this.kommaModule = module;
	}

	@Override
	protected void configure() {
		install(new RDF4JModule());
		install(new EntityManagerFactoryModule(kommaModule, null,
				new CachingEntityManagerModule()));
		install(new CacheModule());

		UnitOfWork uow = new UnitOfWork();
		uow.begin();

		bind(UnitOfWork.class).toInstance(uow);
		bind(IUnitOfWork.class).toInstance(uow);
		bind(Repository.class).toInstance(repository);
	}

	@Provides
	protected IDataManager provideDataManager(IDataManagerFactory dmFactory) {
		return dmFactory.get();
	}

	/**
	 * Factory method to create an {@link IEntityManagerFactory entity manager factory} for a 
	 * given {@link Repository repository} and a {@link KommaModule KOMMA configuration}.
	 * 
	 * @param repository The RDF4J repository
	 * @param module The configuration for the entity manager
	 * @return A factory for creating entity manager instances
	 */
	public static IEntityManagerFactory createEntityManagerFactory(Repository repository, KommaModule module) {
		Injector injector = Guice.createInjector(new RDF4JEntityManagerModule(repository, module));
		return injector.getInstance(IEntityManagerFactory.class);
	}
}