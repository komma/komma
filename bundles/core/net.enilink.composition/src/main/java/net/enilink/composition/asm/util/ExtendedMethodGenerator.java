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
package net.enilink.composition.asm.util;

import org.objectweb.asm.Type;
import net.enilink.composition.asm.ExtendedClassNode;
import net.enilink.composition.asm.ExtendedMethod;

import com.google.inject.Injector;

/**
 * Generator for {@link ExtendedMethod}s.
 * 
 * @author Ken Wenzel
 * 
 */
public class ExtendedMethodGenerator extends MethodNodeGenerator {

	public ExtendedMethodGenerator(ExtendedMethod mn) {
		super(mn);
	}

	public ExtendedMethodGenerator(int access, String name, String desc,
			String signature, String[] exceptions) {
		super(access, name, desc, signature, exceptions);
	}

	public ExtendedMethodGenerator(int access, String name, String desc) {
		super(access, name, desc);
	}

	/**
	 * Generates the instruction to push the value of a static field on the
	 * stack.
	 * 
	 * @param name
	 *            the name of the field.
	 * @param type
	 *            the type of the field.
	 */
	public void getStatic(final String name, final Type type) {
		getStatic(getMethod().getOwner().getType(), name, type);
	}

	/**
	 * Generates the instruction to store the top stack value in a static field.
	 * 
	 * @param name
	 *            the name of the field.
	 * @param type
	 *            the type of the field.
	 */
	public void putStatic(final String name, final Type type) {
		putStatic(getMethod().getOwner().getType(), name, type);
	}

	/**
	 * Generates the instruction to push the value of a non static field on the
	 * stack.
	 * 
	 * @param name
	 *            the name of the field.
	 * @param type
	 *            the type of the field.
	 */
	public void getField(final String name, final Type type) {
		getField(getMethod().getOwner().getType(), name, type);
	}

	/**
	 * Generates the instruction to store the top stack value in a non static
	 * field.
	 * 
	 * @param name
	 *            the name of the field.
	 * @param type
	 *            the type of the field.
	 */
	public void putField(final String name, final Type type) {
		putField(getMethod().getOwner().getType(), name, type);
	}

	public ExtendedMethod getMethod() {
		return (ExtendedMethod) mn;
	}

	public void injectMembers() {
		dup();
		loadThis();
		getField(getMethod().getOwner().getType(),
				ExtendedClassNode.INJECTOR_FIELD, Type.getType(Injector.class));
		swap();
		invokeInterface(Type.getType(Injector.class),
				org.objectweb.asm.commons.Method
						.getMethod("void injectMembers(Object)"));
	}

}