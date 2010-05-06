/*******************************************************************************
 * Copyright (c) 2009 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.models;

public class ListModelEvent {
	public static final int NO_INDEX = -1;
	
	public static enum Type { ADDED, REMOVED, CHANGED };
	public Type type;
	public int index;
	public Object[] elements;
	
	public ListModelEvent(Type type, Object[] elements) {
		this(type, NO_INDEX, elements);
	}

	public ListModelEvent(Type type, int index, Object[] elements) {
		this.type = type;
		this.index = index;
		this.elements = elements;
	}
}
