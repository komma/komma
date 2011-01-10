package net.enilink.komma.dm;

import com.google.inject.Provider;

public interface IDataManagerFactory extends Provider<IDataManager> {
	/**
	 * Closes the underlying data repository and frees all resources.
	 */
	void close();
}