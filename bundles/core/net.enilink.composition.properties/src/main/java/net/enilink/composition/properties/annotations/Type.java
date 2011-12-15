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
package net.enilink.composition.properties.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.vocabulary.OBJ;

/**
 * Methods with this annotation will use the configured type instead of the
 * actual return type of the method. Intended to help with the internal mapping
 * and ease-of-use for lists etc. while keeping the bean interface clean.
 * 
 * @author Fraunhofer IWU
 */
@Iri(OBJ.NAMESPACE + "Type")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Type {
	String value();
}
