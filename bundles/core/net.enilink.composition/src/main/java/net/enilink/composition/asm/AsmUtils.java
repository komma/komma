/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.asm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import net.enilink.composition.ClassDefiner;
import net.enilink.composition.annotations.parameterTypes;
import net.enilink.composition.asm.meta.ClassInfo;

/**
 * Utility methods for the transformation process.
 * 
 */
public class AsmUtils {
	protected static Map<Reference<ClassLoader>, Map<String, ClassInfo>> classInfos = new ConcurrentHashMap<Reference<ClassLoader>, Map<String, ClassInfo>>();

	public static ClassInfo getClassInfo(String className,
			ClassLoader classLoader) throws Exception {
		Reference<ClassLoader> loaderRef = new WeakReference<ClassLoader>(
				classLoader);
		Map<String, ClassInfo> infoMap = classInfos.get(loaderRef);
		if (infoMap == null) {
			infoMap = new ConcurrentHashMap<String, ClassInfo>();
			classInfos.put(loaderRef, infoMap);
		}
		ClassInfo classInfo = infoMap.get(className);
		if (classInfo == null) {
			ClassReader reader = createClassReader(className, classLoader);
			classInfo = new ClassInfo();
			reader.accept(classInfo, ClassReader.SKIP_FRAMES);
			infoMap.put(className, classInfo);
		}
		return classInfo;
	}

	public static ClassReader createClassReader(String className,
			ClassLoader classLoader) throws ClassNotFoundException {
		String classFilename = className.replace('.', '/') + ".class";
		InputStream inputStream = classLoader
				.getResourceAsStream(classFilename);
		try {
			return new ClassReader(inputStream);
		} catch (IOException e) {
			throw new ClassNotFoundException("Class not found: " + className, e);
		}
	}

	public static Class<?> defineExtendedClass(ClassDefiner definer,
			ExtendedClassNode classNode) {
		classNode.endClass();

		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);

		// printClass(classWriter.toByteArray());
		verifyClass(classNode, definer, classWriter.toByteArray());

		Class<?> newClass = definer.defineClass(
				classNode.name.replace('/', '.'), classWriter.toByteArray());

		return newClass;
	}

	protected static void verifyClass(ExtendedClassNode classNode,
			ClassDefiner definer, byte[] code) {
		PrintWriter writer = new PrintWriter(System.err);
		CheckClassAdapter.verify(new ClassReader(code), definer, false, writer);
	}

	protected static void printClass(byte[] code) {
		ClassReader cr = new ClassReader(code);
		PrintWriter writer = new PrintWriter(System.err);
		cr.accept(new TraceClassVisitor(writer), 0);
	}

	/**
	 * Returns the class with the given name if it can been loaded by the given
	 * class loader. Otherwise the method returns null.
	 * 
	 * @param className
	 *            the full name of the class
	 * @param classLoader
	 *            the class loader to use
	 * @return the class instance or null
	 */
	public static Class<?> findClass(String className, ClassLoader classLoader) {
		try {
			return classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static <T extends Annotation> T findAnnotation(
			Class<T> annotationClass, Method method) {
		T annotation = method.getAnnotation(annotationClass);

		if (annotation != null) {
			return annotation;
		}

		Method superMethod = findInterfaceOrSuperMethod(method, method
				.getDeclaringClass().getSuperclass(), method
				.getDeclaringClass().getInterfaces());
		if (superMethod != null && !method.equals(superMethod)) {
			annotation = findAnnotation(annotationClass, superMethod);
		}
		return annotation;
	}

	public static Method findInterfaceOrSuperMethod(Method method,
			Class<?> baseClass, Class<?>... interfaces) {
		String name = method.getName();
		Class<?> type = method.getReturnType();
		Class<?>[] types = getParameterTypes(method);
		Method m = findInterfaceMethod(interfaces, name, type, types);
		if (m != null) {
			return m;
		}
		m = findSuperMethod(baseClass, name, type, types);
		if (m != null) {
			return m;
		}
		return method;
	}

	public static Class<?>[] getParameterTypes(Method m) {
		if (m.isAnnotationPresent(parameterTypes.class)) {
			return m.getAnnotation(parameterTypes.class).value();
		}
		return m.getParameterTypes();
	}

	private static Method findSuperMethod(Class<?> base, String name,
			Class<?> type, Class<?>[] types) {
		if (base == null)
			return null;
		try {
			Method m = base.getDeclaredMethod(name, types);
			if (m.getReturnType().equals(type)) {
				return m;
			}
		} catch (NoSuchMethodException e) {
			// continue
		}
		Method m = findSuperMethod(base.getSuperclass(), name, type, types);
		if (m == null) {
			return null;
		}
		return m;
	}

	private static Method findInterfaceMethod(Class<?>[] interfaces,
			String name, Class<?> type, Class<?>[] types) {
		for (Class<?> face : interfaces) {
			try {
				Method m = face.getDeclaredMethod(name, types);
				if (m.getReturnType().equals(type)) {
					return m;
				}
			} catch (NoSuchMethodException e) {
				// continue
			}
			Class<?>[] faces = face.getInterfaces();
			Method m = findInterfaceMethod(faces, name, type, types);
			if (m != null) {
				return m;
			}
		}
		return null;
	}
}
