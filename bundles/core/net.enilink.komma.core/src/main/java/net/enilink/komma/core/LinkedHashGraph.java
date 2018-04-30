/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.core;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import net.enilink.commons.iterator.Filter;
import net.enilink.commons.iterator.FilterIterator;

/**
 * Implementation of the {@link IGraph} interface using an underlying
 * {@link LinkedHashSet}.
 */
public class LinkedHashGraph extends AbstractSet<IStatement> implements IGraph {

	private static final long serialVersionUID = -9161104123818983614L;

	static final IReference[] NULL_CTX = new IReference[] { null };

	transient Map<Object, GraphNode<Object>> values;

	transient Set<GraphStatement> statements;

	public LinkedHashGraph() {
		values = new HashMap<>();
		statements = new LinkedHashSet<>();
	}

	public LinkedHashGraph(Collection<? extends IStatement> c) {
		values = new HashMap<>(c.size() * 2);
		statements = new LinkedHashSet<>(c.size());
		addAll(c);
	}

	public LinkedHashGraph(int size) {
		values = new HashMap<>(size * 2);
		statements = new LinkedHashSet<>(size);
	}

	@Override
	public int size() {
		return statements.size();
	}

	@Override
	public boolean add(IStatement st) {
		return add(st.getSubject(), st.getPredicate(), st.getObject(),
				st.getContext());
	}

	public boolean add(IReference subj, IReference pred, Object obj,
			IReference... contexts) {
		IReference[] ctxs = notNull(contexts);
		if (ctxs.length == 0) {
			ctxs = NULL_CTX;
		}
		boolean changed = false;
		for (IReference ctx : ctxs) {
			GraphNode<IReference> s = asNode(subj);
			GraphNode<IReference> p = asNode(pred);
			GraphNode<Object> o = asNode(obj);
			GraphNode<IReference> c = asNode(ctx);
			GraphStatement st = new GraphStatement(s, p, o, c, false);
			changed |= addGraphStatement(st);
		}
		return changed;
	}

	@Override
	public void clear() {
		values.clear();
		statements.clear();
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Statement) {
			Iterator<?> iter = find((Statement) o);
			if (iter.hasNext()) {
				iter.next();
				iter.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Statement) {
			return find((Statement) o).hasNext();
		}
		return false;
	}

	@Override
	public Iterator<IStatement> iterator() {
		return match(null, null, null);
	}

	public boolean contains(IReference subj, IReference pred, Object obj,
			IReference... contexts) {
		return match(subj, pred, obj, contexts).hasNext();
	}

	public boolean remove(IReference subj, IReference pred, Object obj,
			IReference... contexts) {
		Iterator<?> iter = match(subj, pred, obj, contexts);
		if (!iter.hasNext()) {
			return false;
		}
		while (iter.hasNext()) {
			iter.next();
			iter.remove();
		}
		return true;
	}

	public boolean clear(IReference... contexts) {
		return remove(null, null, null, contexts);
	}

	public IGraph filter(IReference subj, IReference pred, Object obj,
			IReference... contexts) {
		return new FilteredGraph(subj, pred, obj, contexts);
	}

	public Set<IReference> subjects() {
		return subjects(null, null);
	}

	public Set<IReference> predicates() {
		return predicates(null, null);
	}

	public Set<Object> objects() {
		return objects(null, null);
	}

	public Set<IReference> contexts() {
		return contexts(null, null, null);
	}

	public Object objectValue() throws KommaException {
		Iterator<Object> iter = objects().iterator();
		if (iter.hasNext()) {
			Object obj = iter.next();
			if (iter.hasNext()) {
				throw new KommaException();
			}
			return obj;
		}
		return null;
	}

	public ILiteral objectLiteral() throws KommaException {
		Object obj = objectValue();
		if (obj == null) {
			return null;
		}
		if (obj instanceof ILiteral) {
			return (ILiteral) obj;
		}
		throw new KommaException();
	}

	public IReference objectReference() throws KommaException {
		Object obj = objectValue();
		if (obj == null) {
			return null;
		}
		if (obj instanceof IReference) {
			return (IReference) obj;
		}
		throw new KommaException();
	}

	public String objectString() throws KommaException {
		Object obj = objectValue();
		if (obj == null) {
			return null;
		}
		if (obj instanceof ILiteral) {
			return ((ILiteral) obj).getLabel();
		}
		return obj.toString();
	}

	@Override
	public void rename(IReference source, IReference target) {
		LinkedHashGraph.rename(this, source, target);
	}

	static void rename(IGraph graph, IReference source, IReference target) {
		Iterator<IStatement> it = graph.iterator();
		List<IStatement> newStatements = new ArrayList<>();
		while (it.hasNext()) {
			IStatement stmt = it.next();
			IReference s = stmt.getSubject();
			IReference p = stmt.getPredicate();
			Object o = stmt.getObject();
			boolean changed = false;
			if (source.equals(s)) {
				s = target;
				changed = true;
			}
			if (source.equals(p)) {
				p = target;
				changed = true;
			}
			if (source.equals(o)) {
				o = target;
				changed = true;
			}
			if (changed) {
				it.remove();
			}
			newStatements.add(new Statement(s, p, o, stmt.getContext()));
		}
		graph.addAll(newStatements);
	}

	@Override
	public int hashCode() {
		return size();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof IGraph) {
			IGraph graph = (IGraph) o;
			return GraphUtil.equals(this, graph);
		}
		return false;
	}

	Set<IReference> contexts(final IReference subj, final IReference pred,
			final Object obj) {
		return new ObjectSet<IReference>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof IReference || o == null) {
					return LinkedHashGraph.this.contains(subj, pred, obj,
							(IReference) o);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof IReference || o == null) {
					return LinkedHashGraph.this.remove(subj, pred, obj,
							(IReference) o);
				}
				return false;
			}

			@Override
			public boolean add(IReference ctx) {
				if (subj == null || pred == null || obj == null) {
					throw new UnsupportedOperationException(
							"Incomplete statement");
				}
				if (contains(ctx)) {
					return false;
				}
				return LinkedHashGraph.this.add(subj, pred, obj, ctx);
			}

			@Override
			public void clear() {
				LinkedHashGraph.this.remove(subj, pred, obj);
			}

			@Override
			protected GraphIterator statementIterator() {
				return match(subj, pred, obj);
			}

			@Override
			protected GraphNode<IReference> node(GraphStatement st) {
				return st.ctx;
			}

			@Override
			protected Set<GraphStatement> set(GraphNode<IReference> node) {
				return node.contexts;
			}
		};
	}

	Set<Object> objects(final IReference subj, final IReference pred,
			final IReference... contexts) {
		return new ObjectSet<Object>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof Object) {
					return LinkedHashGraph.this.contains(subj, pred,
							(Object) o, contexts);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof Object) {
					return LinkedHashGraph.this.remove(subj, pred, (Object) o,
							contexts);
				}
				return false;
			}

			@Override
			public boolean add(Object obj) {
				if (subj == null || pred == null) {
					throw new UnsupportedOperationException(
							"Incomplete statement");
				}
				if (contains(obj)) {
					return false;
				}
				return LinkedHashGraph.this.add(subj, pred, obj, contexts);
			}

			@Override
			public void clear() {
				LinkedHashGraph.this.remove(subj, pred, null, contexts);
			}

			@Override
			protected GraphIterator statementIterator() {
				return match(subj, pred, null, contexts);
			}

			@Override
			protected GraphNode<Object> node(GraphStatement st) {
				return st.obj;
			}

			@Override
			protected Set<GraphStatement> set(GraphNode<Object> node) {
				return node.objects;
			}
		};
	}

	Set<IReference> predicates(final IReference subj, final Object obj,
			final IReference... contexts) {
		return new ObjectSet<IReference>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof IReference) {
					return LinkedHashGraph.this.contains(subj, (IReference) o,
							obj, contexts);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof IReference) {
					return LinkedHashGraph.this.remove(subj, (IReference) o,
							obj, contexts);
				}
				return false;
			}

			@Override
			public boolean add(IReference pred) {
				if (subj == null || obj == null) {
					throw new UnsupportedOperationException(
							"Incomplete statement");
				}
				if (contains(pred)) {
					return false;
				}
				return LinkedHashGraph.this.add(subj, pred, obj, contexts);
			}

			@Override
			public void clear() {
				LinkedHashGraph.this.remove(subj, null, obj, contexts);
			}

			@Override
			protected GraphIterator statementIterator() {
				return match(subj, null, obj, contexts);
			}

			@Override
			protected GraphNode<IReference> node(GraphStatement st) {
				return st.pred;
			}

			@Override
			protected Set<GraphStatement> set(GraphNode<IReference> node) {
				return node.predicates;
			}
		};
	}

	Set<IReference> subjects(final IReference pred, final Object obj,
			final IReference... contexts) {
		return new ObjectSet<IReference>() {

			@Override
			public boolean contains(Object o) {
				if (o instanceof IReference) {
					return LinkedHashGraph.this.contains((IReference) o, pred,
							obj, contexts);
				}
				return false;
			}

			@Override
			public boolean remove(Object o) {
				if (o instanceof IReference) {
					return LinkedHashGraph.this.remove((IReference) o, pred,
							obj, contexts);
				}
				return false;
			}

			@Override
			public boolean add(IReference subj) {
				if (pred == null || obj == null) {
					throw new UnsupportedOperationException(
							"Incomplete statement");
				}
				if (contains(subj)) {
					return false;
				}
				return LinkedHashGraph.this.add(subj, pred, obj, contexts);
			}

			@Override
			public void clear() {
				LinkedHashGraph.this.remove(null, pred, obj, contexts);
			}

			@Override
			protected GraphIterator statementIterator() {
				return match(null, pred, obj, contexts);
			}

			@Override
			protected GraphNode<IReference> node(GraphStatement st) {
				return st.subj;
			}

			@Override
			protected Set<GraphStatement> set(GraphNode<IReference> node) {
				return node.subjects;
			}
		};
	}

	protected GraphIterator match(IReference subj, IReference pred, Object obj,
			IReference... contexts) {
		assert contexts != null;
		Set<GraphStatement> s = null;
		Set<GraphStatement> p = null;
		Set<GraphStatement> o = null;
		if (subj != null) {
			if (!values.containsKey(subj)) {
				return emptyGraphIterator();
			}
			s = values.get(subj).subjects;
		}
		if (pred != null) {
			if (!values.containsKey(pred)) {
				return emptyGraphIterator();
			}
			p = values.get(pred).predicates;
		}
		if (obj != null) {
			if (!values.containsKey(obj)) {
				return emptyGraphIterator();
			}
			o = values.get(obj).objects;
		}
		Set<GraphStatement> set;
		contexts = notNull(contexts);
		if (contexts.length == 1) {
			if (!values.containsKey(contexts[0])) {
				return emptyGraphIterator();
			}
			Set<GraphStatement> c = values.get(contexts[0]).contexts;
			set = smallest(statements, s, p, o, c);
		} else {
			set = smallest(statements, s, p, o);
		}
		return new GraphIterator(new PatternIterator<>(set.iterator(), subj,
				pred, obj, contexts), set);
	}

	boolean matches(IStatement st, IReference subj, IReference pred,
			Object obj, IReference... contexts) {
		if (subj != null && !subj.equals(st.getSubject())) {
			return false;
		}
		if (pred != null && !pred.equals(st.getPredicate())) {
			return false;
		}
		if (obj != null && !obj.equals(st.getObject())) {
			return false;
		}

		return matches(st.getContext(), contexts);
	}

	boolean matches(IReference[] stContext, IReference... contexts) {
		if (stContext != null && stContext.length > 0) {
			for (IReference c : stContext) {
				if (!matches(c, contexts)) {
					return false;
				}
			}
		}
		return true;
	}

	boolean matches(IReference stContext, IReference... contexts) {
		if (contexts != null && contexts.length == 0) {
			// Any context matches
			return true;
		} else {
			// Accept if one of the contexts from the pattern matches
			for (IReference context : notNull(contexts)) {
				if (context == null && stContext == null) {
					return true;
				}
				if (context != null && context.equals(stContext)) {
					return true;
				}
			}

			return false;
		}
	}

	IGraph emptyGraph = new EmptyGraph();

	protected IGraph emptyGraph() {
		return emptyGraph;
	}

	GraphIterator emptyGraphIterator() {
		Set<GraphStatement> set = Collections.emptySet();
		return new GraphIterator(set.iterator(), set);
	}

	protected class EmptyGraph extends AbstractSet<IStatement> implements
			IGraph {
		private static final long serialVersionUID = 3123007631452759092L;

		private Set<IStatement> emptySet = Collections.emptySet();

		@Override
		public Iterator<IStatement> iterator() {
			return emptySet.iterator();
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public boolean add(IStatement e) {
			throw new UnsupportedOperationException(
					"All statements are filtered out of view");
		}

		public boolean add(IReference subj, IReference pred, Object obj,
				IReference... contexts) {
			throw new UnsupportedOperationException(
					"All statements are filtered out of view");
		}

		public boolean clear(IReference... context) {
			return false;
		}

		public boolean contains(IReference subj, IReference pred, Object obj,
				IReference... contexts) {
			return false;
		}

		public Set<IReference> contexts() {
			return Collections.emptySet();
		}

		public IGraph filter(IReference subj, IReference pred, Object obj,
				IReference... contexts) {
			return emptyGraph();
		}

		public Set<Object> objects() {
			return Collections.emptySet();
		}

		public Set<IReference> predicates() {
			return Collections.emptySet();
		}

		public boolean remove(IReference subj, IReference pred, Object obj,
				IReference... contexts) {
			return false;
		}

		public Set<IReference> subjects() {
			return Collections.emptySet();
		}

		public ILiteral objectLiteral() {
			return null;
		}

		public Object objectValue() {
			return null;
		}

		public IReference objectReference() {
			return null;
		}

		public String objectString() {
			return null;
		}

		@Override
		public void rename(IReference source, IReference target) {
		}

		@Override
		public int hashCode() {
			return size();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof IGraph) {
				IGraph model = (IGraph) o;
				return model.isEmpty();
			}
			return false;
		}

	}

	protected class FilteredGraph extends AbstractSet<IStatement> implements
			IGraph {

		private static final long serialVersionUID = -2353344619836326934L;

		protected IReference subj;

		protected IReference pred;

		protected Object obj;

		protected IReference[] contexts;

		public FilteredGraph(IReference subj, IReference pred, Object obj,
				IReference... contexts) {
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.contexts = notNull(contexts);
		}

		@Override
		public Iterator<IStatement> iterator() {
			return match(subj, pred, obj, contexts);
		}

		@Override
		public int size() {
			int size = 0;
			Iterator<IStatement> iter = iterator();
			while (iter.hasNext()) {
				size++;
				iter.next();
			}
			return size;
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Statement) {
				Statement st = (Statement) o;
				if (accept(st)) {
					return LinkedHashGraph.this.contains(o);
				}
			}
			return false;
		}

		@Override
		public boolean add(IStatement st) {
			if (accept(st)) {
				return LinkedHashGraph.this.add(st);
			}
			throw new IllegalArgumentException(
					"Statement is filtered out of view: " + st);
		}

		public boolean add(IReference s, IReference p, Object o,
				IReference... c) {
			if (!accept(s, p, o, c)) {
				throw new IllegalArgumentException(
						"Statement is filtered out of view");
			}
			if (s == null) {
				s = subj;
			}
			if (p == null) {
				p = pred;
			}
			if (o == null) {
				o = obj;
			}
			if (c != null && c.length == 0) {
				c = contexts;
			}
			return LinkedHashGraph.this.add(s, p, o, c);
		}

		@Override
		public void clear() {
			LinkedHashGraph.this.remove(subj, pred, obj, contexts);
		}

		public boolean clear(IReference... c) {
			c = notNull(c);
			if (c.length == 0) {
				return remove(subj, pred, obj, contexts);
			} else if (matches(c, contexts)) {
				return LinkedHashGraph.this.remove(subj, pred, obj, c);
			} else {
				return false;
			}
		}

		public boolean remove(IReference s, IReference p, Object o,
				IReference... c) {
			if (!accept(s, p, o, c)) {
				return false;
			}
			if (s == null) {
				s = subj;
			}
			if (p == null) {
				p = pred;
			}
			if (o == null) {
				o = obj;
			}
			if (c != null && c.length == 0) {
				c = contexts;
			}
			return LinkedHashGraph.this.remove(s, p, o, c);
		}

		public boolean contains(IReference s, IReference p, Object o,
				IReference... c) {
			if (!accept(s, p, o, c)) {
				return false;
			}
			if (s == null) {
				s = subj;
			}
			if (p == null) {
				p = pred;
			}
			if (o == null) {
				o = obj;
			}
			if (c != null && c.length == 0) {
				c = contexts;
			}
			return LinkedHashGraph.this.contains(s, p, o, c);
		}

		public IGraph filter(IReference s, IReference p, Object o,
				IReference... c) {
			if (!accept(s, p, o, c)) {
				return emptyGraph();
			}
			if (s == null) {
				s = subj;
			}
			if (p == null) {
				p = pred;
			}
			if (o == null) {
				o = obj;
			}
			if (c != null && c.length == 0) {
				c = contexts;
			}
			return LinkedHashGraph.this.filter(s, p, o, c);
		}

		public Set<IReference> contexts() {
			if (contexts != null && contexts.length > 0) {
				return unmodifiableSet(new LinkedHashSet<IReference>(
						asList(contexts)));
			}
			return LinkedHashGraph.this.contexts(subj, pred, obj);
		}

		public Set<Object> objects() {
			if (obj != null) {
				return Collections.singleton(obj);
			}
			return LinkedHashGraph.this.objects(subj, pred, contexts);
		}

		public Set<IReference> predicates() {
			if (pred != null) {
				return Collections.singleton(pred);
			}
			return LinkedHashGraph.this.predicates(subj, obj, contexts);
		}

		public Set<IReference> subjects() {
			if (subj != null) {
				return Collections.singleton(subj);
			}
			return LinkedHashGraph.this.subjects(pred, obj, contexts);
		}

		public Object objectValue() throws KommaException {
			Iterator<Object> iter = objects().iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				if (iter.hasNext()) {
					throw new KommaException();
				}
				return obj;
			}
			return null;
		}

		public ILiteral objectLiteral() throws KommaException {
			Object obj = objectValue();
			if (obj == null) {
				return null;
			}
			if (obj instanceof ILiteral) {
				return (ILiteral) obj;
			}
			throw new KommaException();
		}

		public IReference objectReference() throws KommaException {
			Object obj = objectValue();
			if (obj == null) {
				return null;
			}
			if (obj instanceof IReference) {
				return (IReference) obj;
			}
			throw new KommaException();
		}

		public String objectString() throws KommaException {
			Object obj = objectValue();
			if (obj == null) {
				return null;
			}
			if (obj instanceof ILiteral) {
				return ((ILiteral) obj).getLabel();
			}
			return obj.toString();
		}

		@Override
		public void rename(IReference source, IReference target) {
			LinkedHashGraph.rename(this, source, target);
		}

		@Override
		public int hashCode() {
			return size();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof IGraph) {
				IGraph model = (IGraph) o;
				return GraphUtil.equals(this, model);
			}
			return false;
		}

		private boolean accept(IStatement st) {
			return matches(st, subj, pred, obj, contexts);
		}

		private boolean accept(IReference s, IReference p, Object o,
				IReference... c) {
			if (subj != null && !subj.equals(s)) {
				return false;
			}
			if (pred != null && !pred.equals(p)) {
				return false;
			}
			if (obj != null && !obj.equals(o)) {
				return false;
			}
			if (!matches(notNull(c), contexts)) {
				return false;
			}
			return true;
		}
	}

	abstract protected class ObjectSet<V> extends AbstractSet<V> {

		@Override
		public Iterator<V> iterator() {
			final Set<V> set = new LinkedHashSet<V>();
			final GraphIterator iter = statementIterator();
			return new Iterator<V>() {

				private GraphStatement current;

				private GraphStatement next;

				public boolean hasNext() {
					if (next == null) {
						next = findNext();
					}
					return next != null;
				}

				public V next() {
					if (next == null) {
						next = findNext();
						if (next == null) {
							throw new NoSuchElementException();
						}
					}
					current = next;
					next = null;
					V value = convert(current);
					set.add(value);
					return value;
				}

				public void remove() {
					if (current == null) {
						throw new IllegalStateException();
					}
					removeAll(set(node(current)), iter.getOwner());
					current = null;
				}

				private GraphStatement findNext() {
					while (iter.hasNext()) {
						GraphStatement st = iter.next();
						if (accept(st)) {
							return st;
						}
					}
					return null;
				}

				private boolean accept(GraphStatement st) {
					return !set.contains(convert(st));
				}

				private V convert(GraphStatement st) {
					return node(st).getObject();
				}
			};
		}

		@Override
		public int size() {
			Set<V> set = new LinkedHashSet<V>();
			Iterator<IStatement> iter = statementIterator();
			while (iter.hasNext()) {
				set.add(node((GraphStatement) iter.next()).getObject());
			}
			return set.size();
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			if (values.containsKey(o)) {
				return removeAll(set((GraphNode<V>) values.get(o)), null);
			}
			return false;
		}

		protected abstract GraphIterator statementIterator();

		protected abstract GraphNode<V> node(GraphStatement st);

		protected abstract Set<GraphStatement> set(GraphNode<V> node);

		boolean removeAll(Set<GraphStatement> remove, Set<GraphStatement> owner) {
			if (remove.isEmpty()) {
				return false;
			}
			for (GraphStatement st : remove) {
				GraphNode<IReference> subj = st.subj;
				Set<GraphStatement> subjects = subj.subjects;
				if (subjects == owner) {
					subj.subjects = new LinkedHashSet<GraphStatement>(owner);
					subj.subjects.removeAll(remove);
				} else if (subjects != remove) {
					subjects.remove(st);
				}
				GraphNode<IReference> pred = st.pred;
				Set<GraphStatement> predicates = pred.predicates;
				if (predicates == owner) {
					pred.predicates = new LinkedHashSet<GraphStatement>(owner);
					pred.predicates.removeAll(remove);
				} else if (predicates != remove) {
					predicates.remove(st);
				}
				GraphNode<Object> obj = st.obj;
				Set<GraphStatement> objects = obj.objects;
				if (objects == owner) {
					obj.objects = new LinkedHashSet<GraphStatement>(owner);
					obj.objects.removeAll(remove);
				} else if (objects != remove) {
					objects.remove(st);
				}
				GraphNode<IReference> ctx = st.ctx;
				Set<GraphStatement> contexts = ctx.contexts;
				if (contexts == owner) {
					ctx.contexts = new LinkedHashSet<GraphStatement>(owner);
					ctx.contexts.removeAll(remove);
				} else if (contexts != remove) {
					contexts.remove(st);
				}
				if (statements == owner) {
					statements = new LinkedHashSet<GraphStatement>(statements);
					statements.removeAll(remove);
				} else if (statements != remove && statements != owner) {
					statements.remove(st);
				}
			}
			remove.clear();
			return true;
		}
	}

	protected class GraphIterator implements Iterator<IStatement> {

		private Iterator<GraphStatement> iter;

		private Set<GraphStatement> owner;

		private GraphStatement last;

		public GraphIterator(Iterator<GraphStatement> iter,
				Set<GraphStatement> owner) {
			this.iter = iter;
			this.owner = owner;
		}

		public Set<GraphStatement> getOwner() {
			return owner;
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public GraphStatement next() {
			return last = iter.next();
		}

		public void remove() {
			if (last == null) {
				throw new IllegalStateException();
			}
			removeIfNotOwner(statements);
			removeIfNotOwner(last.subj.subjects);
			removeIfNotOwner(last.pred.predicates);
			removeIfNotOwner(last.obj.objects);
			removeIfNotOwner(last.ctx.contexts);
			iter.remove(); // remove from owner
		}

		private void removeIfNotOwner(Set<GraphStatement> subjects) {
			if (subjects != owner) {
				subjects.remove(last);
			}
		}
	}

	protected class GraphNode<V extends Object> implements Serializable {

		private static final long serialVersionUID = -1205676084606998540L;

		Set<GraphStatement> subjects = new LinkedHashSet<GraphStatement>();

		Set<GraphStatement> predicates = new LinkedHashSet<GraphStatement>();

		Set<GraphStatement> objects = new LinkedHashSet<GraphStatement>();

		Set<GraphStatement> contexts = new LinkedHashSet<GraphStatement>();

		private V value;

		public GraphNode(V value) {
			this.value = value;
		}

		public V getObject() {
			return value;
		}

		public boolean isNull() {
			return value == null;
		}
	}

	protected class GraphStatement implements IStatement {

		GraphNode<IReference> subj;

		GraphNode<IReference> pred;

		GraphNode<Object> obj;

		GraphNode<IReference> ctx;

		boolean isInferred;

		public GraphStatement(GraphNode<IReference> subj,
				GraphNode<IReference> pred, GraphNode<Object> obj,
				GraphNode<IReference> ctx, boolean isInferred) {
			assert subj != null;
			assert pred != null;
			assert obj != null;
			assert ctx != null;
			this.subj = subj;
			this.pred = pred;
			this.obj = obj;
			this.ctx = ctx;
			this.isInferred = isInferred;
		}

		public IReference getSubject() {
			return subj.getObject();
		}

		public IReference getPredicate() {
			return pred.getObject();
		}

		public Object getObject() {
			return obj.getObject();
		}

		public IReference getContext() {
			return ctx.getObject();
		}

		@Override
		public boolean isInferred() {
			return isInferred;
		}

		@Override
		public boolean equalsIgnoreContext(IStatementPattern other) {
			return Statements.equalsIgnoreContext(this, other);
		}

		@Override
		public boolean equals(Object obj) {
			return Statements.equals(this, obj);
		}

		@Override
		public int hashCode() {
			return Statements.hashCode(this);
		}

		@Override
		public String toString() {
			return Statements.toString(this);
		}

		@Override
		public Iterator<IStatement> iterator() {
			return Collections.<IStatement> singleton(this).iterator();
		}
	}

	protected class PatternIterator<S extends IStatement> extends
			FilterIterator<S> {
		public PatternIterator(Iterator<S> iter, final IReference subj,
				final IReference pred, final Object obj,
				final IReference... contexts) {
			super(new Filter<S>() {
				IReference[] ctxs = notNull(contexts);

				public boolean accept(S st) {
					return matches(st, subj, pred, obj, ctxs);
				}
			}, iter);
		}
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		// Write out any hidden serialization magic
		s.defaultWriteObject();
		// Write in size
		s.writeInt(statements.size());
		// Write in all elements
		for (GraphStatement st : statements) {
			IReference subj = st.getSubject();
			IReference pred = st.getPredicate();
			Object obj = st.getObject();
			IReference ctx = st.getContext();
			s.writeObject(new Statement(subj, pred, obj, ctx));
		}
	}

	private void readObject(ObjectInputStream s) throws IOException,
			ClassNotFoundException {
		// Read in any hidden serialization magic
		s.defaultReadObject();
		// Read in size
		int size = s.readInt();
		values = new HashMap<>(size * 2);
		statements = new LinkedHashSet<>(size);
		// Read in all elements
		for (int i = 0; i < size; i++) {
			Statement st = (Statement) s.readObject();
			add(st);
		}
	}

	private Iterator<?> find(Statement st) {
		IReference subj = st.getSubject();
		IReference pred = st.getPredicate();
		Object obj = st.getObject();
		IReference ctx = st.getContext();
		return match(subj, pred, obj, ctx);
	}

	private boolean addGraphStatement(GraphStatement st) {
		Set<GraphStatement> subj = st.subj.subjects;
		Set<GraphStatement> pred = st.pred.predicates;
		Set<GraphStatement> obj = st.obj.objects;
		Set<GraphStatement> ctx = st.ctx.contexts;
		if (smallest(subj, pred, obj, ctx).contains(st)) {
			return false;
		}
		statements.add(st);
		subj.add(st);
		pred.add(st);
		obj.add(st);
		ctx.add(st);
		return true;
	}

	@SafeVarargs
	private final <V> Set<V> smallest(Set<V>... sets) {
		int minSize = Integer.MAX_VALUE;
		Set<V> minSet = null;
		for (Set<V> set : sets) {
			if (set != null && set.size() < minSize) {
				minSet = set;
			}
		}
		return minSet;
	}

	@SuppressWarnings("unchecked")
	private <V> GraphNode<V> asNode(V value) {
		if (values.containsKey(value)) {
			return (GraphNode<V>) values.get(value);
		}
		GraphNode<V> node = new GraphNode<>(value);
		values.put(value, (GraphNode<Object>) node);
		return node;
	}

	private static final IReference[] DEFAULT_CONTEXTS = new IReference[] { null };

	/**
	 * Verifies that the supplied contexts parameter is not <tt>null</tt>,
	 * returning the default context if it is.
	 * 
	 * @param contexts
	 *            The parameter to check.
	 * @return a non-null array
	 */
	public static IReference[] notNull(IReference... contexts) {
		if (contexts == null) {
			return DEFAULT_CONTEXTS;
		}
		return contexts;
	}
}
