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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Represents a mutable method that knows its owner class.
 */
public class ExtendedMethod extends org.objectweb.asm.tree.MethodNode {
	private ExtendedClassNode owner;
	private Method method;

	public ExtendedMethod(ExtendedClassNode owner, int access, String name,
			String desc, String signature, String[] exceptions) {
		super(Opcodes.ASM7, access, name, desc, signature, exceptions);
		this.owner = owner;
	}

	public ExtendedMethod(ExtendedClassNode owner, Method method) {
		super(Opcodes.ASM7, method.getModifiers(), method.getName(), Type
				.getMethodDescriptor(method), null, null);
		this.owner = owner;
		this.method = method;
	}

	public Method getOverriddenMethod() {
		return method;
	}

	public ExtendedClassNode getOwner() {
		return owner;
	}
}