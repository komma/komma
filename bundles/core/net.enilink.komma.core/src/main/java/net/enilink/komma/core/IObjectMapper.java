package net.enilink.komma.core;

/**
 * Interface for custom mappers between Java objects and RDF representations.
 */
public interface IObjectMapper {
	/**
	 * Returns a blank node or URI for the given object.
	 *
	 * @param object a Java object
	 * @param em the associated entity manager
	 * @return an RDF reference
	 */
	IReference getReference(Object object, IEntityManager em);

	/**
	 * Converts an RDF resource into a Java object.
	 *
	 * @param reference URI or blank node of the resource
	 * @param em the associated entity manager
	 * @param initialData initial data that can be used for initialization
	 * @return a Java object for the given RDF resource
	 */
	Object toObject(IReference reference, IEntityManager em, IGraph initialData);

	/**
	 * Converts a Java object to RDF triples.
	 *
	 * @param object the Java object to convert
	 * @param em the associated entity manager
	 */
	void merge(Object object, IEntityManager em);
}
