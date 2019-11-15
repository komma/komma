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

/**
 * Implements or transforms methods of a {@link BehaviourClassNode}.
 * 
 * @see DependsOn
 * @see UseWith
 */
public interface BehaviourMethodProcessor {
	/**
	 * Returns <code>true</code> if this processor is able to implement
	 * <code>method</code> of <code>targetClass</code>. Implementing the given
	 * <code>method</code> does either mean to create an implementation for an
	 * abstract method or to override a superclass method.
	 * 
	 * @param targetClass
	 *            The defining class of the respective method.
	 * @param method
	 *            The method that can be implemented.
	 * @return <code>true</code> if this processor is able to create an
	 *         implementation for <code>method</code>, else <code>false</code>.
	 */
	boolean implementsMethod(Class<?> targetClass, Method method);

	/**
	 * Called by the container directly before
	 * {@link BehaviourMethodProcessor#process(BehaviourClassNode, ExtendedMethod)}
	 * is called. Returns <code>true</code> if this processor can be applied to
	 * the given <code>method</code>. If
	 * {@link BehaviourMethodProcessor#implementsMethod(Class, Method)} returns
	 * <code>true</code> then
	 * {@link BehaviourMethodProcessor#appliesTo(BehaviourClassNode, ExtendedMethod)}
	 * must also return <code>true</code> for the same method.
	 * 
	 * @param classNode
	 *            The owner of the respective method.
	 * @param method
	 *            The method that should be implemented or transformed.
	 * @return <code>true</code> if this processor is able to implement or
	 *         transform the <code>method</code>, else <code>false</code>.
	 */
	boolean appliesTo(BehaviourClassNode classNode, ExtendedMethod method);

	/**
	 * Called once for each distinct <code>classNode</code> before
	 * {@link BehaviourMethodProcessor#process(BehaviourClassNode, ExtendedMethod)}
	 * is executed.
	 * 
	 * @param classNode
	 *            The class node that should be initialized.
	 * @throws Exception
	 *             An exception if initialization has been failed.
	 */
	void initialize(BehaviourClassNode classNode) throws Exception;

	/**
	 * Implements or transforms the given <code>method</code>.
	 * 
	 * @param classNode
	 *            The owner of the respective method.
	 * @param method
	 *            The method to implement or transform.
	 * @throws Exception
	 *             An exception if processing has been failed.
	 */
	void process(BehaviourClassNode classNode, ExtendedMethod method)
			throws Exception;
}