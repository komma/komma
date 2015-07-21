/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.composition.mappers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks recorded roles and maps them to their subject type.
 * 
 */
public class DirectMapper<T> implements Cloneable {
	private Map<Class<?>, Set<T>> directTypes = new HashMap<Class<?>, Set<T>>(
			256);

	private Map<T, Set<Class<?>>> directRoles = new HashMap<T, Set<Class<?>>>(
			256);

	public DirectMapper<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			DirectMapper<T> cloned = (DirectMapper<T>) super.clone();
			cloned.directTypes = clone(directTypes);
			cloned.directRoles = clone(directRoles);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	private <K, V> Map<K, Set<V>> clone(Map<K, Set<V>> map) {
		Map<K, Set<V>> cloned = new HashMap<K, Set<V>>(map);
		for (Map.Entry<K, Set<V>> e : cloned.entrySet()) {
			e.setValue(new HashSet<V>(e.getValue()));
		}
		return cloned;
	}

	public Set<Class<?>> getDirectRoles(T type) {
		return directRoles.get(type);
	}

	public Set<T> getDirectTypes(Class<?> role) {
		return directTypes.get(role);
	}

	public void recordRole(Class<?> role, T type) {
		Set<T> roleTypes = directTypes.get(role);
		if (roleTypes == null) {
			directTypes.put(role, roleTypes = new HashSet<T>());
		}
		if (type != null) {
			roleTypes.add(type);
		}
		if (type != null) {
			Set<Class<?>> typeRoles = directRoles.get(type);
			if (typeRoles == null) {
				directRoles.put(type, typeRoles = new HashSet<Class<?>>());
			}
			if (role != null) {
				typeRoles.add(role);
			}
		}
	}
}