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
package net.enilink.komma.internal.sesame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.Result;
import org.openrdf.store.StoreException;

import com.google.inject.Inject;

/**
 * Reads and manages the rdf:type statements of objects.
 * 
 * @author James Leigh
 * 
 */
public class SesameTypeManager {
	private RepositoryConnection connection;

	public void addType(Resource resource, URI type) throws StoreException {
		if (!RDFS.RESOURCE.equals(type)) {
			connection.add(resource, RDF.TYPE, type);
		}
	}

	public Collection<URI> getTypes(Resource res) throws StoreException {
		Result<Statement> match = connection.match(res, RDF.TYPE, null, true);
		try {
			if (!match.hasNext())
				return Collections.emptySet();
			Value obj = match.next().getObject();
			if (obj instanceof URI && !match.hasNext())
				return Collections.singleton((URI) obj);
			List<URI> types = new ArrayList<URI>();
			if (obj instanceof URI) {
				types.add((URI) obj);
			}
			while (match.hasNext()) {
				obj = match.next().getObject();
				if (obj instanceof URI) {
					types.add((URI) obj);
				}
			}
			return types;
		} finally {
			match.close();
		}
	}

	public void removeType(Resource resource, URI type) throws StoreException {
		connection.removeMatch(resource, RDF.TYPE, type);
	}

	@Inject
	public void setConnection(RepositoryConnection connection) {
		this.connection = connection;
	}
}
