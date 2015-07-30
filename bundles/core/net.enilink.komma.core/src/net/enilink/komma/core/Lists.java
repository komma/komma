package net.enilink.komma.core;

import java.util.List;

/**
 * Helper methods to work with {@link List} instances.
 */
public class Lists {
	private static final URI TYPE_LIST = URIs
			.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#List");

	/**
	 * Creates a new anonymous <code>rdf:List</code>.
	 * 
	 * @param em
	 *            The entity manager
	 * @return A {@link List} instance
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> create(IEntityManager em) {
		return (List<E>) em.create(TYPE_LIST);
	}

	/**
	 * Creates a new anonymous list of the given <code>type</code>.
	 * 
	 * @param em
	 *            The entity manager
	 * @param type
	 *            The type of the new list
	 * @return A {@link List} instance
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> create(IEntityManager em, URI type) {
		return (List<E>) em.create(type);
	}

	/**
	 * Creates a new named <code>rdf:List</code>.
	 * 
	 * @param em
	 *            The entity manager
	 * @param name
	 *            The name of the new list
	 * @return A {@link List} instance
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> createNamed(IEntityManager em, URI name) {
		return (List<E>) em.createNamed(name, TYPE_LIST);
	}

	/**
	 * Creates a new named list of the given <code>type</code>.
	 * 
	 * @param em
	 *            The entity manager
	 * @param name
	 *            The name of the new list
	 * @param type
	 *            The type of the new list
	 * @return A {@link List} instance
	 */
	@SuppressWarnings("unchecked")
	public static <E> List<E> create(IEntityManager em, URI name, URI type) {
		return (List<E>) em.create(type);
	}
}
