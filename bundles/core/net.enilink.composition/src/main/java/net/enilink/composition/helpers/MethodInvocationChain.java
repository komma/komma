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
package net.enilink.composition.helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.aopalliance.intercept.MethodInvocation;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.exceptions.BehaviourException;

/**
 * Implements the {@link MethodInvocation} interface.
 */
public class MethodInvocationChain implements MethodInvocation {
	private final Object[] arguments;

	private int count;

	private final List<Method> invokeMethod = new ArrayList<Method>();

	private final List<Object> invokeTarget = new ArrayList<Object>();

	private final Method method;

	private final Object target;

	public MethodInvocationChain(Object target, Method method,
			Object[] arguments) {
		this.target = target;
		this.method = method;
		this.arguments = arguments;
	}

	public synchronized MethodInvocationChain appendInvocation(Object target,
			Method method) {
		invokeTarget.add(target);
		invokeMethod.add(method);
		return this;
	}

	public static Object cast(Object result, Class<?> resultType,
			Class<?> responseType) {
		if (isNil(result, resultType))
			return nil(responseType);
		if (resultType.equals(responseType) || Object.class.equals(resultType))
			return result;
		if (responseType.equals(Set.class))
			return Collections.singleton(result);
		if (resultType.equals(Set.class)) {
			Set<?> set = (Set<?>) result;
			if (set.isEmpty())
				return nil(responseType);
			return set.iterator().next();
		}
		return result;
	}

	@Override
	public Object[] getArguments() {
		return arguments;
	}

	private Object[] getArguments(Method method) {
		Object[] parameters = getArguments();
		Annotation[][] anns = method.getParameterAnnotations();
		Object[] result = new Object[anns.length];
		for (int i = 0; i < anns.length; i++) {
			if (i < parameters.length) {
				// if no @Iri copy over parameter by position
				result[i] = parameters[i];
			}
			for (int j = 0; j < anns[i].length; j++) {
				if (anns[i][j].annotationType().equals(Iri.class)) {
					String uri = ((Iri) anns[i][j]).value();
					result[i] = parameters[getParameterIndex(uri)];
				}
			}
		}
		return result;
	}

	public Method getMethod() {
		return method;
	}

	private int getParameterIndex(String uri) {
		Annotation[][] anns = method.getParameterAnnotations();
		for (int i = 0; i < anns.length; i++) {
			for (int j = 0; j < anns[i].length; j++) {
				if (anns[i][j].annotationType().equals(Iri.class)) {
					if (((Iri) anns[i][j]).value().equals(uri)) {
						return i;
					}
				}
			}
		}
		throw new UnsupportedOperationException("Parameter not found: " + uri);
	}

	@Override
	public AccessibleObject getStaticPart() {
		return method;
	}

	@Override
	public Object getThis() {
		return target;
	}

	public static boolean isNil(Object result, Class<?> type) {
		if (result == null)
			return true;
		if (!type.isPrimitive())
			return false;
		return result.equals(nil(type));
	}

	public static Object nil(Class<?> type) {
		if (Set.class.equals(type))
			return Collections.emptySet();
		if (!type.isPrimitive())
			return null;
		if (Void.TYPE.equals(type))
			return null;
		if (Boolean.TYPE.equals(type))
			return Boolean.FALSE;
		if (Character.TYPE.equals(type))
			return Character.valueOf((char) 0);
		if (Byte.TYPE.equals(type))
			return Byte.valueOf((byte) 0);
		if (Short.TYPE.equals(type))
			return Short.valueOf((short) 0);
		if (Integer.TYPE.equals(type))
			return Integer.valueOf((int) 0);
		if (Long.TYPE.equals(type))
			return Long.valueOf((long) 0);
		if (Float.TYPE.equals(type))
			return Float.valueOf((float) 0);
		if (Double.TYPE.equals(type))
			return Double.valueOf((double) 0);
		throw new AssertionError();
	}

	public synchronized Object proceed() throws Exception {
		try {
			Class<?> responseType = method.getReturnType();
			while (true) {
				if (count >= invokeTarget.size()) {
					return nil(responseType);
				}
				Method im = invokeMethod.get(count);
				Object it = invokeTarget.get(count);
				count++;
				Class<?>[] param = im.getParameterTypes();
				Class<?> resultType = im.getReturnType();
				if (param.length == 1
						&& MethodInvocation.class.isAssignableFrom(param[0])) {
					Object result = im.invoke(it, this);
					return cast(result, resultType, responseType);
				} else {
					Object result = im.invoke(it, getArguments(im));
					if (isNil(result, resultType))
						continue;
					return cast(result, resultType, responseType);
				}
			}
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception)
				throw (Exception) cause;
			if (cause instanceof Error)
				throw (Error) cause;
			throw new BehaviourException(cause);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			IllegalAccessError error = new IllegalAccessError(e.getMessage());
			error.initCause(e);
			throw error;
		}
	}

	@Override
	public synchronized String toString() {
		String params = Arrays.asList(arguments).toString();
		String values = params.substring(1, params.length() - 1);
		return method.getName() + "(" + values + ")";
	}
}