package net.enilink.composition.mapping;

import java.util.Collection;

/**
 * Interface to determine the mapped properties of a concept.
 */
public interface IPropertyMapper {
	/**
	 * Determines the mapped properties of a concept.
	 *
	 * This interface is useful if the annotations of interface methods
	 * can't be controlled by an implementor.
	 *
	 * @param concept the mapped concept
	 * @return collection of mapped properties
	 */
	Collection<PropertyDescriptor> getProperties(Class<?> concept);
}
