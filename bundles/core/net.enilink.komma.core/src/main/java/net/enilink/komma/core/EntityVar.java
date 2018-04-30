package net.enilink.komma.core;

/**
 * Represents an entity variable whose value is eagerly cached using an entity's
 * {@link IReference}. This allows values to be shared between different
 * instances of the same behaviour class.
 * 
 * @param <T>
 *            Value type represented by this variable.
 */
public interface EntityVar<T> {
	/**
	 * Sets the entity variable to the specified value.
	 * 
	 * @param value
	 *            the value to be stored in this entity variable.
	 */
	void set(T value);

	/**
	 * Returns the value of this entity variable.
	 * 
	 * @return the value of this entity variable
	 */
	T get();

	/**
	 * Removes the value of this entity variable. Invoking this method is
	 * equivalent to using <code>set(null)</code>.
	 */
	void remove();
}
