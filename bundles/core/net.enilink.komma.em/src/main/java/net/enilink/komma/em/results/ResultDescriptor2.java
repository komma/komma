/*******************************************************************************
 * Copyright (c) 2021 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.results;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.em.internal.query.QueryBase;

public class ResultDescriptor2<R> extends QueryBase<IResultDescriptor<R>> implements IResultDescriptor<R> {
	protected static Pattern PREFIX_DECL = Pattern
			.compile("prefix\\s+([^:]+)\\s*:\\s*<((?:[^>\\\\]|\\\\[tbnrf\\\\>])+)>", Pattern.CASE_INSENSITIVE);

	protected static Pattern STRING_OR_IRI = Pattern.compile(//
			"'''((:?'|'')?(?:[^'\\\\]|\\\\[tbnrf\\\\\"']))*'''|" + //
					"\"\"\"((:?\"|\"\")?(?:[^'\\\\]|\\\\[tbnrf\\\\\"']))*\"\"\"|" + //
					"'([^'\\\\]|\\\\[tbnrf\\\\\"'])*'|" + //
					"\"([^\"\\\\]|\\\\[tbnrf\\\\\"'])*\"|" + //
					"<([^>\\\\]|\\\\[tbnrf\\\\>])*>");

	// this is the SPARQL definition of PN_CHARS_BASE, but excludes the '<'
	// character
	// https://www.w3.org/TR/sparql11-query/
	protected static Pattern PN_CHARS_BASE = Pattern.compile(
			"[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD&&[^<]]");
	protected static Pattern PN_CHARS_U = Pattern.compile(PN_CHARS_BASE + "|[_]");
	protected static Pattern PN_CHARS = Pattern.compile(PN_CHARS_U + "|-|[0-9]|\u00B7|[\u0300-\u036F\u203F-\u2040]");

	protected static Pattern PN_PREFIX = Pattern.compile(PN_CHARS_BASE + "((" + PN_CHARS + "|[.])*" + PN_CHARS + ")?");
	protected static Pattern PNAME_NS = Pattern.compile("(" + PN_PREFIX + ")?:");

	protected static Pattern PERCENT = Pattern.compile("%[0-9A-Fa-f]{2}");
	protected static Pattern PN_LOCAL_ESC = Pattern.compile("\\\\[_~.\\-!$&'()*+,;=/?#@%]");
	protected static Pattern PLX = Pattern.compile(PERCENT + "|" + PN_LOCAL_ESC);
	protected static Pattern PN_LOCAL = Pattern.compile("(?:" + PN_CHARS_U + "|[:0-9]|" + PLX + ")((?:" + PN_CHARS
			+ "|[.:]|" + PLX + ")*(?:" + PN_CHARS + "|:|" + PLX + "))?");

	protected static Pattern PNAME_LN = Pattern.compile(PNAME_NS.toString() + PN_LOCAL);

	protected static Pattern VARNAME = Pattern
			.compile("(?:" + PN_CHARS_U + "|[0-9])(?:" + PN_CHARS_U + "|[0-9\u00B7\u0300-\u036F\u203F-\u2040])*");
	protected static Pattern VAR = Pattern.compile("[?$]" + VARNAME);

	protected boolean includeInferred;

	protected Map<String, Object> parameters;

	protected IExtendedIterator<R> results;

	protected String property;

	protected String parameterVariable;

	protected String resultVariable;

	static class PartialQuery {
		protected String whereClause;

		protected String modifiers;
	}

	PartialQuery partialQuery;

	public ResultDescriptor2(String sparql) {
		partialQuery = new PartialQuery();
		int whereStart = sparql.indexOf('{');
		int whereEnd = sparql.lastIndexOf('}');

		partialQuery.whereClause = sparql.substring(whereStart + 1, whereEnd);
		partialQuery.modifiers = sparql.substring(whereEnd + 1);
		String prologue = sparql.substring(0, whereStart);
		Map<String, String> prefixDeclarations = extractPrefixDeclarations(prologue);
	}

	public ResultDescriptor2(String sparql, String property, String resultVariable, String parameterVariable) {
		this(sparql);
		this.property = property;
		this.resultVariable = resultVariable;
		this.parameterVariable = parameterVariable;
	}

	protected static Map<String, String> extractPrefixDeclarations(String prologue) {
		Map<String, String> declarations = new HashMap<>();
		Matcher m = PREFIX_DECL.matcher(prologue);
		while (m.find()) {
			String prefix = m.group(1);
			String uri = m.group(2);
			declarations.put(prefix, uri);
		}
		return declarations;
	}

	@Override
	public <T> IResultDescriptor<T> bindResultType(Class<T> resultType, Class<?>... resultTypes) {
		return super.<IResultDescriptor<T>>doBindResultType(resultType, resultTypes);
	}

	@SuppressWarnings("unchecked")
	public IExtendedIterator<R> evaluate(IEntityManager manager) {
		if (results == null) {
			String sparql = toQueryString();
			IQuery<?> query = manager.createQuery(sparql, getIncludeInferred());

			((QueryBase<?>) query).initializeFrom(this);
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
		return "";
	}

	@Override
	public IResultDescriptor<R> prefetch(IResultDescriptor<?> descriptor) {
		if (descriptor.getProperty() == null) {
			throw new IllegalArgumentException("Iterator has an invalid property.");
		}
		return prefetch(descriptor.getProperty(), descriptor);
	}

	@Override
	public IResultDescriptor<R> prefetch(String property, IResultDescriptor<?> descriptor) {
		// todo implement prefetching
		return this;
	}

	public IResultDescriptor<R> prefetchTypes() {
		// todo
		return this;
	}

	@Override
	public <T> IResultDescriptor<T> restrictResultType(Class<T> resultType, Class<?>... resultTypes) {
		return super.<IResultDescriptor<T>>doRestrictResultType(resultType, resultTypes);
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
