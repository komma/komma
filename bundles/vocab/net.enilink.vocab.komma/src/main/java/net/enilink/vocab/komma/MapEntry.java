/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.vocab.komma;

import net.enilink.vocab.owl.Thing;
import net.enilink.komma.core.IEntity;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/komma#MapEntry")
public interface MapEntry extends Thing, IEntity {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#key")
	Object getKommaKey();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaKey(Object kommaKey);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#keyData")
	Object getKommaKeyData();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaKeyData(Object kommaKeyData);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#value")
	Set<Object> getKommaValue();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaValue(Set<?> kommaValue);

	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#valueData")
	Object getKommaValueData();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaValueData(Object kommaValueData);

}
