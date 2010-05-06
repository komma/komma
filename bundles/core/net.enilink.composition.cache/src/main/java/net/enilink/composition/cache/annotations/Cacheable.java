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
package net.enilink.composition.cache.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.vocabulary.OBJ;

/**
 * Return values of methods with this annotation will be cached.
 * 
 * @author Ken Wenzel
 */
@Iri(OBJ.NAMESPACE + "Cached")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Cacheable {
	/**
	 * Key that is used for caching the value.
	 */
	String key() default "";
}