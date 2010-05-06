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
package net.enilink.komma.concepts;

import net.enilink.vocab.owl.Thing;
import net.enilink.komma.core.IEntity;
import net.enilink.composition.annotations.Iri;
import java.util.Set;

/** 
 * 
 * @generated 
 */
@Iri("http://enilink.net/vocab/komma#Map")
public interface Map extends Thing, IEntity {
	/** 
	 * 
	 * @generated 
	 */
	@Iri("http://enilink.net/vocab/komma#entry")
	Set<MapEntry> getKommaEntry();
	/** 
	 * 
	 * @generated 
	 */
	void setKommaEntry(Set<? extends MapEntry> kommaEntry);

}
