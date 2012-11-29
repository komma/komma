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

import org.objectweb.asm.Type;
import net.enilink.composition.asm.meta.ClassInfo;

/**
 * Represents the mutable structure of a behaviour class.
 */
public class BehaviourClassNode extends ExtendedClassNode {
	public BehaviourClassNode(Type type, Class<?> parentClass,
			ClassInfo parentClassInfo) {
		super(type, parentClass, parentClassInfo);
	}
}
