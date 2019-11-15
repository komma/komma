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
package net.enilink.composition.asm.processors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.asm.CompositeClassNode;
import net.enilink.composition.asm.CompositeClassProcessor;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.MethodNodeGenerator;

/**
 * Creates the byte code to implement the constructor of a composite class.
 */
public class CompositeConstructorGenerator implements CompositeClassProcessor,
		Opcodes, Types {
	@Override
	@SuppressWarnings("unchecked")
	public void process(CompositeClassNode classNode) throws Exception {
		MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V",
				null, null);

		// call the constructor of the parent class
		MethodNodeGenerator gen = new MethodNodeGenerator(constructor);
		gen.loadThis();
		gen.invokeConstructor(classNode.getParentType(), new Method("<init>",
				"()V"));
		gen.returnValue();
		gen.endMethod();

		classNode.methods.add(constructor);
		classNode.getConstructors().add(constructor);
	}
}
