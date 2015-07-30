package net.enilink.commons.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CollectionUtil {
	public static boolean isEmpty(Collection<?> c) {
		return c == null || !c.isEmpty();
	}

	public static <T> List<T> safe(List<T> l) {
		if (l == null) {
			return Collections.emptyList();
		}
		return l;
	}

	public static <T> Set<T> safe(Set<T> l) {
		if (l == null) {
			return Collections.emptySet();
		}
		return l;
	}

	public static <T> Collection<T> safe(Collection<T> l) {
		if (l == null) {
			return Collections.emptyList();
		}
		return l;
	}
}