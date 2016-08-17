package net.enilink.komma.model.sesame;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;

public interface IRepositoryModelSet {

	public Repository createRepository() throws RepositoryException;
}
