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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import net.enilink.composition.asm.util.MethodNodeGenerator;

/**
 * Represents the mutable structure of a composite class. 
 * 
 * @author Ken Wenzel
 *
 */
public class CompositeClassNode extends ExtendedClassNode {
	protected Set<Class<?>> interfaceClassesSet = new LinkedHashSet<Class<?>>();
	protected Class<?>[] interfaceClasses;

	private Map<List<?>, FieldNode> methodFields = new HashMap<List<?>, FieldNode>();
	private int varCount;

	public CompositeClassNode(Type type, Class<?> parentClass) {
		super(type, parentClass, null);
	}

	@SuppressWarnings("unchecked")
	public void addInterfaceClass(Class<?> face) {
		if (interfaceClassesSet.add(face)) {
			interfaces.add(Type.getInternalName(face));
			interfaceClasses = null;
		}
	}

	public Class<?>[] getInterfacesClasses() {
		if (interfaceClasses == null) {
			interfaceClasses = interfaceClassesSet
					.toArray(new Class<?>[interfaceClassesSet.size()]);
		}
		return interfaceClasses;
	}

	@SuppressWarnings("unchecked")
	public FieldNode addStaticMethodField(Type declaringClass, String name,
			Type returnType, Type[] paramTypes) {
		List<?> key = Arrays.asList(declaringClass, name, Arrays
				.asList(paramTypes));
		FieldNode field = methodFields.get(key);
		if (field == null) {
			field = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
					"_$method" + ++varCount, Type.getDescriptor(Method.class),
					null, null);
			fields.add(field);
			methodFields.put(key, field);

			MethodNodeGenerator gen = getClassInitGen();

			gen.push(declaringClass);
			gen.push(name);
			gen.loadArray(paramTypes);

			gen.invokeVirtual(Type.getType(Class.class),
					new org.objectweb.asm.commons.Method("getDeclaredMethod",
							Type.getType(Method.class), new Type[] {
									Type.getType(String.class),
									Type.getType(Class[].class) }));

			gen.putStatic(getType(), field.name, Type.getType(field.desc));
		}
		return field;
	}
}
