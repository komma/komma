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
package net.enilink.composition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.enilink.composition.exceptions.CompositionException;
import net.enilink.composition.mappers.RoleMapper;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Converts between resources and objects.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 */
public class DefaultObjectFactory<T> implements ObjectFactory<T> {
	private RoleMapper<T> mapper;

	private ClassResolver<T> resolver;

	@Inject
	private Injector injector;

	@Inject
	public DefaultObjectFactory(RoleMapper<T> mapper, ClassResolver<T> resolver) {
		this.mapper = mapper;
		this.resolver = resolver;
	}

	protected Object createInstance(Class<?> proxy) {
		try {
			return injector.getInstance(proxy);
		} catch (Exception e) {
			throw new CompositionException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.objects.ObjectFactory#createObject(R)
	 */
	public Object createObject() {
		return createInstance(resolver
				.resolveComposite(Collections.<T> emptyList()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.objects.ObjectFactory#createObject(R,
	 * java.lang.Class)
	 */
	public <C> C createObject(Class<C> type) {
		Set<T> types = Collections.singleton(getType(type));
		return type.cast(createObject(types));
	}

	@Override
	public <C> C createObject(Class<C> type, Class<?>... types) {
		Set<T> typeList = new HashSet<T>(1 + types.length);
		typeList.add(getType(type));
		for (Class<?> typeClass : types) {
			typeList.add(getType(typeClass));
		}
		return type.cast(createObject(typeList));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.objects.ObjectFactory#createObject(R,
	 * java.util.Collection)
	 */
	public Object createObject(Collection<T> types) {
		Class<?> proxy = resolver.resolveComposite(types);
		return createInstance(proxy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.enilink.composition.objects.ObjectFactory#createObject(R, T)
	 */
	public Object createObject(T... types) {
		assert types != null && types.length > 0;
		List<T> list = Arrays.asList(types);
		return createObject(list);
	}

	protected T getType(Class<?> concept) {
		return mapper.findType(concept);
	}

	/**
	 * @return <code>true</code> If the given type can be used as a concept
	 *         parameter.
	 */
	public boolean isNamedConcept(Class<?> type) {
		return mapper.findType(type) != null;
	}
}
