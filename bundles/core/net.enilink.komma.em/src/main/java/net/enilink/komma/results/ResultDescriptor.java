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
package net.enilink.komma.results;

import java.util.HashMap;
import java.util.Map;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.em.internal.query.QueryBase;
import net.enilink.komma.query.SparqlBuilder;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IResultDescriptor;

public class ResultDescriptor<R> extends QueryBase<IResultDescriptor<R>>
		implements IResultDescriptor<R> {
	protected boolean includeInferred;

	protected Map<String, Object> parameters;

	protected IExtendedIterator<R> results;

	protected String property;

	protected String parameterVariable;

	protected String resultVariable;

	protected String sparql;

	protected SparqlBuilder sparqlBuilder;

	public ResultDescriptor(String sparql) {
		this.sparql = sparql;
	}

	public ResultDescriptor(String sparql, String property,
			String resultVariable, String parameterVariable) {
		this.sparql = sparql;
		this.property = property;
		this.resultVariable = resultVariable;
		this.parameterVariable = parameterVariable;
	}

	@Override
	public <T> IResultDescriptor<T> bindResultType(Class<T> resultType,
			Class<?>... resultTypes) {
		return super.doBindResultType(resultType, resultTypes);
	}

	@SuppressWarnings("unchecked")
	public IExtendedIterator<R> evaluate(IEntityManager manager) {
		if (results == null) {
			String sparql = toQueryString();
			IQuery<?> query = manager.createQuery(sparql);

			((QueryBase<?>) query).initializeFrom(this);
			query.setIncludeInferred(getIncludeInferred());
			if (parameters != null) {
				for (Map.Entry<String, Object> entry : parameters.entrySet()) {
					query.setParameter(entry.getKey(), entry.getValue());
				}
			}

			results = (IExtendedIterator<R>) query.evaluate();
		}
		return results;
	}

	@Override
	public boolean getIncludeInferred() {
		return includeInferred;
	}

	public String getParameterVariable() {
		return parameterVariable;
	}

	public String getProperty() {
		return property;
	}

	public String getResultVariable() {
		return resultVariable;
	}

	public String toQueryString() {
		if (sparqlBuilder != null) {
			return sparqlBuilder.toString();
		}
		return sparql;
	}

	protected SparqlBuilder getSparqlBuilder() {
		if (sparqlBuilder == null) {
			sparqlBuilder = new SparqlBuilder(sparql);
		}
		return sparqlBuilder;
	}

	@Override
	public IResultDescriptor<R> prefetch(IResultDescriptor<?> descriptor) {
		if (descriptor.getProperty() == null) {
			throw new IllegalArgumentException(
					"Iterator has an invalid property.");
		}
		return prefetch(descriptor.getProperty(), descriptor);
	}

	@Override
	public IResultDescriptor<R> prefetch(String property,
			IResultDescriptor<?> descriptor) {
		if (descriptor instanceof ResultDescriptor<?>
				&& ((ResultDescriptor<?>) descriptor).sparqlBuilder != null) {
			getSparqlBuilder().optional(property,
					descriptor.getResultVariable(),
					descriptor.getParameterVariable(),
					((ResultDescriptor<?>) descriptor).sparqlBuilder);
		} else {
			getSparqlBuilder().optional(property,
					descriptor.getResultVariable(),
					descriptor.getParameterVariable(), descriptor.toQueryString());
		}

		return this;
	}

	public IResultDescriptor<R> prefetchTypes() {
		getSparqlBuilder().fetchTypes();
		return this;
	}

	@Override
	public <T> IResultDescriptor<T> restrictResultType(Class<T> resultType,
			Class<?>... resultTypes) {
		return super.doRestrictResultType(resultType, resultTypes);
	}

	@Override
	public IResultDescriptor<R> setFirstResult(int startPosition) {
		return super.setFirstResult(startPosition);
	}

	@Override
	public IResultDescriptor<R> setIncludeInferred(boolean include) {
		this.includeInferred = include;
		return this;
	}

	@Override
	public IResultDescriptor<R> setParameter(String name, Object value) {
		if (parameters == null) {
			parameters = new HashMap<String, Object>();
		}
		parameters.put(name, value);
		return this;
	}
}
