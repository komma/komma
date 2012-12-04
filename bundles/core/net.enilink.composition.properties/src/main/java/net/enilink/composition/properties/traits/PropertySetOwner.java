package net.enilink.composition.properties.traits;

import net.enilink.composition.properties.PropertySet;

/**
 * Implemented by all behaviours with mapped properties.
 */
public interface PropertySetOwner {
	/**
	 * Returns the property set for the given <code>uri</code>.
	 * 
	 * @param uri
	 *            The property name.
	 * @return A property set for the <code>uri</code>.
	 */
	<E> PropertySet<E> getPropertySet(String uri);
}
