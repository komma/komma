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
package net.enilink.composition.asm.processors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.BehaviourClassProcessor;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.MethodNodeGenerator;

/**
 * Creates the byte code to implement the constructor of a behaviour class.
 */
public class BehaviourConstructorGenerator implements BehaviourClassProcessor,
		Opcodes, Types {
	@Override
	@SuppressWarnings("unchecked")
	public void process(BehaviourClassNode classNode) throws Exception {
		FieldNode beanField = new FieldNode(ACC_PRIVATE, "_$bean", OBJECT_TYPE
				.getDescriptor(), null, null);
		classNode.fields.add(beanField);

		MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>",
				Type.getMethodDescriptor(Type.VOID_TYPE,
						new Type[] { OBJECT_TYPE }), null, null);

		// call the constructor of the parent class
		MethodNodeGenerator gen = new MethodNodeGenerator(constructor);
		gen.loadThis();
		gen.invokeConstructor(
				classNode.getParentClass().isInterface() ? OBJECT_TYPE
						: classNode.getParentType(),
				new Method("<init>", "()V"));
		gen.loadThis();
		gen.loadArg(0);
		gen.putField(classNode.getType(), "_$bean", OBJECT_TYPE);
		gen.returnValue();
		gen.endMethod();

		classNode.methods.add(constructor);
		classNode.getConstructors().add(constructor);
	}

	@Override
	public boolean implementsClass(Class<?> behaviourClass) {
		return false;
	}
}
