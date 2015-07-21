package net.enilink.komma.model.sesame;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.model.MODELS;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

@Iri(MODELS.NAMESPACE + "SPARQLModelSet")
public abstract class SPARQLModelSetSupport extends MemoryModelSetSupport {

	public Repository createRepository() throws RepositoryException {
		SPARQLRepository repository = new SPARQLRepository(
				"http://localhost:8005/bigdata");
		repository.initialize();
		addBasicKnowledge(repository);
		return repository;
	}

}
