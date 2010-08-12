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
package net.enilink.composition.properties.komma;

import java.beans.PropertyDescriptor;

import net.enilink.composition.properties.PropertySetDescriptor;
import net.enilink.composition.properties.PropertySetDescriptorFactory;

public class KommaPropertySetDescriptorFactory implements
		PropertySetDescriptorFactory {
	@Override
	public <E> PropertySetDescriptor<E> createDescriptor(
			PropertyDescriptor property, String uri, boolean readOnly) {
		return new KommaPropertySetDescriptor<E>(property, uri);
	}
}
