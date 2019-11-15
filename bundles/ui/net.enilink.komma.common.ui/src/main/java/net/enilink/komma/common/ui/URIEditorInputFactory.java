/**
 * <copyright>
 *
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: URIEditorInputFactory.java,v 1.1 2007/04/25 21:09:43 emerks Exp $
 */
package net.enilink.komma.common.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

/**
 * A implementation of an {@link IElementFactory} for creating
 * {@link URIEditorInput} instances.
 */
public class URIEditorInputFactory implements IElementFactory {
	public static final String ID = URIEditorInputFactory.class.getName();

	public URIEditorInputFactory() {
		super();
	}

	public IAdaptable createElement(IMemento memento) {
		return URIEditorInput.create(memento);
	}
}
