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
package net.enilink.composition.properties;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.annotations.Localized;
import net.enilink.composition.properties.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static java.util.Locale.ENGLISH;

/**
 * Determines mapped properties of a given class.
 */
public class PropertyMapper {
	private static final String GET_PREFIX = "get";
	private static final String SET_PREFIX = "set";
	private static final String IS_PREFIX = "is";

	private static Logger logger = LoggerFactory.getLogger(PropertyMapper.class);
	private Properties properties = new Properties();

	public PropertyMapper() {
	}

	public Collection<PropertyDescriptor> getProperties(Class<?> concept) {
		List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
		while (concept != null) {
			for (Method method : concept.getDeclaredMethods()) {
				if (isMappedGetter(method)) {
					properties.add(createPropertyDescriptor(method));
				}
			}
			concept = concept.getSuperclass();
		}
		return properties;
	}

	protected String getPredicate(Method readMethod) {
		Iri iri = readMethod.getAnnotation(Iri.class);
		return iri == null ? null : iri.value();
	}

	private boolean isMappedGetter(Method method) {
		if (method.getParameterTypes().length != 0)
			return false;
		if (method.isAnnotationPresent(Iri.class))
			return true;
		return false;
	}

	private PropertyDescriptor createPropertyDescriptor(Method readMethod) {
		List<PropertyAttribute> attributes = new ArrayList<>();
		for (Annotation annotation : readMethod.getAnnotations()) {
			if (annotation instanceof Localized) {
				attributes.add(new PropertyAttribute(PropertyAttribute.LOCALIZED, null));
			} else if (annotation instanceof Type) {
				attributes.add(new PropertyAttribute(PropertyAttribute.TYPE, ((Type) annotation).value()));
			}
		}
		String name = getPropertyName(readMethod);
		Method setter = getSetterMethod(name, readMethod);
		return new PropertyDescriptor(name, readMethod, setter, getPredicate(readMethod),
				attributes.toArray(new PropertyAttribute[attributes.size()]));
	}

	protected Method getSetterMethod(String property, Method getter) {
		try {
			Class<?> dc = getter.getDeclaringClass();
			Class<?> rt = getter.getReturnType();
			Method setter;
			if (isBeanGet(getter) || isBeanIs(getter)) {
				String setterName = SET_PREFIX + capitalize(property);
				setter = dc.getDeclaredMethod(setterName, rt);
			} else {
				setter = dc.getDeclaredMethod(getter.getName(), rt);
			}
			if (setter != null) {
				Class<?> returnType = setter.getReturnType();
				if (!Void.TYPE.equals(returnType)
						&& !returnType.isAssignableFrom(dc)) {
					throw new AssertionError(
							String.format(
									"Setter signature for property %s should either "
											+ "return void or %s or a superclass of %s",
									property, dc.getCanonicalName(),
									dc.getCanonicalName()));
				}
			}
			return setter;
		} catch (NoSuchMethodException exc) {
			return null;
		}
	}

	protected String getPropertyName(Method method) {
		String name = method.getName();
		if (isBeanGet(method)) {
			// Simple getter
			return decapitalize(name.substring(3));
		} else if (isBeanIs(method)) {
			// Boolean getter
			return decapitalize(name.substring(2));
		}
		return name;
	}

	private static String capitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
	}

	private static String decapitalize(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
				&& Character.isUpperCase(name.charAt(0))) {
			return name;
		}
		char chars[] = name.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		return new String(chars);
	}

	private static boolean isBeanGet(Method method) {
		String name = method.getName();
		return name.startsWith(GET_PREFIX) && name.length() > 3
				&& Character.isUpperCase(name.charAt(3));
	}

	private static boolean isBeanIs(Method method) {
		String name = method.getName();
		boolean bool = method.getReturnType() == boolean.class;
		return bool && name.startsWith(IS_PREFIX) && name.length() > 2
				&& Character.isUpperCase(name.charAt(2));
	}
}
