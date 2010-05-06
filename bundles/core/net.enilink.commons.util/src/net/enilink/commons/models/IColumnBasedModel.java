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

public interface IColumnBasedModel<E> {
	int getColumnCount();
	String getColumnName(int column);
	Object getValue(E element, int columnIndex);
	boolean isValueEditable(E element, int columnIndex);
	void setValue(E element, Object value, int columnIndex);
}
