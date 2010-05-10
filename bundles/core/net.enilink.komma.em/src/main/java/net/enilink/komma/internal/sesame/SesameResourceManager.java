/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
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

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.Result;

import com.google.inject.Inject;

import net.enilink.komma.core.KommaException;

/**
 * Determine the rdf:types of a Sesame Resource.
 * 
 * @author James Leigh
 * 
 */
public class SesameResourceManager {
	private RepositoryConnection conn;

	private ValueFactory vf;

	@Inject
	public void setConnection(RepositoryConnection conn) {
		this.conn = conn;
		this.vf = conn.getValueFactory();
	}

	public Resource createResource(net.enilink.komma.core.URI uri) {
		if (uri == null) {
			return vf.createBNode();
		}
		return vf.createURI(uri.toString());
	}

	public void removeResource(Resource resource) {
		try {
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit) {
				conn.begin();
			}
			conn.removeMatch(resource, (URI) null, null);
			conn.removeMatch((URI) null, (URI) null, resource);
			if (autoCommit) {
				conn.commit();
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}

	public void renameResource(Resource before, Resource after) {
		try {
			Result<Statement> stmts;
			boolean autoCommit = conn.isAutoCommit();
			if (autoCommit) {
				conn.begin();
			}
			stmts = conn.match(before, null, null, false);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					URI pred = stmt.getPredicate();
					Value obj = stmt.getObject();
					conn.removeMatch(before, pred, obj);
					conn.add(after, pred, obj);
				}
			} finally {
				stmts.close();
			}
			stmts = conn.match(null, null, before, false);
			try {
				while (stmts.hasNext()) {
					Statement stmt = stmts.next();
					Resource subj = stmt.getSubject();
					URI pred = stmt.getPredicate();
					conn.removeMatch(subj, pred, before);
					conn.add(subj, pred, after);
				}
			} finally {
				stmts.close();
			}
			if (autoCommit) {
				conn.commit();
			}
		} catch (Exception e) {
			throw new KommaException(e);
		}
	}
}
