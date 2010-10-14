package net.enilink.komma.model.sesame;

import net.enilink.composition.annotations.Iri;
import org.openrdf.repository.Repository;
import org.openrdf.repository.event.base.NotifyingRepositoryWrapper;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.store.StoreException;

import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.base.AbstractModelSetSupport;
import net.enilink.komma.core.KommaException;

@Iri(MODELS.NAMESPACE + "RemoteModelSet")
public abstract class RemoteModelSetSupport extends AbstractModelSetSupport {
	protected Repository createRepository() throws StoreException {
		try {
			HTTPRepository repo = new HTTPRepository(
					"http://192.168.56.101:8080/openrdf-sesame", "owlim-max");
			repo.setReadOnly(false);
			repo.initialize();
			return repo;
			// RemoteRepositoryManager man = RemoteRepositoryManager
			// .getInstance("http://192.168.56.101:8080/openrdf-sesame");
			// return man.getRepository("owlim-max");
		} catch (Exception e) {
			throw new StoreException("Unable to initialize repository", e);
		}
	}

	@Override
	protected void init() {
		super.init();

		Repository repository;
		try {
			repository = createRepository();
		} catch (StoreException e) {
			throw new KommaException("Creating repository failed", e);
		}
		NotifyingRepositoryWrapper notifyingRepository = new NotifyingRepositoryWrapper(
				repository);
		notifyingRepository.setDefaultReportDeltas(true);
		initRepository(notifyingRepository);
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
}
