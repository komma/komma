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

import org.openrdf.query.Query;
import org.openrdf.result.BooleanResult;
import org.openrdf.result.GraphResult;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

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
			Result<?> result = query.evaluate();

			IExtendedIterator<R> convertedResult;
			if (result instanceof TupleResult) {
				convertedResult = (IExtendedIterator<R>) new SesameTupleResult(
						(TupleResult) result);
			} else if (result instanceof GraphResult) {
				convertedResult = (IExtendedIterator<R>) new SesameGraphResult(
						(GraphResult) result);
			} else {
				convertedResult = (IExtendedIterator<R>) new SesameBooleanResult(
						(BooleanResult) result);
			}
			injector.injectMembers(convertedResult);
			return convertedResult;
		} catch (StoreException e) {
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
