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
package net.enilink.komma.internal.sesame;

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
import net.enilink.komma.internal.sesame.result.SesameBooleanResult;
import net.enilink.komma.internal.sesame.result.SesameGraphResult;
import net.enilink.komma.internal.sesame.result.SesameTupleResult;
import net.enilink.komma.sesame.SesameValueConverter;

/**
 * Implements {@link IDataManagerQuery} for {@link SesameRepositoryDataManager}.
 */
public class SesameQuery<R> implements IDataManagerQuery<R> {
	protected static Set<String> supportedProperties = new HashSet<>(
			Arrays.asList(Properties.TIMEOUT));

	protected Map<String, Object> properties;

	@Inject
	Injector injector;

	protected Query query;

	@Inject
	SesameValueConverter valueConverter;

	public SesameQuery(Query query) {
		this.query = query;
	}

	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	public IExtendedIterator<R> evaluate() {
		try {
			IExtendedIterator<R> convertedResult;
			if (query instanceof TupleQuery) {
				convertedResult = (IExtendedIterator<R>) new SesameTupleResult(
						((TupleQuery) query).evaluate());
			} else if (query instanceof GraphQuery) {
				convertedResult = (IExtendedIterator<R>) new SesameGraphResult(
						((GraphQuery) query).evaluate());
			} else {
				convertedResult = (IExtendedIterator<R>) new SesameBooleanResult(
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
		Value boundValue = valueConverter.toSesame(value);
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
