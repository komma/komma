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
package net.enilink.komma.internal.query;

import java.util.Arrays;

import net.enilink.komma.core.IQueryBase;

public abstract class QueryBase<Q extends IQueryBase<Q>> implements
		IQueryBase<Q> {
	protected int firstResult;
	protected int maxResults;
	protected ResultInfo[] resultInfos;

	@SuppressWarnings("unchecked")
	protected <T, NQ extends IQueryBase<NQ>> NQ doBindResultType(Class<T> resultType,
			Class<?>... resultTypes) {
		ensureResultInfos(1);

		ResultInfo resultInfo = new ResultInfo(false,
				new Class<?>[resultTypes.length + 1]);
		resultInfo.types[0] = resultType;
		System.arraycopy(resultTypes, 0, resultInfo.types, 1,
				resultTypes.length);

		this.resultInfos[0] = resultInfo;

		return (NQ) this;
	}

	@SuppressWarnings("unchecked")
	public Q bindResultType(int column, Class<?>... resultTypes) {
		ensureResultInfos(column);

		ResultInfo resultInfo = new ResultInfo(false, resultTypes);
		this.resultInfos[column] = resultInfo;

		return (Q) this;
	}

	protected void ensureResultInfos(int column) {
		if (resultInfos == null) {
			resultInfos = new ResultInfo[column + 1];
		} else if (resultInfos.length <= column) {
			resultInfos = Arrays.copyOf(resultInfos, column);
		}
	}

	public int getFirstResult() {
		return 0;
	}

	public int getMaxResults() {
		return maxResults;
	}

	@SuppressWarnings("unchecked")
	protected <T, NQ extends IQueryBase<NQ>> NQ doRestrictResultType(
			Class<T> resultType, Class<?>... resultTypes) {
		ensureResultInfos(1);

		ResultInfo resultInfo = new ResultInfo(true,
				new Class<?>[resultTypes.length + 1]);
		resultInfo.types[0] = resultType;
		System.arraycopy(resultTypes, 0, resultInfo.types, 1,
				resultTypes.length);

		this.resultInfos[0] = resultInfo;

		return (NQ) this;
	}

	@SuppressWarnings("unchecked")
	public Q restrictResultType(int column, Class<?>... resultTypes) {
		ensureResultInfos(column);

		ResultInfo resultInfo = new ResultInfo(true, resultTypes);
		this.resultInfos[column] = resultInfo;

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
