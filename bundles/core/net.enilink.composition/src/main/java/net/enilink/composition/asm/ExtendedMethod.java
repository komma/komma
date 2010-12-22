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

import org.objectweb.asm.Type;

/**
 * Represents a mutable method that knows its owner class.
 * 
 * @author Ken Wenzel
 *
 */
public class ExtendedMethod extends org.objectweb.asm.tree.MethodNode {
	private ExtendedClassNode owner;
	private Method method;

	public ExtendedMethod(ExtendedClassNode owner, int access, String name,
			String desc, String signature, String[] exceptions) {
		super(access, name, desc, signature, exceptions);
		this.owner = owner;
	}

	public ExtendedMethod(ExtendedClassNode owner, Method method) {
		super(method.getModifiers(), method.getName(), Type
				.getMethodDescriptor(method), null, null);
		this.owner = owner;
		this.method = method;
	}

	public Method getMethodDescriptor() {
		return method;
	}

	public ExtendedClassNode getOwner() {
		return owner;
	}
}