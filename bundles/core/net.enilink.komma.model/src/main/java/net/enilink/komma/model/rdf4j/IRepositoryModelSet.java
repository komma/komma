package net.enilink.komma.model.rdf4j;

import net.enilink.komma.core.IGraph;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

public interface IRepositoryModelSet {

	Repository createRepository(IGraph config) throws RepositoryException;
}
