/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.internal.rdf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.TupleQuery;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Properties;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.internal.rdf4j.result.RDF4JBooleanResult;
import net.enilink.komma.internal.rdf4j.result.RDF4JGraphResult;
import net.enilink.komma.internal.rdf4j.result.RDF4JTupleResult;
import net.enilink.komma.rdf4j.RDF4JValueConverter;

/**
 * Implements {@link IDataManagerQuery} for {@link RDF4JRepositoryDataManager}.
 */
public class RDF4JQuery<R> implements IDataManagerQuery<R> {
	protected static Set<String> supportedProperties = new HashSet<>(
			Arrays.asList(Properties.TIMEOUT));

	protected Map<String, Object> properties;

	@Inject
	Injector injector;

	protected Query query;

	@Inject
	RDF4JValueConverter valueConverter;

	public RDF4JQuery(Query query) {
		this.query = query;
	}

	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	public IExtendedIterator<R> evaluate() {
		try {
			IExtendedIterator<R> convertedResult;
			if (query instanceof TupleQuery) {
				convertedResult = (IExtendedIterator<R>) new RDF4JTupleResult(
						((TupleQuery) query).evaluate());
			} else if (query instanceof GraphQuery) {
				convertedResult = (IExtendedIterator<R>) new RDF4JGraphResult(
						((GraphQuery) query).evaluate());
			} else {
				convertedResult = (IExtendedIterator<R>) new RDF4JBooleanResult(
						((BooleanQuery) query).evaluate());
			}
			injector.injectMembers(convertedResult);
			return convertedResult;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public Map<String, Object> getProperties() {
		return properties == null ? Collections.<String, Object> emptyMap()
				: Collections.unmodifiableMap(properties);
	}

	@Override
	public Set<String> getSupportedProperties() {
		return supportedProperties;
	}

	@Override
	public IDataManagerQuery<R> setParameter(String name, IValue value) {
		Value boundValue = valueConverter.toRdf4j(value);
		if (boundValue == null) {
			query.removeBinding(name);
		} else {
			query.setBinding(name, boundValue);
		}
		return this;
	}

	protected Map<String, Object> ensureProperties() {
		if (properties == null) {
			properties = new HashMap<>();
		}
		return properties;
	}

	@Override
	public IDataManagerQuery<R> setProperty(String propertyName, Object value) {
		switch (propertyName) {
		case Properties.TIMEOUT:
			if (value instanceof Number) {
				long timeout = ((Number) value).longValue();
				query.setMaxExecutionTime(timeout <= 0 ? 0 : (int) (timeout / 1000));
				ensureProperties().put(propertyName, value);
			} else
				throw new IllegalArgumentException("Illegal argument '" + value
						+ "' for property " + Properties.TIMEOUT);
		}
		return this;
	}
}
