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

import java.lang.reflect.Modifier;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.asm.DependsOn;
import net.enilink.composition.asm.ExtendedMethod;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.MethodNodeGenerator;
import net.enilink.composition.traits.Behaviour;

/**
 * Implements abstract behaviour methods by delegating to the respective methods
 * of the composite class.
 */
@DependsOn(
// successor of all other method processors
BehaviourMethodProcessor.class)
public class MethodDelegationGenerator implements BehaviourMethodProcessor,
		Opcodes, Types {
	@Override
	public boolean appliesTo(BehaviourClassNode classNode, ExtendedMethod method) {
		// method was already implemented by other processor
		return method.instructions.size() == 0;
	}

	@Override
	public boolean implementsMethod(Class<?> targetClass,
			java.lang.reflect.Method method) {
		if (targetClass.isInterface()
				|| Modifier.isFinal(method.getModifiers())
				|| !Modifier.isAbstract(method.getModifiers())
				|| Behaviour.class.equals(method.getDeclaringClass())) {
			return false;
		}
		return method.getDeclaringClass().isInterface();
	}

	@Override
	public void initialize(BehaviourClassNode classNode) throws Exception {
		// no initialization required
	}

	@Override
	public void process(BehaviourClassNode classNode, ExtendedMethod method)
			throws Exception {
		// remove abstract modifier and mark as transient
		method.access &= ~ACC_ABSTRACT;
		method.access |= ACC_TRANSIENT;

		MethodNodeGenerator gen = new MethodNodeGenerator(method);
		gen.loadThis();
		gen.invokeInterface(BEHAVIOUR_TYPE, new Method(
				Behaviour.GET_ENTITY_METHOD, OBJECT_TYPE, new Type[0]));

		// if (method.getMethodDescriptor().getDeclaringClass().isInterface()) {

		Type faceType = Type.getType(method.getOverriddenMethod()
				.getDeclaringClass());
		gen.checkCast(faceType);

		gen.loadArgs();
		gen.invokeInterface(faceType, new Method(method.name, method.desc));

		// } else {
		// // reflective call of target method
		// gen.dup();
		// gen.invokeVirtual(OBJECT_TYPE, new Method("getClass", CLASS_TYPE,
		// new Type[0]));
		// gen.push(method.name);
		// gen.invokeVirtual(CLASS_TYPE, new Method("getMethod", METHOD_TYPE,
		// new Type[] { STRING_TYPE, Type.getType(Class[].class) }));
		// gen.swap();
		// gen.loadArgArray();
		// gen.invokeVirtual(METHOD_TYPE, new Method("invoke", OBJECT_TYPE,
		// new Type[] { OBJECT_TYPE, Type.getType(Object[].class) }));
		// }

		gen.returnValue();
		gen.endMethod();
	}
}