package net.enilink.komma.core;

/**
 * Allows to dynamically provide an object of type <code>T</code>, for example,
 * depending on the current HTTP request.
 */
public interface IProvider<T> {
	/**
	 * Provides an instance of <code>T</code>. Must never return
	 * <code>null</code>.
	 * 
	 * @return an object of type <code>T</code>
	 */
	T get();
}
