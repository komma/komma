/*
 * Copyright (c) 2009, 2010, James Leigh All rights reserved.
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
package net.enilink.composition.properties.sparql;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.BehaviourMethodProcessor;
import net.enilink.composition.asm.ExtendedMethod;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.util.BehaviourMethodGenerator;
import net.enilink.composition.asm.util.MethodNodeGenerator;
import net.enilink.composition.exceptions.BehaviourException;
import net.enilink.composition.properties.PropertyMapper;

import com.google.inject.Inject;

import net.enilink.komma.core.IEntityManager;

/**
 * Generate a behaviour for {@link sparql} annotated methods.
 * 
 */
public class SparqlBehaviourMethodProcessor implements
		BehaviourMethodProcessor, Opcodes, Types {

	public static Type MANAGER_TYPE = Type.getType(IEntityManager.class);
	public static org.objectweb.asm.commons.Method GET_MANAGER = new org.objectweb.asm.commons.Method(
			"getContext", Type.getMethodDescriptor(MANAGER_TYPE, new Type[0]));

	@Inject
	private PropertyMapper propertyMapper;

	@Override
	public boolean implementsMethod(Class<?> targetClass, Method method) {
		return method.isAnnotationPresent(sparql.class)
				&& Modifier.isAbstract(method.getModifiers());
	}

	@Override
	public boolean appliesTo(BehaviourClassNode classNode, ExtendedMethod method) {
		return method.getMethodDescriptor().isAnnotationPresent(sparql.class)
				&& method.instructions.size() == 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(BehaviourClassNode classNode) throws Exception {
		FieldNode contextField = new FieldNode(Opcodes.ACC_PRIVATE, "context",
				MANAGER_TYPE.getDescriptor(), null, null);
		contextField.visitAnnotation(Type.getDescriptor(Inject.class), true);
		classNode.fields.add(contextField);

		MethodNode mn = new MethodNode(ACC_PRIVATE, GET_MANAGER.getName(),
				GET_MANAGER.getDescriptor(), null, null);

		MethodNodeGenerator gen = new MethodNodeGenerator(mn);
		gen.loadThis();
		gen.getField(classNode.getType(), "context", MANAGER_TYPE);
		gen.returnValue();
		gen.endMethod();

		classNode.methods.add(mn);
	}

	@Override
	public void process(BehaviourClassNode classNode, ExtendedMethod method)
			throws Exception {
		BehaviourMethodGenerator gen = new BehaviourMethodGenerator(method);

		String sparql = method.getMethodDescriptor()
				.getAnnotation(sparql.class).value();
		String base;
		if (method.getMethodDescriptor().getDeclaringClass()
				.isAnnotationPresent(Iri.class)) {
			base = method.getMethodDescriptor().getDeclaringClass()
					.getAnnotation(Iri.class).value();
		} else {
			base = "java:"
					+ method.getMethodDescriptor().getDeclaringClass()
							.getName();
		}

		Label tryLabel = gen.mark();
		// try
		SPARQLQueryOptimizer oqo = new SPARQLQueryOptimizer();
		oqo.implementQuery(sparql, base, propertyMapper,
				method.getMethodDescriptor(), gen);

		// catch (RuntimeException e) {
		// throw e;
		// }
		Label catchLabel = gen.mark();
		Type runtimeException = Type.getType(RuntimeException.class);
		gen.catchException(tryLabel, catchLabel, runtimeException);
		gen.throwException();

		// catch (Exception e) {
		// throw new BehaviourException(e);
		// }
		gen.catchException(tryLabel, catchLabel, Type.getType(Exception.class));
		int exceptionVar = gen.newLocal(Type.getType(Exception.class));
		gen.storeLocal(exceptionVar);

		Type behaviourException = Type.getType(BehaviourException.class);
		gen.newInstance(behaviourException);
		gen.dup();
		gen.loadLocal(exceptionVar);
		gen.invokeConstructor(behaviourException,
				org.objectweb.asm.commons.Method
						.getMethod("void <init>(Throwable)"));
		gen.throwException();
	}

}
