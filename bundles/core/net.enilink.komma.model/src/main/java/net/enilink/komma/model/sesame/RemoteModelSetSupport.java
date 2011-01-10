package net.enilink.komma.model.sesame;

import java.util.Collection;

import net.enilink.composition.annotations.Iri;
import org.openrdf.repository.Repository;
import org.openrdf.repository.http.HTTPRepository;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.SesameModule;

@Iri(MODELS.NAMESPACE + "RemoteModelSet")
public abstract class RemoteModelSetSupport implements IModelSet.Internal {
	@Override
	public void collectInjectionModules(Collection<Module> modules) {
		modules.add(new SesameModule());
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				
			}

			@Singleton
			@Provides
			protected Repository provideRepository() {
				try {
					HTTPRepository repo = new HTTPRepository(
							"http://192.168.56.101:8080/openrdf-sesame",
							"owlim-max");
					repo.setReadOnly(false);
					repo.initialize();
					return repo;
					// RemoteRepositoryManager man = RemoteRepositoryManager
					// .getInstance("http://192.168.56.101:8080/openrdf-sesame");
					// return man.getRepository("owlim-max");
				} catch (Exception e) {
					throw new KommaException("Unable to initialize repository",
							e);
				}

			}
		});
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
}
