package net.enilink.composition.helpers;

import java.lang.reflect.Method;

abstract class Methods {
	public static final Method METHODINVOCATIONCHAIN_ISNIL;
	public static final Method METHODINVOCATIONCHAIN_CAST;
	public static final Method METHODINVOCATIONCHAIN_NIL;

	static {
		METHODINVOCATIONCHAIN_ISNIL = getMethod(MethodInvocationChain.class,
				"isNil", Object.class, Class.class);
		METHODINVOCATIONCHAIN_CAST = getMethod(MethodInvocationChain.class,
				"cast", Object.class, Class.class, Class.class);
		METHODINVOCATIONCHAIN_NIL = getMethod(MethodInvocationChain.class,
				"nil", Class.class);
	}

	private static Method getMethod(Class<?> owner, String name,
			Class<?>... parameterTypes) {
		try {
			return owner.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException nse) {
			throw new RuntimeException("Required method was not found", nse);
		}
	}
}