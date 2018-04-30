package net.enilink.komma.core;

/**
 * Allows to access typed generic classes for the {@link IBindings} interface.
 */
public class Bindings {
	/**
	 * Return a parameterized class instance for {@link IBindings}.
	 * 
	 * @return Parameterized {@link IBindings} class
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> Class<IBindings<T>> typed() {
		return (Class) IBindings.class;
	}

	/**
	 * Return a parameterized class instance for {@link IBindings}.
	 * 
	 * @param c
	 *            The element type
	 * @return Parameterized {@link IBindings} class
	 */
	public static <T> Class<IBindings<T>> typed(Class<T> c) {
		return Bindings.<T> typed();
	}
}