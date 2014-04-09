package net.enilink.komma.core;

/**
 * Constants for properties that may be used to configure instances of
 * {@link IEntityManagerFactory}, {@link IEntityManager} or {@link IQuery}.
 */
public class Properties {
	private Properties() {
	}

	/**
	 * Property for setting the maximum query execution time in milliseconds.
	 * 
	 * The corresponding value should be an integer.
	 */
	public static final String TIMEOUT = "net.enilink.komma.query.timeout";
}
