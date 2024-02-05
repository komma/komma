package net.enilink.komma.model.rdf4j;

import org.eclipse.rdf4j.repository.Repository;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.name.Names;

import net.enilink.komma.core.KommaModule;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IModelSetFactory;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelSetModule;

/**
 * Helper class to create RDF4J based model sets. 
 */
public class RDF4JModelSetFactory {
	/**
	 * Creates a model set that works on the given {@link Repository RDF4J repository}
	 * 
	 * @param repository The base repository
	 * @return A model set
	 */
	public static IModelSet createModelSet(Repository repository) {
		// create configuration and a model set factory
		KommaModule module = ModelPlugin.createModelSetModule(RDF4JModelSetFactory.class.getClassLoader());
		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module), new AbstractModule() {
			@Override
			protected void configure() {
				bind(Key.get(Repository.class, Names.named("data-repository"))).toInstance(repository);
			}
		}).getInstance(IModelSetFactory.class);

		// create a model set with the given repository
		return factory.createModelSet(MODELS.NAMESPACE_URI.appendFragment("InjectedRepositoryModelSet"));
	}
}
