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

/**
 * The default factory that is used to transform abstract behaviours into
 * concrete behaviours.
 */
public class DefaultBehaviourFactory extends BehaviourFactoryBase {
	public static final String PKG_PREFIX = "object.behaviours.";
	
	@Override
	protected String getExtendedClassName(Class<?> behaviourClass) {
		return PKG_PREFIX + behaviourClass.getName() + "$$behaviour";
	}
}
