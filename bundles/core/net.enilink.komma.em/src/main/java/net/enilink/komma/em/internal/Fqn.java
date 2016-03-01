package net.enilink.komma.em.internal;

import java.util.Arrays;

/**
 * A composite key for caching. 
 */
public class Fqn {
	private final Object[] elements;
	private transient int hashCode = 0;

	public Fqn(Object... elements) {
		this.elements = elements;
	}

	/**
	 * Computes hash code and caches it.
	 */
	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = Arrays.hashCode(elements);
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Fqn)) {
			return false;
		}
		Fqn other = (Fqn) obj;
		if (elements.length != other.elements.length)
			return false;
		return Arrays.equals(elements, other.elements);
	}
}
