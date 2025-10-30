package net.enilink.komma.model.rdf4j;

import net.enilink.komma.core.IGraph;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.model.MODELS;

@Iri(MODELS.NAMESPACE + "SPARQLModelSet")
public abstract class SPARQLModelSetSupport extends MemoryModelSetSupport {

	public Repository createRepository(IGraph config) throws RepositoryException {
		SPARQLRepository repository = new SPARQLRepository(
				"http://localhost:8005/bigdata");
		repository.init();
		addBasicKnowledge(repository);
		return repository;
	}

}
