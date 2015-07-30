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

/**
 * Implements or transforms {@link BehaviourClassNode}s.
 */
public interface BehaviourClassProcessor {
	/**
	 * Returns <code>true</code> if this processor is able to implement the
	 * given <code>behaviourClass</code>.
	 * 
	 * @param behaviourClass
	 *            The class that could be implemented.
	 * @return <code>true</code> if this processor implements the class, else
	 *         <code>false</code>.
	 */
	boolean implementsClass(Class<?> behaviourClass);

	/**
	 * Implements or transforms the given <code>classNode</code>.
	 * 
	 * @param classNode
	 *            The class that should be implemented or transformed.
	 * @throws Exception
	 *             An exception if the processing has been failed.
	 */
	void process(BehaviourClassNode classNode) throws Exception;
}