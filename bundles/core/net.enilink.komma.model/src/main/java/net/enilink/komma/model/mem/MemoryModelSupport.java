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
package net.enilink.komma.model.mem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import net.enilink.composition.annotations.Iri;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.event.base.NotifyingRepositoryConnectionWrapper;
import org.openrdf.result.Result;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.store.StoreException;

import net.enilink.komma.KommaCore;
import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.model.base.AbstractModelSupport;
import net.enilink.komma.core.KommaException;

@Iri(MODELS.NAMESPACE + "MemoryModel")
public abstract class MemoryModelSupport extends AbstractModelSupport {
	@Override
	public void load(InputStream in, Map<?, ?> options) throws IOException {
		RepositoryConnection conn = null;

		org.openrdf.model.URI modelUri = URIUtil.toSesameUri(getURI());
		try {
			setModelLoading(true);

			conn = getRepository().getConnection();

			// ensure that the underlying add(...) method is used
			RepositoryConnection delegate = conn;
			while (delegate instanceof NotifyingRepositoryConnectionWrapper) {
				delegate = ((NotifyingRepositoryConnectionWrapper) conn)
						.getDelegate();
			}

			if (in != null && in.available() > 0) {
				conn.begin();
				delegate.add(in, modelUri.stringValue(), RDFFormat.RDFXML,
						modelUri);
				conn.commit();
			}
		} catch (IOException e) {
			throw new KommaException("Cannot access RDF data", e);
		} catch (StoreException e) {
			throw new KommaException("Loading RDF failed", e);
		} catch (RDFParseException e) {
			throw new KommaException("Invalid RDF data", e);
		} finally {
			setModelLoading(false);
			setModified(false);

			if (conn != null) {
				try {
					if (!conn.isAutoCommit()) {
						conn.rollback();
					}
				} catch (StoreException e) {
					KommaCore.log(e);
				} finally {
					try {
						conn.close();
					} catch (StoreException e) {
						KommaCore.log(e);
					}
				}
			}
		}

		setModelLoaded(true);
	}

	@Override
	public void save(OutputStream os, Map<?, ?> options) throws IOException {
		RDFXMLWriter rdfWriter = new RDFXMLWriter(new OutputStreamWriter(os,
				"UTF-8"));
		try {
			rdfWriter.startRDF();
			RepositoryConnection conn = null;

			org.openrdf.model.URI modelUri = URIUtil.toSesameUri(getURI());
			try {
				conn = getRepository().getConnection();

				for (Namespace namespace : conn.getNamespaces().asList()) {
					rdfWriter.handleNamespace(namespace.getPrefix(),
							namespace.getName());
				}

				Result<Statement> stmts = conn.match(null, null, null, false,
						modelUri);
				while (stmts.hasNext()) {
					rdfWriter.handleStatement(stmts.next());
				}
			} catch (StoreException e) {
				KommaCore.log(e);
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (StoreException e) {
						KommaCore.log(e);
					}
				}
			}

			rdfWriter.endRDF();
		} catch (RDFHandlerException e) {
			throw new KommaException("Saving RDF failed", e);
		}
	}
}