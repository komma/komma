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

import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.Query;
import org.openrdf.query.TupleQuery;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManagerQuery;
import net.enilink.komma.internal.sesame.result.SesameBooleanResult;
import net.enilink.komma.internal.sesame.result.SesameGraphResult;
import net.enilink.komma.internal.sesame.result.SesameTupleResult;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.SesameValueConverter;

/**
 * Implements {@link IDataManagerQuery} for {@link SesameRepositoryDataManager}.
 */
public class SesameQuery<R> implements IDataManagerQuery<R> {
	protected Query query;

	@Inject
	SesameValueConverter valueConverter;

	@Inject
	Injector injector;

	public SesameQuery(Query query) {
		this.query = query;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IExtendedIterator<R> evaluate() {
		try {
			IExtendedIterator<R> convertedResult;
			if (query instanceof TupleQuery) {
				convertedResult = (IExtendedIterator<R>) new SesameTupleResult(
						((TupleQuery) query).evaluate());
			} else if (query instanceof GraphQuery) {
				convertedResult = (IExtendedIterator<R>) new SesameGraphResult(
						((GraphQuery)query).evaluate());
			} else {
				convertedResult = (IExtendedIterator<R>) new SesameBooleanResult(
						((BooleanQuery)query).evaluate());
			}
			injector.injectMembers(convertedResult);
			return convertedResult;
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	@Override
	public IDataManagerQuery<R> setFirstResult(int startPosition) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IDataManagerQuery<R> setIncludeInferred(boolean include) {
		query.setIncludeInferred(include);
		return this;
	}

	@Override
	public IDataManagerQuery<R> setMaxResults(int maxResult) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IDataManagerQuery<R> setParameter(String name, IValue value) {
		query.setBinding(name, valueConverter.toSesame(value));
		return this;
	}

	@Override
	public boolean supportsIncludeInferred() {
		return true;
	}

	@Override
	public boolean supportsLimit() {
		return false;
	}
}
