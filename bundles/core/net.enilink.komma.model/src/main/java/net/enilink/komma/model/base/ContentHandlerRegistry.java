/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.base;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.enilink.komma.model.IContentHandler;

public class ContentHandlerRegistry extends
		TreeMap<Integer, List<IContentHandler>> implements
		IContentHandler.Registry {
	private static final long serialVersionUID = 1L;

	public void put(int priority, IContentHandler contentHandler) {
		Integer integerPriority = priority;
		List<IContentHandler> contentHandlers = get(integerPriority);
		if (contentHandlers == null) {
			put(integerPriority,
					contentHandlers = new ArrayList<IContentHandler>());
		}
		contentHandlers.add(contentHandler);
	}

	public List<IContentHandler> getContentHandlers() {
		ArrayList<IContentHandler> result = new ArrayList<IContentHandler>();
		for (List<IContentHandler> contentHandlers : values()) {
			result.addAll(contentHandlers);
		}
		return result;
	}
}
