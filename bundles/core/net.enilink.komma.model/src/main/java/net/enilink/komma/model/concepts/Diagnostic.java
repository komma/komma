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
package net.enilink.komma.model.concepts;

import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.IEntity;


/** 
 * A noteworthy issue in a document.
 * @generated 
 */
@Iri("http://enilink.net/vocab/komma/models#Diagnostic")
public interface Diagnostic extends IEntity {
	/** 
	 * Returns the column location of the issue within the source. Column 1 is the first column.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#column")
	int getColumn();
	/** 
	 * Returns the column location of the issue within the source. Column 1 is the first column.
	 * @generated 
	 */
	void setColumn(int column);

	/** 
	 * Returns the line location of the issue within the source. Line 1 is the first line.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#line")
	int getLine();
	/** 
	 * Returns the line location of the issue within the source. Line 1 is the first line.
	 * @generated 
	 */
	void setLine(int line);

	/** 
	 * Returns the source location of the issue.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#location")
	String getLocation();
	/** 
	 * Returns the source location of the issue.
	 * @generated 
	 */
	void setLocation(String location);

	/** 
	 * Returns a translated message describing the issue.
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma/models#message")
	String getMessage();
	/** 
	 * Returns a translated message describing the issue.
	 * @generated 
	 */
	void setMessage(String message);

}
