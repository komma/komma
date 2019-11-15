/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.asm;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.ClassDefiner;
import net.enilink.composition.asm.meta.ClassInfo;
import net.enilink.composition.asm.util.MethodNodeGenerator;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Represents the mutable structure of a Java class.
 */
public abstract class ExtendedClassNode extends ClassNode {
	public static final String INJECTOR_FIELD = "_$injector";

	private MethodNodeGenerator clinitGen;
	private final List<MethodNode> constructors = new ArrayList<MethodNode>();
	private final Map<Object, ExtendedMethod> extendedMethods = new HashMap<Object, ExtendedMethod>();
	private Map<String, FieldNode> fieldIndex;
	private final Class<?> parentClass;

	private final Type parentType;

	private Type type;

	/**
	 * Creates an {@link ExtendedClassNode}.
	 * 
	 * @param type
	 *            Java type that gets defined by this class node.
	 * @param parentClass
	 *            The direct super class or the primary interface for this class
	 *            node.
	 * @param parentClassInfo
	 *            Optional meta information for <code>parentClass</code>.
	 */
	public ExtendedClassNode(Type type, Class<?> parentClass,
			ClassInfo parentClassInfo) {
		super(Opcodes.ASM5);
		String[] interfaces = new String[parentClass.getInterfaces().length];
		int i = 0;
		for (Class<?> face : parentClass.getInterfaces()) {
			interfaces[i++] = Type.getInternalName(face);
		}

		Class<?> superClass = parentClass.isInterface() ? Object.class
				: parentClass;
		visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, type.getInternalName(), null,
				Type.getInternalName(superClass), interfaces);

		if (parentClassInfo != null) {
			parentClassInfo.copyAnnotations(this);
		}

		this.type = type;
		this.parentClass = parentClass;
		parentType = Type.getType(parentClass);
	}

	@SuppressWarnings("unchecked")
	public ExtendedMethod addExtendedMethod(Method method, ClassDefiner definer)
			throws Exception {
		Object key = getKey(method);

		ExtendedMethod extendedMethod = extendedMethods.get(key);
		if (extendedMethod == null) {
			extendedMethod = new ExtendedMethod(this, method);

			MethodNode faceNode = AsmUtils.getClassInfo(
					method.getDeclaringClass().getName(), definer).getMethod(
					org.objectweb.asm.commons.Method.getMethod(method));
			faceNode.accept(extendedMethod);

			methods.add(extendedMethod);
			extendedMethods.put(key, extendedMethod);

			extendedMethod.access &= ~Opcodes.ACC_ABSTRACT;
		}

		return extendedMethod;
	}

	@SuppressWarnings("unchecked")
	public void addField(FieldNode field) {
		if (fieldIndex == null) {
			fieldIndex = new HashMap<String, FieldNode>();
		}
		fieldIndex.put(field.name, field);
		fields.add(field);
	}

	@SuppressWarnings("unchecked")
	public void addInjectorField() {
		FieldNode injectorField = new FieldNode(Opcodes.ACC_PRIVATE,
				INJECTOR_FIELD, Type.getDescriptor(Injector.class), null, null);
		injectorField.visitAnnotation(Type.getDescriptor(Inject.class), true);
		fields.add(injectorField);
	}

	@SuppressWarnings("unchecked")
	public void addInterface(String internalName) {
		if (!interfaces.contains(internalName)) {
			interfaces.add(internalName);
		}
	}

	public void endClass() {
		if (clinitGen != null) {
			clinitGen.returnValue();
			clinitGen.endMethod();
		}
	}

	@SuppressWarnings("unchecked")
	public MethodNodeGenerator getClassInitGen() {
		if (clinitGen == null) {
			MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>",
					"()V", null, null);
			methods.add(clinit);
			clinitGen = new MethodNodeGenerator(clinit);
		}
		return clinitGen;
	}

	public List<MethodNode> getConstructors() {
		return constructors;
	}

	public ExtendedMethod getExtendedMethod(Method method) {
		return extendedMethods.get(getKey(method));
	}

	public ExtendedMethod getExtendedMethod(Method method, ClassDefiner definer)
			throws Exception {
		return extendedMethods.get(getKey(method));
	}

	public Collection<ExtendedMethod> getExtendedMethods() {
		return extendedMethods.values();
	}

	public FieldNode getField(String name) {
		if (fieldIndex == null) {
			return null;
		}
		return fieldIndex.get(name);
	}

	private Object getKey(Method method) {
		return Arrays.asList(method.getReturnType(), method.getName(),
				Arrays.asList(method.getParameterTypes()));
	}

	public Class<?> getParentClass() {
		return parentClass;
	}

	public Type getParentType() {
		return parentType;
	}

	public Type getType() {
		return type;
	}
}
