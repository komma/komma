/*
 * Copyright (c) 2009, 2010, James Leigh All rights reserved.
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

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoleMatcher implements Cloneable {
	private ConcurrentNavigableMap<String, List<Class<?>>> pathprefix = new ConcurrentSkipListMap<String, List<Class<?>>>();
	private ConcurrentNavigableMap<String, List<Class<?>>> uriprefix = new ConcurrentSkipListMap<String, List<Class<?>>>();
	private ConcurrentMap<String, List<Class<?>>> paths = new ConcurrentHashMap<String, List<Class<?>>>();
	private ConcurrentMap<String, List<Class<?>>> uris = new ConcurrentHashMap<String, List<Class<?>>>();
	private boolean empty = true;

	public RoleMatcher clone() {
		RoleMatcher cloned = new RoleMatcher();
		for (String key : pathprefix.keySet()) {
			for (Class<?> role : pathprefix.get(key)) {
				cloned.addRoles(key + '*', role);
			}
		}
		for (String key : uriprefix.keySet()) {
			for (Class<?> role : uriprefix.get(key)) {
				cloned.addRoles(key + '*', role);
			}
		}
		for (String key : paths.keySet()) {
			for (Class<?> role : paths.get(key)) {
				cloned.addRoles(key, role);
			}
		}
		for (String key : uris.keySet()) {
			for (Class<?> role : uris.get(key)) {
				cloned.addRoles(key, role);
			}
		}
		return cloned;
	}

	public boolean isEmpty() {
		return empty;
	}

	public void addRoles(String pattern, Class<?> role) {
		if (pattern.endsWith("*")) {
			String prefix = pattern.substring(0, pattern.length() - 1);
			if (prefix.startsWith("/")) {
				add(paths, prefix, role);
				add(pathprefix, prefix, role);
			} else {
				add(uris, prefix, role);
				add(uriprefix, prefix, role);
			}
		} else {
			if (pattern.startsWith("/")) {
				add(paths, pattern, role);
			} else {
				add(uris, pattern, role);
			}
		}
		empty = false;
	}

	public void findRoles(String uri, Collection<Class<?>> roles) {
		List<Class<?>> list = uris.get(uri);
		if (list != null) {
			roles.addAll(list);
		}
		findRoles(uriprefix, uri, roles);
		int idx = uri.indexOf("://");
		if (idx > 0) {
			String path = uri.substring(uri.indexOf('/', idx + 3));
			list = paths.get(path);
			if (list != null) {
				roles.addAll(list);
			}
			findRoles(pathprefix, path, roles);
		}
	}

	private void add(ConcurrentMap<String, List<Class<?>>> map, String pattern,
			Class<?> role) {
		List<Class<?>> list = map.get(pattern);
		if (list == null) {
			list = new CopyOnWriteArrayList<Class<?>>();
			List<Class<?>> o = map.putIfAbsent(pattern, list);
			if (o != null) {
				list = o;
			}
		}
		list.add(role);
	}

	private boolean findRoles(NavigableMap<String, List<Class<?>>> map,
			String full, Collection<Class<?>> roles) {
		String key = map.lowerKey(full);
		if (key == null) {
			return false;
		} else if (full.startsWith(key)) {
			roles.addAll(map.get(key));
			findRoles(map, key, roles);
			return true;
		} else {
			int idx = 0;
			while (idx < full.length() && idx < key.length()
					&& full.charAt(idx) == key.charAt(idx)) {
				idx++;
			}
			String prefix = full.substring(0, idx);
			if (map.containsKey(prefix)) {
				roles.addAll(map.get(prefix));
				if (idx > 1) {
					findRoles(map, prefix, roles);
				}
				return true;
			} else if (idx > 1) {
				return findRoles(map, prefix, roles);
			} else {
				return false;
			}
		}
	}
}
