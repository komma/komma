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
package net.enilink.commons.util;

/**
 *
 * @author Ken Wenzel
 */
public enum ValueType {
    BOOLEAN, BYTE, CHARACTER, SHORT, INTEGER, LONG, BIGINTEGER, FLOAT, DOUBLE, BIGDECIMAL, ANY;
    public static ValueType max(ValueType t1, ValueType t2) {
        return (t1.compareTo(t2) >= 0) ? t1 : t2;
    }
}
