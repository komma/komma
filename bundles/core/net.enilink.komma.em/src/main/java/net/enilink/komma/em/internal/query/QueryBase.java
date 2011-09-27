/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.enilink.komma.core.IQueryBase;

public abstract class QueryBase<Q extends IQueryBase<Q>> implements
		IQueryBase<Q> {
	protected int firstResult;
	protected int maxResults;
	protected Map<String, ResultInfo> resultInfos;

	@SuppressWarnings("unchecked")
	protected <T, NQ extends IQueryBase<NQ>> NQ doBindResultType(
			Class<T> resultType, Class<?>... resultTypes) {
		ResultInfo resultInfo = new ResultInfo(false, new ArrayList<Class<?>>(
				1 + resultTypes.length));
		resultInfo.types.add(resultType);
		for (Class<?> type : resultTypes) {
			resultInfo.types.add(type);
		}
		ensureResultInfos().put(null, resultInfo);

		return (NQ) this;
	}

	@SuppressWarnings("unchecked")
	public Q bindResultType(String name, Class<?>... resultTypes) {
		ensureResultInfos();

		ResultInfo resultInfo = new ResultInfo(false,
				Arrays.asList(resultTypes));
		ensureResultInfos().put(name, resultInfo);

		return (Q) this;
	}

	protected Map<String, ResultInfo> ensureResultInfos() {
		if (resultInfos == null) {
			resultInfos = new HashMap<String, ResultInfo>();
		}
		return resultInfos;
	}

	public int getFirstResult() {
		return firstResult;
	}

	public int getMaxResults() {
		return maxResults;
	}

	@SuppressWarnings("unchecked")
	protected <T, NQ extends IQueryBase<NQ>> NQ doRestrictResultType(
			Class<T> resultType, Class<?>... resultTypes) {
		ResultInfo resultInfo = new ResultInfo(true, new ArrayList<Class<?>>(
				1 + resultTypes.length));
		resultInfo.types.add(resultType);
		for (Class<?> type : resultTypes) {
			resultInfo.types.add(type);
		}

		ensureResultInfos().put(null, resultInfo);

		return (NQ) this;
	}

	@SuppressWarnings("unchecked")
	public Q restrictResultType(String name, Class<?>... resultTypes) {
		ResultInfo resultInfo = new ResultInfo(true, Arrays.asList(resultTypes));
		ensureResultInfos().put(name, resultInfo);

		return (Q) this;
	}

	@SuppressWarnings("unchecked")
	public Q setFirstResult(int startPosition) {
		this.firstResult = startPosition;
		return (Q) this;
	}

	@SuppressWarnings("unchecked")
	public Q setMaxResults(int maxResult) {
		this.maxResults = maxResult;
		return (Q) this;
	}

	public void initializeFrom(QueryBase<?> other) {
		this.resultInfos = other.resultInfos;
		this.firstResult = other.firstResult;
		this.maxResults = other.maxResults;
	}
}
