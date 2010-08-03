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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.concepts.Message;
import net.enilink.composition.exceptions.BehaviourException;
import net.enilink.composition.vocabulary.OBJ;

/**
 * Implements the Message interface(s) through an InvocationHandler.
 * 
 */
public class InvocationMessageContext implements InvocationHandler, Message {
	private Object target;

	private Class<?> type;

	private Method method;

	private Object[] parameters;

	private Set<?> response;

	private List<Object> invokeTarget = new ArrayList<Object>();

	private List<Method> invokeMethod = new ArrayList<Method>();

	private int count;

	public InvocationMessageContext(Object target, Class<?> messageType,
			Method method, Object[] parameters) {
		this.target = target;
		if (messageType != null && isMessageType(messageType)) {
			this.type = messageType;
		}
		this.method = method;
		this.parameters = parameters;
	}

	public InvocationMessageContext(Object target, Method method,
			Object[] parameters) {
		this(target, null, method, parameters);
	}

	public InvocationMessageContext appendInvocation(Object target,
			Method method) {
		invokeTarget.add(target);
		invokeMethod.add(method);
		return this;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		if (method.getDeclaringClass().equals(Message.class)) {
			return method.invoke(this, args);
		}
		String uri = method.getAnnotation(Iri.class).value();
		if (uri.equals(OBJ.PROCEED)) {
			proceed();
			return null;
		} else if (uri.equals(OBJ.TARGET)) {
			if (args == null || args.length == 0)
				return getTarget();
			setTarget(args[0]);
			return null;
		} else if (uri.equals(OBJ.OBJECT_RESPONSE)) {
			if (args == null || args.length == 0)
				return getObjectResponse();
			setObjectResponse((Set<?>) args[0]);
			return null;
		} else if (uri.equals(OBJ.LITERAL_RESPONSE)) {
			if (args == null || args.length == 0)
				return getLiteralResponse();
			setLiteralResponse((Set<?>) args[0]);
			return null;
		} else if (uri.equals(OBJ.FUNCTIONAL_OBJECT_RESPONSE)) {
			if (args == null || args.length == 0)
				return getFunctionalObjectResponse();
			setFunctionalObjectResponse(args[0]);
			return null;
		} else if (uri.equals(OBJ.FUNCITONAL_LITERAL_RESPONSE)) {
			if (args == null || args.length == 0)
				return getFunctionalLiteralResponse();
			setFunctionalLiteralResponse(args[0]);
			return null;
		}
		int idx = getParameterIndex(uri);
		if (args == null || args.length == 0) {
			return getParameters()[idx];
		} else {
			Object[] params = getParameters();
			params[idx] = args[0];
			setParameters(params);
			return null;
		}
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}

	public void proceed() {
		response = nextResponse();
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Object getFunctionalLiteralResponse() {
		if (response == null) {
			response = nextResponse();
		}
		if (response.size() == 1) {
			return response.iterator().next();
		}
		return null;
	}

	public void setFunctionalLiteralResponse(Object functionalLiteralResponse) {
		this.response = Collections.singleton(functionalLiteralResponse);
	}

	public Object getFunctionalObjectResponse() {
		if (response == null) {
			response = nextResponse();
		}
		if (response.size() == 1) {
			return response.iterator().next();
		}
		return null;
	}

	public void setFunctionalObjectResponse(Object functionalObjectResponse) {
		this.response = Collections.singleton(functionalObjectResponse);
	}

	@SuppressWarnings("unchecked")
	public Set<Object> getLiteralResponse() {
		if (response == null) {
			response = nextResponse();
		}
		return (Set<Object>) response;
	}

	public void setLiteralResponse(Set<?> literalResponse) {
		this.response = literalResponse;
	}

	@SuppressWarnings("unchecked")
	public Set<Object> getObjectResponse() {
		if (response == null) {
			response = nextResponse();
		}
		return (Set<Object>) response;
	}

	public void setObjectResponse(Set<?> objectResponse) {
		this.response = objectResponse;
	}

	@SuppressWarnings("unchecked")
	private Set<Object> nextResponse() {
		Method im = null;
		Object it = null;
		try {
			if (count >= invokeTarget.size()) {
				return Collections.emptySet();
			}
			im = invokeMethod.get(count);
			it = invokeTarget.get(count);
			count++;
			Class<?>[] param = im.getParameterTypes();
			if (param.length == 1 && isMessageType(param[0])) {
				Object result = im.invoke(it, as(param[0]));
				// let collected response pass through
				if (Void.TYPE.equals(im.getReturnType())) {
					return (Set<Object>) response;
				}
				if (im.getReturnType().equals(Set.class)) {
					return (Set<Object>) result;
				}
				return Collections.singleton(result);
			}
			Object result = im.invoke(it, getParameters(im));
			if (im.getReturnType().equals(Set.class)) {
				return (Set<Object>) result;
			}
			if (isNil(result, im.getReturnType())) {
				Set<Object> set = nextResponse();
				if (set != null && !set.isEmpty()) {
					return set;
				}
			}
			return Collections.singleton(result);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			if (cause instanceof Error)
				throw (Error) cause;
			throw new BehaviourException(cause);
		} catch (IllegalArgumentException e) {
			throw new BehaviourException(e);
		} catch (IllegalAccessException e) {
			throw new BehaviourException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T as(Class<T> type) {
		if (this.type != null && type.isAssignableFrom(this.type)) {
			type = (Class<T>) this.type;
		}
		ClassLoader cl = type.getClassLoader();
		Class<?>[] types = new Class<?>[] { type };
		return type.cast(Proxy.newProxyInstance(cl, types, this));
	}

	private boolean isNil(Object result, Class<?> type) {
		if (result == null)
			return true;
		if (!type.isPrimitive())
			return false;
		return result.equals(nil(type));
	}

	private static Object nil(Class<?> type) {
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

	private boolean isMessageType(Class<?> type) {
		if (!type.isInterface())
			return false;
		Iri ann = type.getAnnotation(Iri.class);
		if (ann != null && OBJ.MESSAGE.equals(ann.value()))
			return true;
		for (Class<?> s : type.getInterfaces()) {
			if (isMessageType(s))
				return true;
		}
		return false;
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

	private Object[] getParameters(Method method) {
		Object[] parameters = getParameters();
		Annotation[][] anns = method.getParameterAnnotations();
		Object[] result = new Object[anns.length];
		for (int i = 0; i < anns.length; i++) {
			if (i < parameters.length) {
				// if no @rdf copy over parameter by position
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

}