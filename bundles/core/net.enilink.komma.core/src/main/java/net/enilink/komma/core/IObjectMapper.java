package net.enilink.komma.core;

import java.util.function.Consumer;

/**
 * Interface for custom mappers between Java objects and RDF representations.
 */
public interface IObjectMapper {
	/**
	 * Returns a blank node or URI for the given object.
	 *
	 * @param object a Java object
	 * @return an RDF reference
	 */
	IReference getReference(Object object);

	/**
	 * Converts an RDF resource into a Java object.
	 *
	 * @param reference URI or blank node of the resource
	 * @param initialData initial data that can be used for initialization
	 * @param manager an associated entity manager or <code>null</code>
	 * @return a Java object for the given RDF resource
	 */
	Object readObject(IReference reference, IStatementSource initialData, IEntityManager manager);

	/**
	 * Converts a Java object to RDF triples.
	 *
	 * @param object the Java object to convert
	 * @param sink sink for the generated triples
	 */
	void writeObject(Object object, Consumer<IStatement> sink);
}
