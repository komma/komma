package net.enilink.komma.model.sesame;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;

public interface IRepositoryModelSet {

	public Repository createRepository() throws RepositoryException;
}