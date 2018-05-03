package net.enilink.komma.dm;

import com.google.inject.Provider;

import net.enilink.komma.core.IDialect;

public interface IDataManagerFactory extends Provider<IDataManager> {
	/**
	 * Return the SPARQL dialect that is specific for the underlying store.
	 * 
	 * @return The specific dialect.
	 */
	IDialect getDialect();

	/**
	 * Closes the underlying data repository and frees all resources.
	 */
	void close();
}