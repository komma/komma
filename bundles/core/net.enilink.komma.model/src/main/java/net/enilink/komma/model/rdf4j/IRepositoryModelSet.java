package net.enilink.komma.model.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

public interface IRepositoryModelSet {

	public Repository createRepository() throws RepositoryException;
}
