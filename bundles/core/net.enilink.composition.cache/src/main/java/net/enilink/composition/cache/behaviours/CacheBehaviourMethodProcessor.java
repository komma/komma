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
package net.enilink.composition.cache.behaviours;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldNode;
import net.enilink.composition.asm.AsmUtils;
import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.asm.DependsOn;
import net.enilink.composition.asm.ExtendedMethod;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.BehaviourMethodGenerator;
import net.enilink.composition.cache.IPropertyCache;
import net.enilink.composition.cache.annotations.Cacheable;

import com.google.inject.Inject;

@DependsOn(BehaviourMethodProcessor.class)
public class CacheBehaviourMethodProcessor implements BehaviourMethodProcessor,
		Opcodes, Types {

	@Override
	public boolean appliesTo(BehaviourClassNode classNode, ExtendedMethod method) {
		return AsmUtils.findAnnotation(Cacheable.class, method
				.getMethodDescriptor()) != null
				&& !method.getMethodDescriptor().getReturnType().equals(
						Void.TYPE);
	}

	@Override
	public boolean implementsMethod(Class<?> targetClass, Method method) {
		return !Modifier.isAbstract(method.getModifiers())
				&& AsmUtils.findAnnotation(Cacheable.class, method) != null
				&& !method.getReturnType().equals(Void.TYPE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(BehaviourClassNode classNode) throws Exception {
		FieldNode cacheField = new FieldNode(Opcodes.ACC_PRIVATE, "cache", Type
				.getDescriptor(IPropertyCache.class), null, null);
		cacheField.visitAnnotation(Type.getDescriptor(Inject.class), true);
		classNode.fields.add(cacheField);
	}

	@Override
	public void process(BehaviourClassNode classNode, ExtendedMethod method)
			throws Exception {
		boolean isAbstract = method.instructions.size() == 0;

		Type returnType = Type.getReturnType(method.desc);

		Cacheable cacheable = AsmUtils.findAnnotation(Cacheable.class, method
				.getMethodDescriptor());
		String cacheKey = cacheable.key().isEmpty() ? method.name : cacheable
				.key();

		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(method);
		if (!isAbstract) {
			gen.pushInsns();
		}

		gen.loadThis();
		gen.getField("cache", Type.getType(IPropertyCache.class));
		gen.loadBean();
		gen.push(cacheKey);
		gen.loadArgArray();
		gen.invokeInterface(Type.getType(IPropertyCache.class),
				new org.objectweb.asm.commons.Method("get", OBJECT_TYPE,
						new Type[] { OBJECT_TYPE, OBJECT_TYPE,
								Type.getType(Object[].class) }));
		gen.dup();

		Label isNull = gen.newLabel();
		gen.ifNull(isNull);

		gen.unbox(returnType);
		gen.returnValue();

		gen.mark(isNull);
		gen.pop();

		if (!isAbstract) {
			gen.peekInsns().insertBefore(gen.peekInsns().getFirst(),
					gen.instructions);
			gen.instructions.clear();
		}

		int result = gen.newLocal(returnType);

		if (!isAbstract) {
			// add caching instructions
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn.getOpcode() >= IRETURN & insn.getOpcode() <= ARETURN) {
					gen.pushInsns();

					storeValue(result, returnType, cacheKey, gen);

					gen.peekInsns().insertBefore(insn, gen.instructions);
					gen.instructions.clear();
				}
			}
		} else {
			// generate super call and store value in cache
			gen.loadThis();
			gen.loadArgs();
			gen.invokeSpecial(classNode.getParentType(),
					new org.objectweb.asm.commons.Method(method.name,
							method.desc));
			storeValue(result, returnType, cacheKey, gen);
			gen.returnValue();
		}
	}

	private void storeValue(int resultVar, Type type, String cacheKey,
			BehaviourMethodGenerator gen) {
		gen.storeLocal(resultVar);

		gen.loadThis();
		gen.getField("cache", Type.getType(IPropertyCache.class));
		gen.loadBean();
		gen.push(cacheKey);
		gen.loadArgArray();
		gen.loadLocal(resultVar);
		gen.box(type);
		gen.invokeInterface(Type.getType(IPropertyCache.class),
				new org.objectweb.asm.commons.Method("put", OBJECT_TYPE,
						new Type[] { OBJECT_TYPE, OBJECT_TYPE,
								Type.getType(Object[].class), OBJECT_TYPE }));
		gen.unbox(type);
	}
}