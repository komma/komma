/*
 * Copyright (c) 2008, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package net.enilink.komma.em.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

/**
 * Reads and manages the rdf:type statements of objects.
 * 
 */
public class TypeManager {
	IDataManager dm;

	public TypeManager(IDataManager dm) {
		this.dm = dm;
	}

	public void addType(IReference resource, URI type) {
		if (!RDFS.TYPE_RESOURCE.equals(type)) {
			dm.add(new Statement(resource, RDF.PROPERTY_TYPE, type));
		}
	}

	public Collection<URI> getTypes(IReference res) {
		IExtendedIterator<IStatement> match = dm.match(res, RDF.PROPERTY_TYPE,
				null);
		try {
			if (!match.hasNext())
				return Collections.emptySet();
			Object obj = match.next().getObject();
			if (obj instanceof IReference
					&& ((IReference) obj).getURI() != null && !match.hasNext())
				return Collections.singleton(((IReference) obj).getURI());
			List<URI> types = new ArrayList<URI>();
			if (obj instanceof IReference
					&& ((IReference) obj).getURI() != null) {
				types.add(((IReference) obj).getURI());
			}
			while (match.hasNext()) {
				obj = match.next().getObject();
				if (obj instanceof IReference
						&& ((IReference) obj).getURI() != null) {
					types.add(((IReference) obj).getURI());
				}
			}
			return types;
		} finally {
			match.close();
		}
	}

	public void removeType(IReference resource, URI type) {
		dm.remove(new Statement(resource, RDF.PROPERTY_TYPE, type));
	}
}
