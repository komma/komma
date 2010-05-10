/*
 * Copyright James Leigh (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.internal.sesame.repository;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This repository monitors external datasets and loads then into a context in
 * the repository.
 * 
 * @author James Leigh
 */
public class LoaderRepository extends RepositoryWrapper {
	private final Logger logger = LoggerFactory
			.getLogger(LoaderRepository.class);
	private Map<URL, Map<URL, URI>> index = new ConcurrentHashMap<URL, Map<URL, URI>>();
	private Map<URL, Long> expires = new ConcurrentHashMap<URL, Long>();
	private ClassLoader cl = Thread.currentThread().getContextClassLoader();
	private long lastModified;

	public LoaderRepository() {
		super();
	}

	public LoaderRepository(Repository delegate) {
		super(delegate);
	}

	public void setClassLoader(ClassLoader cl) {
		this.cl = cl;
	}

	public synchronized void loadResources(String path) throws IOException,
			StoreException, RDFParseException {
		refresh();
		Enumeration<URL> resources = cl.getResources(path);
		if (!resources.hasMoreElements()) {
			ClassLoader classLoader = LoaderRepository.class.getClassLoader();
			resources = classLoader.getResources(path);
		}
		if (!resources.hasMoreElements())
			throw new IllegalArgumentException(path + " not found");
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			Map<URL, URI> map = new ConcurrentHashMap<URL, URI>();
			loadIndex(resource, resource.openConnection(), map);
			index.put(resource, map);
			for (Map.Entry<URL, URI> e : map.entrySet()) {
				reload(e.getKey(), e.getValue());
			}
		}
		lastModified = System.currentTimeMillis();
	}

	public synchronized void loadContext(URL dataset, URI context)
			throws IOException, StoreException, RDFParseException {
		URI ctx = context == null ? createURI(null, dataset) : context;
		Map<URL, URI> map = new ConcurrentHashMap<URL, URI>();
		map.put(dataset, ctx);
		index.put(dataset, map);
		reload(dataset, ctx);
		lastModified = System.currentTimeMillis();
	}

	@Override
	public RepositoryConnection getConnection() throws StoreException {
		try {
			refresh();
		} catch (IOException e) {
			throw new StoreException(e);
		} catch (RDFParseException e) {
			throw new StoreException(e);
		}
		return super.getConnection();
	}

	@Override
	public void shutDown() throws StoreException {
		clearLoadedContexts();
		super.shutDown();
	}

	public void clearLoadedContexts() throws StoreException {
		RepositoryConnection conn = super.getConnection();
		try {
			conn.begin();
			for (Map.Entry<URL, Map<URL, URI>> e : index.entrySet()) {
				for (Map.Entry<URL, URI> f : e.getValue().entrySet()) {
					conn.clear(f.getValue());
				}
			}
			conn.commit();
		} finally {
			conn.close();
		}
	}

	private void refresh() throws IOException, StoreException,
			RDFParseException {
		URLConnection open;
		long modified = lastModified;
		long now = System.currentTimeMillis();
		for (Map.Entry<URL, Map<URL, URI>> e : index.entrySet()) {
			open = getUrlIfNeeded(e, now);
			if (open != null) {
				long m = loadIndex(e.getKey(), open, e.getValue());
				if (m > modified) {
					modified = m;
				}
			}
			for (Map.Entry<URL, URI> f : e.getValue().entrySet()) {
				open = getUrlIfNeeded(f.getKey(), now);
				if (open != null) {
					long m = reload(f.getKey(), open, f.getValue());
					if (m > modified) {
						modified = m;
					}
				}
			}
		}
		lastModified = modified;
	}

	private URI createURI(String context, URL dataset) {
		ValueFactory vf = getValueFactory();
		if (context == null || context.length() == 0)
			return vf.createURI(dataset.toExternalForm());
		return vf.createURI(context);
	}

	private URLConnection getUrlIfNeeded(Map.Entry<URL, ?> e, long now)
			throws IOException, StoreException {
		if (e.getValue() == null)
			return e.getKey().openConnection();
		return getUrlIfNeeded(e.getKey(), now);
	}

	private URLConnection getUrlIfNeeded(URL url, long now) throws IOException,
			StoreException {
		if (expires.containsKey(url) && now < expires.get(url))
			return null;
		URLConnection open = url.openConnection();
		open.setUseCaches(true);
		if (lastModified == 0)
			return open;
		open.setIfModifiedSince(lastModified);
		if (open.getLastModified() > lastModified)
			return open;
		return null;
	}

	private long reload(URL dataset, URI context) throws StoreException,
			IOException, RDFParseException {
		URLConnection open = dataset.openConnection();
		open.setUseCaches(true);
		return reload(dataset, open, context);
	}

	private synchronized long loadIndex(URL url, URLConnection conn,
			Map<URL, URI> map) throws IOException {
		long modified = conn.getLastModified();
		long expiration = conn.getExpiration();
		RDFFormat format = findRdfFormat(url, conn, null);
		if (format == null) {
			Properties p = new Properties();
			p.load(conn.getInputStream());
			map.clear();
			for (Map.Entry<?, ?> e : p.entrySet()) {
				String path = e.getKey().toString();
				String context = e.getValue().toString();
				Enumeration<URL> resources = cl.getResources(path);
				while (resources.hasMoreElements()) {
					URL dataset = resources.nextElement();
					map.put(dataset, createURI(context, dataset));
				}
			}
		} else if (!map.containsKey(url)) {
			map.put(url, createURI(null, url));
		}
		if (expiration > 0) {
			expires.put(url, expiration);
		}
		return modified;
	}

	private synchronized long reload(URL dataset, URLConnection url, URI context)
			throws StoreException, IOException, RDFParseException {
		RDFFormat format = findRdfFormat(dataset, url, RDFFormat.RDFXML);
		logger.info("Loading {}", dataset);
		long modified = url.getLastModified();
		long expiration = url.getExpiration();
		RepositoryConnection conn = super.getConnection();
		try {
			conn.begin();
			conn.clear(context);
			conn.add(url.getInputStream(), dataset.toExternalForm(), format,
					context);
			conn.commit();
		} finally {
			conn.close();
		}
		if (expiration > 0) {
			expires.put(dataset, expiration);
		}
		return modified;
	}

	private RDFFormat findRdfFormat(URL url, URLConnection conn,
			RDFFormat defaultFormat) {
		RDFFormat format = RDFFormat.forMIMEType(conn.getContentType());
		if (format == null) {
			format = RDFFormat.forFileName(url.getFile(), defaultFormat);
		}
		return format;
	}
}
