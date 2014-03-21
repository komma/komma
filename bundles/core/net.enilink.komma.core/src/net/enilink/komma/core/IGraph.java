/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2007-2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.core;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * An RDF graph, represented as a set of {@link IStatement}s with predictable
 * iteration order.
 * 
 */
public interface IGraph extends Set<IStatement>, Serializable {

	/**
	 * Gets the map that contains the assigned namespaces.
	 * 
	 * @return Map of prefix to namespace
	 */
	public Map<String, String> getNamespaces();

	/**
	 * Gets the namespace that is associated with the specified prefix, if any.
	 * 
	 * @param prefix
	 *            A namespace prefix.
	 * @return The namespace name that is associated with the specified prefix,
	 *         or <tt>null</tt> if there is no such namespace.
	 */
	public String getNamespace(String prefix);

	/**
	 * Sets the prefix for a namespace.
	 * 
	 * @param prefix
	 *            The new prefix.
	 * @param name
	 *            The namespace name that the prefix maps to.
	 */
	public String setNamespace(String prefix, String name);

	/**
	 * Removes a namespace declaration by removing the association between a
	 * prefix and a namespace name.
	 * 
	 * @param prefix
	 *            The namespace prefix of which the assocation with a namespace
	 *            name is to be removed.
	 */
	public void removeNamespace(String prefix);

	/**
	 * Determines if statements with the specified subject, predicate, object
	 * and (optionally) context exist in this model. The <tt>subject</tt>,
	 * <tt>predicate</tt> and <tt>object</tt> parameters can be <tt>null</tt> to
	 * indicate wildcards. The <tt>contexts</tt> parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements
	 * will match disregarding their context. If one or more contexts are
	 * specified, statements with a context matching one of these will match.
	 * Note: to match statements without an associated context, specify the
	 * value <tt>null</tt> and explicitly cast it to type <tt>Resource</tt>.
	 * <p>
	 * Examples: <tt>model.contains(s1, null, null)</tt> is true if any
	 * statements in this model have subject <tt>s1</tt>,<br>
	 * <tt>model.contains(null, null, null, c1)</tt> is true if any statements
	 * in this model have context <tt>c1</tt>,<br>
	 * <tt>model.contains(null, null, null, (Resource)null)</tt> is true if any
	 * statements in this model have no associated context,<br>
	 * <tt>model.contains(null, null, null, c1, c2, c3)</tt> is true if any
	 * statements in this model have context <tt>c1</tt>, <tt>c2</tt> or
	 * <tt>c3</tt>.
	 * 
	 * @param subj
	 *            The subject of the statements to match, <tt>null</tt> to match
	 *            statements with any subject.
	 * @param pred
	 *            The predicate of the statements to match, <tt>null</tt> to
	 *            match statements with any predicate.
	 * @param obj
	 *            The object of the statements to match, <tt>null</tt> to match
	 *            statements with any object.
	 * @param contexts
	 *            The contexts of the statements to match. If no contexts are
	 *            specified, statements will match disregarding their context.
	 *            If one or more contexts are specified, statements with a
	 *            context matching one of these will match.
	 * @return <code>true</code> if statements match the specified pattern.
	 */
	public boolean contains(IReference subj, IReference pred, Object obj,
			IReference... contexts);

	/**
	 * Adds one or more statements to the model. This method creates a statement
	 * for each specified context and adds those to the model. If no contexts
	 * are specified, a single statement with no associated context is added. If
	 * this Model is a filtered Model then null (if context empty) values are
	 * permitted and will used the corresponding filtered values.
	 * 
	 * @param subj
	 *            The statement's subject.
	 * @param pred
	 *            The statement's predicate.
	 * @param obj
	 *            The statement's object.
	 * @param contexts
	 *            The contexts to add statements to.
	 * @throws IllegalArgumentException
	 *             If This Model cannot store the given statement, because it is
	 *             filtered out of this view.
	 * @throws UnsupportedOperationException
	 *             If this Model cannot accept any statements, because it is
	 *             filter to the empty set.
	 */
	public boolean add(IReference subj, IReference pred, Object obj,
			IReference... contexts);

	/**
	 * Removes statements with the specified context exist in this model.
	 * 
	 * @param context
	 *            The context of the statements to remove.
	 * @return <code>true</code> if one or more statements have been removed.
	 */
	public boolean clear(IReference... context);

	/**
	 * Removes statements with the specified subject, predicate, object and
	 * (optionally) context exist in this model. The <tt>subject</tt>,
	 * <tt>predicate</tt> and <tt>object</tt> parameters can be <tt>null</tt> to
	 * indicate wildcards. The <tt>contexts</tt> parameter is a wildcard and
	 * accepts zero or more values. If no contexts are specified, statements
	 * will be removed disregarding their context. If one or more contexts are
	 * specified, statements with a context matching one of these will be
	 * removed. Note: to remove statements without an associated context,
	 * specify the value <tt>null</tt> and explicitly cast it to type
	 * <tt>Resource</tt>.
	 * <p>
	 * Examples: <tt>model.remove(s1, null, null)</tt> removes any statements in
	 * this model have subject <tt>s1</tt>,<br>
	 * <tt>model.remove(null, null, null, c1)</tt> removes any statements in
	 * this model have context <tt>c1</tt>,<br>
	 * <tt>model.remove(null, null, null, (Resource)null)</tt> removes any
	 * statements in this model have no associated context,<br>
	 * <tt>model.remove(null, null, null, c1, c2, c3)</tt> removes any
	 * statements in this model have context <tt>c1</tt>, <tt>c2</tt> or
	 * <tt>c3</tt>.
	 * 
	 * @param subj
	 *            The subject of the statements to remove, <tt>null</tt> to
	 *            remove statements with any subject.
	 * @param pred
	 *            The predicate of the statements to remove, <tt>null</tt> to
	 *            remove statements with any predicate.
	 * @param obj
	 *            The object of the statements to remove, <tt>null</tt> to
	 *            remove statements with any object.
	 * @param contexts
	 *            The contexts of the statements to remove. If no contexts are
	 *            specified, statements will be removed disregarding their
	 *            context. If one or more contexts are specified, statements
	 *            with a context matching one of these will be removed.
	 * @return <code>true</code> if one or more statements have been removed.
	 */
	public boolean remove(IReference subj, IReference pred, Object obj,
			IReference... contexts);

	// Views

	/**
	 * Returns a view of the statements with the specified subject, predicate,
	 * object and (optionally) context. The <tt>subject</tt>, <tt>predicate</tt>
	 * and <tt>object</tt> parameters can be <tt>null</tt> to indicate
	 * wildcards. The <tt>contexts</tt> parameter is a wildcard and accepts zero
	 * or more values. If no contexts are specified, statements will match
	 * disregarding their context. If one or more contexts are specified,
	 * statements with a context matching one of these will match. Note: to
	 * match statements without an associated context, specify the value
	 * <tt>null</tt> and explicitly cast it to type <tt>Resource</tt>.
	 * <p>
	 * The returned model is backed by this Model, so changes to this Model are
	 * reflected in the returned model, and vice-versa. If this Model is
	 * modified while an iteration over the returned model is in progress
	 * (except through the iterator's own <tt>remove</tt> operation), the
	 * results of the iteration are undefined. The model supports element
	 * removal, which removes the corresponding statement from this Model, via
	 * the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
	 * <tt>retainAll</tt>, and <tt>clear</tt> operations. The statements passed
	 * to the <tt>add</tt> and <tt>addAll</tt> operations must match the
	 * parameter pattern.
	 * <p>
	 * Examples: <tt>model.filter(s1, null, null)</tt> matches all statements
	 * that have subject <tt>s1</tt>,<br>
	 * <tt>model.filter(null, null, null, c1)</tt> matches all statements that
	 * have context <tt>c1</tt>,<br>
	 * <tt>model.filter(null, null, null, (Resource)null)</tt> matches all
	 * statements that have no associated context,<br>
	 * <tt>model.filter(null, null, null, c1, c2, c3)</tt> matches all
	 * statements that have context <tt>c1</tt>, <tt>c2</tt> or <tt>c3</tt>.
	 * 
	 * @param subj
	 *            The subject of the statements to match, <tt>null</tt> to match
	 *            statements with any subject.
	 * @param pred
	 *            The predicate of the statements to match, <tt>null</tt> to
	 *            match statements with any predicate.
	 * @param obj
	 *            The object of the statements to match, <tt>null</tt> to match
	 *            statements with any object.
	 * @param contexts
	 *            The contexts of the statements to match. If no contexts are
	 *            specified, statements will match disregarding their context.
	 *            If one or more contexts are specified, statements with a
	 *            context matching one of these will match.
	 * @return The statements that match the specified pattern.
	 */
	public IGraph filter(IReference subj, IReference pred, Object obj,
			IReference... contexts);

	/**
	 * Returns a {@link Set} view of the subjects contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the
	 * model, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations. It
	 * does not support the <tt>add</tt> or <tt>addAll</tt> operations if the
	 * parameters <tt>pred</tt> or <tt>obj</tt> are null.
	 * 
	 * @return a set view of the subjects contained in this model
	 */
	public Set<IReference> subjects();

	/**
	 * Returns a {@link Set} view of the predicates contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the
	 * model, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations. It
	 * does not support the <tt>add</tt> or <tt>addAll</tt> operations if the
	 * parameters <tt>subj</tt> or <tt>obj</tt> are null.
	 * 
	 * @return a set view of the predicates contained in this model
	 */
	public Set<IReference> predicates();

	/**
	 * Returns a {@link Set} view of the objects contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the
	 * model, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations. It
	 * does not support the <tt>add</tt> or <tt>addAll</tt> operations if the
	 * parameters <tt>subj</tt> or <tt>pred</tt> are null.
	 * 
	 * @return a set view of the objects contained in this model
	 */
	public Set<Object> objects();

	/**
	 * Returns a {@link Set} view of the contexts contained in this model. The
	 * set is backed by the model, so changes to the model are reflected in the
	 * set, and vice-versa. If the model is modified while an iteration over the
	 * set is in progress (except through the iterator's own <tt>remove</tt>
	 * operation), the results of the iteration are undefined. The set supports
	 * element removal, which removes the corresponding statement from the
	 * model, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations. It
	 * does not support the <tt>add</tt> or <tt>addAll</tt> operations if the
	 * parameters <tt>subj</tt> , <tt>pred</tt> or <tt>obj</tt> are null.
	 * 
	 * @return a set view of the contexts contained in this model
	 */
	public Set<IReference> contexts();

	/**
	 * Gets the object of the statement(s). If contains one or more statements,
	 * all these statements should have the same object. A
	 * {@link ModelException} is thrown if this is not the case.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no
	 *         matching statements were found.
	 * @throws KommaException
	 *             If the statements matched by the specified parameters have
	 *             more than one unique object.
	 */
	public Object objectValue();

	/**
	 * Utility method that casts the return value of {@link #objectValue()} to a
	 * Literal, or throws a ModelUtilException if that value is not a Literal.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no
	 *         matching statements were found.
	 * @throws IReference
	 *             If such an exception is thrown by {@link #objectValue()} or
	 *             if its return value is not a Literal.
	 */
	public ILiteral objectLiteral();

	/**
	 * Utility method that casts the return value of {@link #objectValue()} to a
	 * Resource, or throws a ModelUtilException if that value is not a Resource.
	 * 
	 * @return The object of the matched statement(s), or <tt>null</tt> if no
	 *         matching statements were found.
	 * @throws KommaException
	 *             If such an exception is thrown by {@link #objectValue()} or
	 *             if its return value is not a Resource.
	 */
	public IReference objectReference();

	/**
	 * Utility method that returns the string value of {@link #objectValue()}.
	 * 
	 * @return The object string value of the matched statement(s), or
	 *         <tt>null</tt> if no matching statements were found.
	 * @throws KommaException
	 *             If the statements matched by the specified parameters have
	 *             more than one unique object.
	 */
	public String objectString();
}
