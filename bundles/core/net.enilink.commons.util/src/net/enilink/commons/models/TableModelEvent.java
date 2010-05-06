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

public class TableModelEvent extends ListModelEvent {
	public int count;
	
	public TableModelEvent(Type type, int count) {
		this(type, NO_INDEX, count);
	}

	public TableModelEvent(Type type, int index, int count) {
		super(type, index, null);
		this.type = type;
		this.index = index;
		this.count = count;
	}
	
	public TableModelEvent(Type type, int index, Object[] elements) {
		super(type, index, elements);
		this.type = type;
		this.index = index;
		this.count = elements == null ? 0 : elements.length;
	}
}
