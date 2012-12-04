package net.enilink.composition.properties.util;

import net.enilink.composition.properties.PropertySet;

/**
 * Utility methods for property sets.
 */
public class PropertySets {
	/**
	 * Returns an unmodifiable view of the specified property set.
	 * 
	 * @param propertySet
	 *            The property set for which a unmodifiable view is to be
	 *            returned.
	 * @return The unmodifiable property set.
	 */
	public static <E> PropertySet<E> unmodifiable(PropertySet<E> propertySet) {
		return new UnmodifiablePropertySet<E>(propertySet);
	}
}
