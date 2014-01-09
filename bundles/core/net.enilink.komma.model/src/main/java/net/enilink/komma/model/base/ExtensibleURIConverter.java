/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.model.IContentHandler;
import net.enilink.komma.model.IURIConverter;
import net.enilink.komma.model.IURIHandler;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.eclipse.PlatformResourceURIHandler;
import net.enilink.komma.core.URI;

/**
 * A highly functional and extensible URI converter implementation.
 * <p>
 * This implementation provides seamless transparent Eclipse integration by
 * supporting the <code>platform:/resource</code> mechanism both inside of
 * Eclipse and outside of Eclipse. Furthermore, although the implementation
 * imports both {@link org.eclipse.core.runtime} and
 * {@link org.eclipse.core.resources}, and hence requires the Eclipse libraries
 * at development time, the implementation does <b>not</b> require them at
 * runtime. Clients of this implementation must be cautious if they wish to
 * maintain this platform neutral behaviour.
 * </p>
 */
public class ExtensibleURIConverter implements IURIConverter {
	protected List<IURIHandler> uriHandlers;

	protected List<IContentHandler> contentHandlers;

	/**
	 * The URI map.
	 */
	protected IURIMapRuleSet uriMap;

	protected static List<IURIHandler> DEFAULT_URI_HANDLERS = Collections
			.unmodifiableList(Arrays.<IURIHandler> asList(
					new PlatformResourceURIHandler(), new FileURIHandler(),
					new URIHandler()));

	/**
	 * Creates an instance.
	 */
	public ExtensibleURIConverter() {
		this(DEFAULT_URI_HANDLERS, ModelPlugin.getDefault()
				.getContentHandlerRegistry().getContentHandlers());
	}

	/**
	 * Creates an instance.
	 */
	public ExtensibleURIConverter(Collection<IURIHandler> uriHandlers,
			Collection<IContentHandler> contentHandlers) {
		getURIHandlers().addAll(uriHandlers);
		getContentHandlers().addAll(contentHandlers);
	}

	public Collection<IURIHandler> getURIHandlers() {
		if (uriHandlers == null) {
			uriHandlers = new ArrayList<IURIHandler>();
		}
		return uriHandlers;
	}

	public IURIHandler getURIHandler(URI uri) {
		for (IURIHandler uriHandler : uriHandlers) {
			if (uriHandler.canHandle(uri)) {
				return uriHandler;
			}
		}
		throw new RuntimeException("There is no URIHandler to handle " + uri);
	}

	public List<IContentHandler> getContentHandlers() {
		if (contentHandlers == null) {
			contentHandlers = new ArrayList<IContentHandler>();
		}
		return contentHandlers;
	}

	public OutputStream createOutputStream(URI uri) throws IOException {
		return createOutputStream(uri, null);
	}

	static class OptionsMap implements Map<Object, Object> {
		protected Object key;
		protected Object value;
		protected Map<?, ?> options;
		protected Map<Object, Object> mergedMap;

		public OptionsMap(Object key, Object value, Map<?, ?> options) {
			this.options = options == null ? Collections.EMPTY_MAP : options;
			this.key = key;
			this.value = value;
		}

		protected Map<Object, Object> mergedMap() {
			if (mergedMap == null) {
				mergedMap = new LinkedHashMap<Object, Object>(options);
				mergedMap.put(key, value);
			}
			return mergedMap;
		}

		public void clear() {
			throw new UnsupportedOperationException();
		}

		public boolean containsKey(Object key) {
			return this.key == key || this.key.equals(key)
					|| options.containsKey(key);
		}

		public boolean containsValue(Object value) {
			return this.value == value || options.containsValue(value);
		}

		public Set<Map.Entry<Object, Object>> entrySet() {
			return mergedMap().entrySet();
		}

		public Object get(Object key) {
			return this.key == key || this.key.equals(key) ? value : options
					.get(key);
		}

		public boolean isEmpty() {
			return false;
		}

		public Set<Object> keySet() {
			return mergedMap().keySet();
		}

		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		public void putAll(Map<? extends Object, ? extends Object> t) {
			throw new UnsupportedOperationException();
		}

		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			return mergedMap().size();
		}

		public Collection<Object> values() {
			return mergedMap().values();
		}

		@Override
		public int hashCode() {
			return mergedMap().hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return mergedMap().equals(0);
		}
	}

	public OutputStream createOutputStream(URI uri, Map<?, ?> options)
			throws IOException {
		URI normalizedURI = normalize(uri);
		return getURIHandler(normalizedURI).createOutputStream(normalizedURI,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	public InputStream createInputStream(URI uri) throws IOException {
		return createInputStream(uri, null);
	}

	public InputStream createInputStream(URI uri, Map<?, ?> options)
			throws IOException {
		URI normalizedURI = normalize(uri);
		return getURIHandler(normalizedURI).createInputStream(normalizedURI,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	public void delete(URI uri, Map<?, ?> options) throws IOException {
		URI normalizedURI = normalize(uri);
		getURIHandler(normalizedURI).delete(normalizedURI,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	public Map<String, ?> contentDescription(URI uri, Map<?, ?> options)
			throws IOException {
		URI normalizedURI = normalize(uri);
		return getURIHandler(normalizedURI).contentDescription(normalizedURI,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	public boolean exists(URI uri, Map<?, ?> options) {
		URI normalizedURI = normalize(uri);
		return getURIHandler(normalizedURI).exists(normalizedURI,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	public Map<String, ?> getAttributes(URI uri, Map<?, ?> options) {
		URI normalizedURI = normalize(uri);
		return getURIHandler(normalizedURI).getAttributes(normalizedURI,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	public void setAttributes(URI uri, Map<String, ?> attributes,
			Map<?, ?> options) throws IOException {
		URI normalizedURI = normalize(uri);
		getURIHandler(normalizedURI).setAttributes(normalizedURI, attributes,
				new OptionsMap(OPTION_URI_CONVERTER, this, options));
	}

	/**
	 * Returns the normalized form of the URI.
	 * <p>
	 * This implementation does precisely and only the
	 * {@link URIConverter#normalize typical} thing. It calls itself recursively
	 * so that mapped chains are followed.
	 * </p>
	 * 
	 * @param uri
	 *            the URI to normalize.
	 * @return the normalized form.
	 * @see org.eclipse.emf.ecore.plugin.EcorePlugin#getPlatformResourceMap
	 */

	public URI normalize(URI uri) {
		boolean changed;
		do {
			URI oldUri = uri;
			uri = getURIMapRules().map(uri);
			changed = !oldUri.equals(uri);
		} while (changed);

		return uri;
	}

	/*
	 * Javadoc copied from interface.
	 */
	public IURIMapRuleSet getURIMapRules() {
		if (uriMap == null) {
			uriMap = new CompoundURIMapRuleSet(new URIMapRuleSet(), ModelPlugin
					.getDefault().getURIMap());
		}

		return uriMap;
	}

	@Override
	public void setURIMapRules(IURIMapRuleSet uriMapRules) {
		this.uriMap = uriMapRules;
	}
}
