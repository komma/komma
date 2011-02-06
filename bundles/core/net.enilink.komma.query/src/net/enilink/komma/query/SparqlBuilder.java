/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.common.StringUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.parser.sparql.SparqlParser;
import net.enilink.komma.parser.sparql.tree.AbstractGraphNode;
import net.enilink.komma.parser.sparql.tree.BNode;
import net.enilink.komma.parser.sparql.tree.ConstructQuery;
import net.enilink.komma.parser.sparql.tree.Graph;
import net.enilink.komma.parser.sparql.tree.GraphNode;
import net.enilink.komma.parser.sparql.tree.GraphPattern;
import net.enilink.komma.parser.sparql.tree.IriRef;
import net.enilink.komma.parser.sparql.tree.OptionalGraph;
import net.enilink.komma.parser.sparql.tree.PrefixDecl;
import net.enilink.komma.parser.sparql.tree.PropertyList;
import net.enilink.komma.parser.sparql.tree.PropertyPattern;
import net.enilink.komma.parser.sparql.tree.Query;
import net.enilink.komma.parser.sparql.tree.SelectQuery;
import net.enilink.komma.parser.sparql.tree.SolutionModifier;
import net.enilink.komma.parser.sparql.tree.Variable;
import net.enilink.komma.parser.sparql.tree.expr.Expression;
import net.enilink.komma.parser.sparql.tree.visitor.ToStringVisitor;
import net.enilink.komma.parser.sparql.tree.visitor.TreeWalker;
import net.enilink.komma.parser.sparql.tree.visitor.Visitable;

public class SparqlBuilder {
	private static final IriRef RESULT_NODE = new IriRef("urn:komma:Result");
	private static final PropertyPattern PATTERN_TYPE_RESULT_NODE = new PropertyPattern(
			new IriRef(SparqlParser.RDF_TYPE), RESULT_NODE);

	protected Query query;
	protected List<GraphNode> resultNodes;
	protected Set<String> usedVarNames;

	protected SparqlParser parser;
	protected ToStringVisitor toStringVisitor;

	static class VarRenamer extends TreeWalker<Object> {
		private List<Variable> queryVars = new ArrayList<Variable>();
		private Set<String> queryVarNames = new HashSet<String>();
		private Map<String, String> queryVarNameMap = new LinkedHashMap<String, String>();

		public Collection<String> process(Query query,
				Map<String, String> varNameMap, Set<String> usedVarNames) {
			query.accept(this, null);

			if (varNameMap != null) {
				queryVarNameMap.putAll(varNameMap);
			}

			// rename variables
			for (Variable variable : queryVars) {
				String varName = variable.getName();
				String newVarName = mapVarName(varName);
				if (newVarName == null) {
					int i = 1;
					newVarName = varName;
					if (usedVarNames.contains(newVarName)) {
						while (usedVarNames.contains(newVarName)
								|| queryVarNames.contains(newVarName)) {
							newVarName = varName + i++;
						}
					}
					queryVarNameMap.put(varName, newVarName);
				}
				if (!varName.equals(newVarName)) {
					variable.setName(newVarName);
				}
			}

			return queryVarNameMap.values();
		}

		public String mapVarName(String varName) {
			return queryVarNameMap.get(varName);
		}

		@Override
		public Boolean variable(Variable variable, Object value) {
			queryVars.add(variable);
			queryVarNames.add(variable.getName());

			return variable.getPropertyList().accept(this, value);
		}
	}

	public SparqlBuilder(String sparql) {
		query = parseQuery(sparql);
	}

	protected Object parse(Rule rule, String sparql) {
		ParsingResult<Object> result = ReportingParseRunner.run(rule, sparql);

		if (result.hasErrors()) {
			throw new IllegalArgumentException(StringUtils.join(
					result.parseErrors, "---\n"));
		}

		return result.resultValue;
	}

	protected SparqlParser getParser() {
		if (parser == null) {
			parser = Parboiled.createParser(SparqlParser.class);
		}
		return parser;
	}

	protected Query parseQuery(String sparql) {
		return (Query) parse(getParser().Query(), sparql);
	}

	protected void toConstructQuery() {
		List<GraphNode> template = new ArrayList<GraphNode>();
		if (query instanceof SelectQuery) {
			template.addAll(((SelectQuery) query).getProjection());
		}
		if (template.isEmpty()) {
			for (String varName : getUsedVarNames()) {
				template.add(new Variable(varName));
			}
		}
		prepareTemplate(template);

		ConstructQuery constructQuery = new ConstructQuery(template, query
				.getDataset(), query.getGraph(), Collections
				.<SolutionModifier> emptyList());
		constructQuery.setPrologue(query.getPrologue());
		query = constructQuery;
	}

	protected void prepareTemplate(List<GraphNode> template) {
		for (Iterator<GraphNode> it = template.iterator(); it.hasNext();) {
			GraphNode node = it.next();
			if (!(node instanceof Variable)) {
				it.remove();
				continue;
			}

			PropertyList propertyList = new PropertyList();
			propertyList.add(PATTERN_TYPE_RESULT_NODE.copy());
			((AbstractGraphNode) node).setPropertyList(propertyList);
		}
	}

	protected void addPropertyToTemplate(String property, GraphNode object) {
		for (GraphNode resultNode : resultNodes) {
			resultNode.getPropertyList().add(
					new PropertyPattern(new IriRef(property), object
							.copy(false)));
			resultNode.getPropertyList().add(
					new PropertyPattern(new IriRef(property), RESULT_NODE
							.copy(false)));
		}
		if (!object.getPropertyList().isEmpty()) {
			((ConstructQuery) query).getTemplate().add(object.copy(true));
		}
	}

	public SparqlBuilder optional(String property, String param, String sparql) {
		return optional(property, param, null, sparql);
	}

	public SparqlBuilder optional(String property, String variable,
			String param, String sparql) {
		return optional(property, variable, param, parseQuery(sparql));
	}

	public SparqlBuilder optional(String property, String variable,
			String param, SparqlBuilder other) {
		return optional(property, variable, param, other.query);
	}

	protected Set<String> getUsedVarNames() {
		if (usedVarNames == null) {
			usedVarNames = new LinkedHashSet<String>();
			usedVarNames.addAll(new VarRenamer().process(query, null,
					usedVarNames));
		}
		return usedVarNames;
	}

	protected SparqlBuilder optional(String property, String variable,
			String param, Query other) {
		if (!(query instanceof ConstructQuery)) {
			toConstructQuery();
		}
		if (resultNodes == null) {
			resultNodes = new ArrayList<GraphNode>();
			for (GraphNode node : ((ConstructQuery) query).getTemplate()) {
				for (PropertyPattern pattern : node.getPropertyList()) {
					if (RESULT_NODE.equals(pattern.getObject())) {
						resultNodes.add(node);
						break;
					}
				}
			}
		}

		VarRenamer otherVarRenamer = new VarRenamer();
		Map<String, String> nodeNameMap = null;
		if (param != null && !resultNodes.isEmpty()) {
			nodeNameMap = new HashMap<String, String>();
			nodeNameMap.put(param, ((Variable) resultNodes.get(0)).getName());
		}

		Collection<String> otherVarNames = otherVarRenamer.process(other,
				nodeNameMap, getUsedVarNames());
		getUsedVarNames().addAll(otherVarNames);

		if (property != null) {
			if (variable != null) {
				addPropertyToTemplate(property, new Variable(otherVarRenamer
						.mapVarName(variable)));

				if (other instanceof ConstructQuery) {
					for (GraphNode node : ((ConstructQuery) other)
							.getTemplate()) {
						node = node.copy(true);
						node.getPropertyList().remove(PATTERN_TYPE_RESULT_NODE);
						((ConstructQuery) query).getTemplate().add(node);
					}
				}
			} else {
				if (other instanceof ConstructQuery) {
					if (((ConstructQuery) other).getTemplate().isEmpty()) {
						for (String varName : otherVarNames) {
							addPropertyToTemplate(property, new Variable(
									varName));
						}
					} else {
						for (GraphNode node : ((ConstructQuery) other)
								.getTemplate()) {
							addPropertyToTemplate(property, node);
						}
					}
				} else if (((SelectQuery) other).getProjection().isEmpty()) {
					for (String varName : otherVarNames) {
						addPropertyToTemplate(property, new Variable(varName));
					}
				} else {
					for (GraphNode node : ((SelectQuery) other).getProjection()) {
						addPropertyToTemplate(property, node);
					}
				}
			}
		} else if (other instanceof ConstructQuery) {
			// add template from other query
			((ConstructQuery) query).getTemplate().addAll(
					((ConstructQuery) other).getTemplate());
		}

		query.getPrologue().getPrefixDecls().addAll(
				other.getPrologue().getPrefixDecls());
		removeDuplicates(query.getPrologue().getPrefixDecls());

		query.getDataset().add(other.getDataset());
		// query.setGraph(new UnionGraph(Arrays.asList(query.getGraph(), other
		// .getGraph())));

		OptionalGraph optionalGraph = other.getGraph() instanceof OptionalGraph ? (OptionalGraph) other
				.getGraph()
				: new OptionalGraph(other.getGraph());
		query.setGraph(new GraphPattern(new ArrayList<Graph>(Arrays.asList(
				query.getGraph(), optionalGraph)), Collections
				.<Expression> emptyList()));

		return this;
	}

	public SparqlBuilder fetchTypes() {
		if (!(query instanceof ConstructQuery)) {
			toConstructQuery();
		}

		final Map<GraphNode, GraphPattern> nodesWithoutType = new HashMap<GraphNode, GraphPattern>();
		TreeWalker<GraphNode> nodeFinder = new TreeWalker<GraphNode>() {
			public Boolean propertyList(PropertyList propertyList,
					GraphNode subject) {
				nodesWithoutType.put(subject, null);
				for (PropertyPattern pattern : propertyList) {
					if (pattern.getPredicate() instanceof IriRef
							&& ((IriRef) pattern.getPredicate()).getIri()
									.equals(SparqlParser.RDF_TYPE)) {
						if (RESULT_NODE.equals(pattern.getObject())) {
							continue;
						}
						nodesWithoutType.remove(subject);
					}
					nodesWithoutType.put(pattern.getObject(), null);
					pattern.getObject().accept(this, pattern.getObject());
				}
				return true;
			}
		};
		for (GraphNode node : ((ConstructQuery) query).getTemplate()) {
			node.accept(nodeFinder, node);
		}
		TreeWalker<GraphNode> patternFinder = new TreeWalker<GraphNode>() {
			class PatternInfo {
				int depth;
				int optionals;

				PatternInfo(int depth, int optionals) {
					this.depth = depth;
					this.optionals = optionals;
				}
			}

			Stack<GraphPattern> patterns = new Stack<GraphPattern>();
			Map<GraphPattern, PatternInfo> patternInfos = new HashMap<GraphPattern, PatternInfo>();
			int depth;
			int optionals;

			@Override
			public Boolean bNode(BNode bNode, GraphNode data) {
				return super.bNode(bNode, bNode);
			}

			@Override
			public Boolean iriRef(IriRef iriRef, GraphNode data) {
				return super.iriRef(iriRef, iriRef);
			}

			@Override
			public Boolean variable(Variable variable, GraphNode data) {
				return super.variable(variable, variable);
			}

			@Override
			public Boolean optionalGraph(OptionalGraph optionalGraph,
					GraphNode data) {
				try {
					optionals++;
					return super.optionalGraph(optionalGraph, data);
				} finally {
					optionals--;
				}
			}

			@Override
			public Boolean graphPattern(GraphPattern graphPattern,
					GraphNode data) {
				try {
					depth++;
					patterns.push(graphPattern);

					patternInfos.put(graphPattern, new PatternInfo(depth,
							optionals));
					return super.graphPattern(graphPattern, data);
				} finally {
					patterns.pop();
					depth--;
				}
			}

			public Boolean propertyList(PropertyList propertyList,
					GraphNode subject) {
				if (nodesWithoutType.containsKey(subject)) {
					GraphPattern other = nodesWithoutType.get(subject);
					if (other != null) {
						// take best pattern for insertion
						PatternInfo info = patternInfos.get(other);
						if (info.optionals > optionals || info.depth > depth) {
							nodesWithoutType.put(subject, patterns.peek());
						}
					} else {
						nodesWithoutType.put(subject, patterns.peek());
					}
				}
				for (PropertyPattern pattern : propertyList) {
					pattern.getObject().accept(this, pattern.getObject());
				}
				return true;
			}
		};
		query.getGraph().accept(patternFinder, null);

		int i = 1;
		for (Map.Entry<GraphNode, GraphPattern> entry : nodesWithoutType
				.entrySet()) {
			if (entry.getKey().equals(RESULT_NODE)) {
				continue;
			}

			GraphNode copy = entry.getKey().copy(false);

			String nodeName = toString(copy);
			String typeVar;
			do {
				typeVar = "preloadedType_" + i++;
			} while (getUsedVarNames().contains(typeVar));

			getUsedVarNames().add(typeVar);

			parse(parser.PropertyListNotEmpty(copy), "a ?" + typeVar);

			((ConstructQuery) query).getTemplate().add(copy);

			((GraphPattern) entry.getValue()).getPatterns().add(
					(Graph) parse(parser.OptionalGraphPattern(), "OPTIONAL {"
							+ nodeName + " a ?" + typeVar + " . FILTER isIRI(?"
							+ typeVar + ")}}"));
		}

		// if (query.getGraph() instanceof GraphPattern) {
		// ((GraphPattern) query.getGraph()).getPatterns().addAll(newPatterns);
		// } else {
		// newPatterns.add(0, query.getGraph());
		// query.setGraph(new GraphPattern(newPatterns, Collections
		// .<Expression> emptyList()));
		// }

		return this;
	}

	protected void removeDuplicates(List<PrefixDecl> prefixDecls) {
		Set<String> prefixes = new HashSet<String>();
		for (Iterator<PrefixDecl> it = prefixDecls.iterator(); it.hasNext();) {
			PrefixDecl prefixDecl = it.next();
			if (!prefixes.add(prefixDecl.getPrefix() + ":<"
					+ prefixDecl.getIri().getIri() + ">")) {
				it.remove();
			}
		}
	}

	protected String toString(Visitable value) {
		return value.accept(new ToStringVisitor(), new StringBuilder())
				.toString();
	}

	@Override
	public String toString() {
		if (query == null) {
			return "";
		}

		return toString(query);
	}
}
