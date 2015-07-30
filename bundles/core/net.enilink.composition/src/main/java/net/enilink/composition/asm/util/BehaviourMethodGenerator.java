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

import net.enilink.composition.asm.BehaviourClassNode;
import net.enilink.composition.asm.ExtendedMethod;
import net.enilink.composition.asm.Types;

/**
 * Generator for methods of {@link BehaviourClassNode}s.
 */
public class BehaviourMethodGenerator extends ExtendedMethodGenerator {
	public BehaviourMethodGenerator(ExtendedMethod mn) {
		super(mn);
	}

	/**
	 * Loads delegate of this behaviour.
	 */
	public void loadBean() {
		loadThis();
		getField(getMethod().getOwner().getType(), "_$bean", Types.OBJECT_TYPE);
	}
}
