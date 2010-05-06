/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.util;

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
