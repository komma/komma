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

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks recorded roles and maps them to their subject type.
 * 
 */
public class TypeMapper<T> implements Cloneable {
	private final Logger logger = LoggerFactory.getLogger(TypeMapper.class);

	private Map<Class<?>, T> types = new ConcurrentHashMap<Class<?>, T>(256);

	public TypeMapper<T> clone() {
		try {
			@SuppressWarnings("unchecked")
			TypeMapper<T> cloned = (TypeMapper<T>) super.clone();
			cloned.types = new ConcurrentHashMap<Class<?>, T>(types);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Finds the rdf:Class<?> for this Java Class<?>.
	 * 
	 * @param javaClass
	 * @return URI of the rdf:Class<?> for this Java Class<?> or null.
	 */
	public T findType(Class<?> role) {
		return types.get(role);
	}

	public synchronized void recordRole(Class<?> role, T type) {
		assert type != null;
		if (!types.containsKey(role)) {
			types.put(role, type);
			if (logger.isDebugEnabled()) {
				String sn = role.getSimpleName();
				String cn = role.getName().replace('.', '/') + ".class";
				ClassLoader cl = role.getClassLoader();
				if (cl == null)
					cl = Thread.currentThread().getContextClassLoader();
				URL location = cl.getResource(cn);
				logger.debug("Role {} loaded from {}", sn, location);
			}
		}
	}
}