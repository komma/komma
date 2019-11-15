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

import org.objectweb.asm.Type;
import net.enilink.composition.helpers.MethodInvocationChain;
import net.enilink.composition.traits.Behaviour;

/**
 * Common type definitions.
 */
public interface Types {
	final Type OBJECT_TYPE = Type.getType(Object.class);
	final Type CLASS_TYPE = Type.getType(Class.class);
	final Type METHOD_TYPE = Type.getType(Method.class);
	final Type STRING_TYPE = Type.getType(String.class);
	
	final Type BEHAVIOUR_TYPE = Type.getType(Behaviour.class);
	final Type METHODINVOCATIONCHAIN_TYPE = Type
			.getType(MethodInvocationChain.class);
}