/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.internal.sesame.repository;

import java.io.File;
import java.net.URL;

/**
 * Provides a facade factory class to create repositories from sesame
 * configurations.
 * 
 * @author James Leigh
 */
public class LoaderRepositoryFactory {
	private File baseDir;
	private URL server;

	public LoaderRepositoryFactory(File baseDir) {
		assert baseDir != null;
		this.baseDir = baseDir;
	}

	public LoaderRepositoryFactory(URL server) {
		assert server != null;
		this.server = server;
	}

	public File getBaseDir() {
		return baseDir;
	}

	public URL getServer() {
		return server;
	}
}
