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

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IResultDescriptor;
import net.enilink.komma.em.internal.query.QueryBase;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ResultDescriptor<R> extends QueryBase<IResultDescriptor<R>> implements IResultDescriptor<R>, Cloneable {
	private static final String RESULT_NODE = "<komma:Result>";

	protected static Pattern PREFIX_DECL = Pattern
			.compile("prefix\\s+([^:]+)?\\s*:\\s*<((?:[^>\\\\]|\\\\[tbnrf\\\\>])+)>", Pattern.CASE_INSENSITIVE);

	protected static Pattern STRING_OR_IRI = Pattern.compile(//
			"'''((:?'|'')?(?:[^'\\\\]|\\\\[tbnrf\\\\\"']))*'''|" + //
					"\"\"\"((:?\"|\"\")?(?:[^'\\\\]|\\\\[tbnrf\\\\\"']))*\"\"\"|" + //
					"'([^'\\\\]|\\\\[tbnrf\\\\\"'])*'|" + //
					"\"([^\"\\\\]|\\\\[tbnrf\\\\\"'])*\"|" + //
					"<([^>\\\\]|\\\\[tbnrf\\\\>])*>");

	// this is the SPARQL definition of PN_CHARS_BASE, but excludes the '<' character
	// https://www.w3.org/TR/sparql11-query/
	protected static Pattern PN_CHARS_BASE = Pattern.compile(
			"[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02FF\u0370-\u037D\u037F-\u1FFF\u200C-\u200D\u2070-\u218F\u2C00-\u2FEF\u3001-\uD7FF\uF900-\uFDCF\uFDF0-\uFFFD&&[^<]]");
	protected static Pattern PN_CHARS_U = Pattern.compile(PN_CHARS_BASE + "|[_]");
	protected static Pattern PN_CHARS = Pattern.compile(PN_CHARS_U + "|-|[0-9]|\u00B7|[\u0300-\u036F\u203F-\u2040]");

	protected static Pattern PN_PREFIX = Pattern.compile(PN_CHARS_BASE + "(?:(?:" + PN_CHARS + "|[.])*" + PN_CHARS + ")?");
	protected static Pattern PNAME_NS = Pattern.compile("(" + PN_PREFIX + ")?:");
	protected static Pattern VARNAME = Pattern
			.compile("(?:" + PN_CHARS_U + "|[0-9])(?:" + PN_CHARS_U + "|[0-9\u00B7\u0300-\u036F\u203F-\u2040])*");
	protected static Pattern VAR = Pattern.compile("[?$]" + "(" + VARNAME + ")");
	protected static Pattern PERCENT = Pattern.compile("%[0-9A-Fa-f]{2}");
	protected static Pattern PN_LOCAL_ESC = Pattern.compile("\\\\[_~.\\-!$&'()*+,;=/?#@%]");
	protected static Pattern PLX = Pattern.compile(PERCENT + "|" + PN_LOCAL_ESC);
	protected static Pattern PN_LOCAL = Pattern.compile("(?:" + PN_CHARS_U + "|[:0-9]|" + PLX + ")((?:" + PN_CHARS
			+ "|[.:]|" + PLX + ")*(?:" + PN_CHARS + "|:|" + PLX + "))?");
	protected static Pattern PNAME_LN = Pattern.compile(PNAME_NS + "(" + PN_LOCAL + ")");
	protected boolean includeInferred;

	protected Map<String, Object> parameters;

	protected IExtendedIterator<R> results;

	protected String property;

	protected String parameterVariable;

	protected String resultVariable;

	protected QueryFragment queryFragment;

	protected String constructTemplate;

	public ResultDescriptor(String sparql) {
		queryFragment = new QueryFragment(sparql);

		Matcher m = Pattern.compile("construct\\s*\\{", Pattern.CASE_INSENSITIVE).matcher(sparql);
		boolean isConstruct = m.find();
		if (isConstruct) {
			String template = sparql.substring(m.end(), sparql.indexOf("}"));
			constructTemplate = processExcludingLiterals(template,
					(part) -> renameVariables(part, queryFragment.varNameMap, queryFragment.usedVariables)).trim();
		} else {
			constructTemplate = queryFragment.selectOrTemplateVars.stream().map(v ->
							"?" + v + " a " + RESULT_NODE + " . ")
					.collect(Collectors.joining());
		}
		this.resultVariable = queryFragment.selectOrTemplateVars.stream().findFirst().orElse(null);
	}

	public ResultDescriptor(String sparql, String property, String resultVariable, String parameterVariable) {
		this(sparql);
		this.property = property;
		this.resultVariable = resultVariable;
		this.parameterVariable = parameterVariable;
	}

	protected static String processExcludingLiterals(String sparql, Function<String, String> replaceFunction) {
		StringBuilder sb = new StringBuilder();
		Matcher m = STRING_OR_IRI.matcher(sparql);
		int pos = 0;
		while (m.find()) {
			String part = sparql.substring(pos, m.start());
			sb.append(replaceFunction.apply(part));
			sb.append(sparql, m.start(), m.end() + 1);
			pos = m.end() + 1;
		}
		if (pos < sparql.length()) {
			sb.append(replaceFunction.apply(sparql.substring(pos)));
		}
		return sb.toString();
	}

	protected static String expandPrefixedNames(String part, Map<String, String> prefixDeclarations) {
		StringBuilder sb = new StringBuilder();
		Matcher m = PNAME_LN.matcher(part);
		while (m.find()) {
			String prefix = m.group(1);
			String local = m.group(2);
			String namespace = prefixDeclarations.get(prefix == null ? "" : prefix);
			if (namespace != null) {
				m.appendReplacement(sb, Matcher.quoteReplacement("<" + namespace + local + ">"));
			} else {
				m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	protected static String renameVariables(String part, Map<String, String> varNameMap, Set<String> usedVariables) {
		StringBuilder sb = new StringBuilder();
		Matcher m = VAR.matcher(part);
		while (m.find()) {
			String varName = m.group(1);
			String newVarName = varNameMap.get(varName);
			if (newVarName == null) {
				newVarName = varName;

				int i = 1;
				while (usedVariables.contains(newVarName)) {
					newVarName = varName + i++;
				}
				usedVariables.add(newVarName);
				varNameMap.put(varName, newVarName);
			}
			m.appendReplacement(sb, Matcher.quoteReplacement("?" + newVarName));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	@Override
	public <T> IResultDescriptor<T> bindResultType(Class<T> resultType, Class<?>... resultTypes) {
		return super.doBindResultType(resultType, resultTypes);
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
		return (constructTemplate.length() == 0 ? "ask" : "construct { " + constructTemplate + " } where") +
				" { " +
				queryFragment.whereClause +
				" } " + queryFragment.modifiers;
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
		String paramVarName = descriptor.getParameterVariable();
		Map<String, String> varNameMap = new HashMap<>();
		// unify parameter variable with the result variable of this descriptor
		varNameMap.put(paramVarName, getResultVariable());
		QueryFragment newQuery = queryFragment.optional(descriptor.toQueryString(), varNameMap);
		try {
			ResultDescriptor newDescriptor = (ResultDescriptor) clone();
			newDescriptor.queryFragment = newQuery;
			String renamedPrefetchResultVariable = newQuery.varNameMap.get(descriptor.getResultVariable());
			newDescriptor.constructTemplate += "?" + getResultVariable() + " <" + property + "> ?"
					+ renamedPrefetchResultVariable + " . ";
			return newDescriptor;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T> IResultDescriptor<T> restrictResultType(Class<T> resultType, Class<?>... resultTypes) {
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
			parameters = new HashMap<>();
		}
		parameters.put(name, value);
		return this;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	static class QueryFragment {
		protected final String whereClause;

		protected final String modifiers;

		protected final Set<String> usedVariables;

		protected final Map<String, String> varNameMap;

		protected List<String> selectOrTemplateVars;

		public QueryFragment(String whereClause, String modifiers, Set<String> usedVariables, Map<String, String> varNameMap) {
			this.whereClause = whereClause;
			this.modifiers = modifiers;
			this.usedVariables = usedVariables;
			this.varNameMap = varNameMap;
		}

		QueryFragment(String sparql) {
			this(sparql, new HashSet<>(), new HashMap<>());
		}

		QueryFragment(String sparql, Set<String> usedVariables, Map<String, String> varNameMap) {
			this.usedVariables = usedVariables;
			this.varNameMap = varNameMap;

			Map<String, String> prefixDeclarations = new HashMap<>();
			int endPrefixDecls = extractPrefixDeclarations(sparql, prefixDeclarations);

			String sparqlWithoutPrefixes = sparql.substring(endPrefixDecls);
			boolean isConstruct = sparqlWithoutPrefixes.matches("(?i)\\s*construct.*");

			int whereStart = isConstruct ? sparqlWithoutPrefixes.indexOf('{', sparqlWithoutPrefixes.indexOf('}')) :
					sparqlWithoutPrefixes.indexOf('{');
			int whereEnd = sparqlWithoutPrefixes.lastIndexOf('}');

			String whereClauseOrig = sparqlWithoutPrefixes.substring(whereStart + 1, whereEnd);
			String modifiersOrig = sparqlWithoutPrefixes.substring(whereEnd + 1);

			// variables in where clause or in construct template
			this.selectOrTemplateVars = extractVars(sparqlWithoutPrefixes.substring(0, whereStart));

			// expand all prefixed names to absolute IRIs
			String whereClauseOrigExpandedPrefixes = processExcludingLiterals(whereClauseOrig,
					(part) -> expandPrefixedNames(part, prefixDeclarations));

			// make all variables unique
			this.whereClause = processExcludingLiterals(whereClauseOrigExpandedPrefixes,
					(part) -> renameVariables(part, varNameMap, usedVariables)).trim();
			this.modifiers = processExcludingLiterals(modifiersOrig,
					(part) -> renameVariables(part, varNameMap, usedVariables)).trim();
		}

		protected static List<String> extractVars(String prologue) {
			Set<String> vars = new LinkedHashSet<>();
			Matcher m = VAR.matcher(prologue);
			while (m.find()) {
				vars.add(m.group(1));
			}
			return new ArrayList<>(vars);
		}

		protected static int extractPrefixDeclarations(String prologue, Map<String, String> prefixDeclarations) {
			Matcher m = PREFIX_DECL.matcher(prologue);
			int pos = 0;
			while (m.find()) {
				String prefix = m.group(1);
				String uri = m.group(2);
				prefixDeclarations.put(prefix == null ? "" : prefix, uri);
				pos = m.end() + 1;
			}
			return pos;
		}

		protected QueryFragment optional(String otherQuery, Map<String, String> varNameMap) {
			QueryFragment otherQueryParsed = new QueryFragment(otherQuery, new HashSet<>(this.usedVariables),
					varNameMap);

			String newWhereClause = whereClause + " optional {" + otherQueryParsed.whereClause + "}";
			String newModifiers = modifiers + " " + otherQueryParsed.modifiers;
			Set<String> allUsedVariables = new HashSet<>(usedVariables);
			allUsedVariables.addAll(otherQueryParsed.usedVariables);
			Map<String, String> allVarsNameMap = new HashMap<>(varNameMap);
			allVarsNameMap.putAll(otherQueryParsed.varNameMap);

			return new QueryFragment(newWhereClause, newModifiers, allUsedVariables, allVarsNameMap);
		}
	}
}