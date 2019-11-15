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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.BehaviourClassProcessor;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.MethodNodeGenerator;
import net.enilink.composition.traits.Behaviour;

/**
 * Creates the byte code to implement the {@link Behaviour} interface.
 */
public class BehaviourInterfaceImplementor implements BehaviourClassProcessor,
		Opcodes, Types {
	@Override
	@SuppressWarnings("unchecked")
	public void process(BehaviourClassNode classNode) throws Exception {
		// add Behaviour interface if not already present
		if (!Behaviour.class.isAssignableFrom(classNode.getParentClass())) {
			classNode.addInterface(BEHAVIOUR_TYPE.getInternalName());
		}

		MethodNode mn = new MethodNode(ACC_PUBLIC | ACC_TRANSIENT,
				Behaviour.GET_ENTITY_METHOD, Type.getMethodDescriptor(
						OBJECT_TYPE, new Type[0]), null, null);

		MethodNodeGenerator gen = new MethodNodeGenerator(mn);
		gen.loadThis();
		gen.getField(classNode.getType(), "_$bean", OBJECT_TYPE);
		gen.returnValue();
		gen.endMethod();

		classNode.methods.add(mn);
	}

	@Override
	public boolean implementsClass(Class<?> behaviourClass) {
		return false;
	}
}
