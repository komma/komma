/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for working with {@link IGraph} instances.
 * 
 */
class GraphUtil {

	/**
	 * Compares two graphs, defined by two statement collections, and returns
	 * <tt>true</tt> if they are equal. Models are equal if they contain the
	 * same set of statements. Blank node IDs are not relevant for graph
	 * equality, they are mapped from one graph to the other by using the
	 * attached properties.
	 */
	public static boolean equals(Iterable<? extends IStatement> graph1,
			Iterable<? extends IStatement> graph2) {
		// Filter duplicates
		Set<IStatement> set1 = new LinkedHashSet<IStatement>();
		for (IStatement stmt : graph1) {
			set1.add(stmt);
		}

		Set<IStatement> set2 = new LinkedHashSet<IStatement>();
		for (IStatement stmt : graph2) {
			set2.add(stmt);
		}

		return equals(set1, set2);
	}

	/**
	 * Compares two graphs, defined by two statement collections, and returns
	 * <tt>true</tt> if they are equal. Models are equal if they contain the
	 * same set of statements. Blank node IDs are not relevant for graph
	 * equality, they are mapped from one graph to the other by using the
	 * attached properties.
	 */
	public static boolean equals(Set<? extends IStatement> graph1,
			Set<? extends IStatement> graph2) {
		// Compare the number of statements in both sets
		if (graph1.size() != graph2.size()) {
			return false;
		}

		return isSubsetInternal(graph1, graph2);
	}

	/**
	 * Compares two graphs, defined by two statement collections, and returns
	 * <tt>true</tt> if the first graph is a subset of the second graph.
	 */
	public static boolean isSubset(Iterable<? extends IStatement> graph1,
			Iterable<? extends IStatement> graph2) {
		// Filter duplicates
		Set<IStatement> set1 = new LinkedHashSet<IStatement>();
		for (IStatement stmt : graph1) {
			set1.add(stmt);
		}

		Set<IStatement> set2 = new LinkedHashSet<IStatement>();
		for (IStatement stmt : graph2) {
			set2.add(stmt);
		}

		return isSubset(set1, set2);
	}

	/**
	 * Compares two graphs, defined by two statement collections, and returns
	 * <tt>true</tt> if the first graph is a subset of the second graph.
	 */
	public static boolean isSubset(Set<? extends Statement> graph1,
			Set<? extends Statement> graph2) {
		// Compare the number of statements in both sets
		if (graph1.size() > graph2.size()) {
			return false;
		}

		return isSubsetInternal(graph1, graph2);
	}

	private static boolean isSubsetInternal(Set<? extends IStatement> graph1,
			Set<? extends IStatement> graph2) {
		// try to create a full blank node mapping
		return matchModels(graph1, graph2);
	}

	private static boolean matchModels(Set<? extends IStatement> graph1,
			Set<? extends IStatement> graph2) {
		// Compare statements without blank nodes first, save the rest for later
		List<IStatement> graph1BNodes = new ArrayList<IStatement>(graph1.size());

		for (IStatement st : graph1) {
			if (st.getSubject().getURI() == null
					|| (st.getObject() instanceof IReference && ((IReference) st
							.getObject()).getURI() == null)) {
				graph1BNodes.add(st);
			} else {
				if (!graph2.contains(st)) {
					return false;
				}
			}
		}

		return matchModels(graph1BNodes, graph2,
				new HashMap<IReference, IReference>(), 0);
	}

	/**
	 * A recursive method for finding a complete mapping between blank nodes in
	 * graph1 and blank nodes in graph2. The algorithm does a depth-first search
	 * trying to establish a mapping for each blank node occurring in graph1.
	 * 
	 * @param graph1
	 * @param graph2
	 * @param bNodeMapping
	 * @param idx
	 * @return true if a complete mapping has been found, false otherwise.
	 */
	private static boolean matchModels(List<? extends IStatement> graph1,
			Iterable<? extends IStatement> graph2,
			Map<IReference, IReference> bNodeMapping, int idx) {
		boolean result = false;

		if (idx < graph1.size()) {
			IStatement st1 = graph1.get(idx);

			List<IStatement> matchingStats = findMatchingStatements(st1,
					graph2, bNodeMapping);

			for (IStatement st2 : matchingStats) {
				// Map bNodes in st1 to bNodes in st2
				Map<IReference, IReference> newBNodeMapping = new HashMap<IReference, IReference>(
						bNodeMapping);

				if (st1.getSubject().getURI() == null
						&& st2.getSubject().getURI() == null) {
					newBNodeMapping.put(st1.getSubject(), st2.getSubject());
				}

				if (st1.getObject() instanceof IReference
						&& ((IReference) st1.getObject()).getURI() == null
						&& st2.getObject() instanceof IReference
						&& ((IReference) st2.getObject()).getURI() == null) {
					newBNodeMapping.put((IReference) st1.getObject(),
							(IReference) st2.getObject());
				}

				// FIXME: this recursive implementation has a high risk of
				// triggering a stack overflow

				// Enter recursion
				result = matchModels(graph1, graph2, newBNodeMapping, idx + 1);

				if (result == true) {
					// graphs match, look no further
					break;
				}
			}
		} else {
			// All statements have been mapped successfully
			result = true;
		}

		return result;
	}

	private static List<IStatement> findMatchingStatements(IStatement st,
			Iterable<? extends IStatement> graph,
			Map<IReference, IReference> bNodeMapping) {
		List<IStatement> result = new ArrayList<IStatement>();

		for (IStatement graphSt : graph) {
			if (statementsMatch(st, graphSt, bNodeMapping)) {
				// All components possibly match
				result.add(graphSt);
			}
		}

		return result;
	}

	private static boolean statementsMatch(IStatement st1, IStatement st2,
			Map<IReference, IReference> bNodeMapping) {
		IReference pred1 = st1.getPredicate();
		IReference pred2 = st2.getPredicate();

		if (!pred1.equals(pred2)) {
			// predicates don't match
			return false;
		}

		IReference subj1 = st1.getSubject();
		IReference subj2 = st2.getSubject();

		if (subj1.getURI() == null && subj2.getURI() == null) {
			IReference mappedBNode = bNodeMapping.get(subj1);

			if (mappedBNode != null) {
				// bNode 'subj1' was already mapped to some other bNode
				if (!subj2.equals(mappedBNode)) {
					// 'subj1' and 'subj2' do not match
					return false;
				}
			} else {
				// 'subj1' was not yet mapped. we need to check if 'subj2' is a
				// possible mapping candidate
				if (bNodeMapping.containsValue(subj2)) {
					// 'subj2' is already mapped to some other value.
					return false;
				}
			}
		} else {
			// subjects are not (both) bNodes
			if (!subj1.equals(subj2)) {
				return false;
			}
		}

		Object obj1 = st1.getObject();
		Object obj2 = st2.getObject();

		if (obj1 instanceof IReference && ((IReference) obj1).getURI() == null
				&& obj2 instanceof IReference
				&& ((IReference) obj2).getURI() == null) {
			IReference mappedBNode = bNodeMapping.get(obj1);

			if (mappedBNode != null) {
				// bNode 'obj1' was already mapped to some other bNode
				if (!obj2.equals(mappedBNode)) {
					// 'obj1' and 'obj2' do not match
					return false;
				}
			} else {
				// 'obj1' was not yet mapped. we need to check if 'obj2' is a
				// possible mapping candidate
				if (bNodeMapping.containsValue(obj2)) {
					// 'obj2' is already mapped to some other value.
					return false;
				}
			}
		} else {
			// objects are not (both) bNodes
			if (!obj1.equals(obj2)) {
				return false;
			}
		}

		return true;
	}
}