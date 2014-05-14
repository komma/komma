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
package net.enilink.komma.em.internal;

import java.util.Arrays;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;

/**
 * Manage the statements about a resource.
 * 
 */
public class ResourceManager {
	IDataManager dm;
	IReference[] contexts;

	public ResourceManager(IDataManager dm, IReference[] contexts) {
		this.dm = dm;
		this.contexts = contexts;
	}

	public IReference createResource(URI uri) {
		if (uri == null) {
			return dm.blankNode();
		}
		return uri;
	}

	public void removeResource(IReference resource) {
		boolean active = dm.getTransaction().isActive();
		try {
			if (!active) {
				dm.getTransaction().begin();
			}
			dm.remove(Arrays.asList( //
					new Statement(resource, null, null), //
					new Statement(null, null, resource)), contexts);
			if (!active) {
				dm.getTransaction().commit();
			}
		} catch (Exception e) {
			if (!active && dm.getTransaction().isActive()) {
				dm.getTransaction().rollback();
			}
			throw e;
		}
	}

	public void renameResource(IReference before, IReference after) {
		boolean active = dm.getTransaction().isActive();
		try {
			if (!active) {
				dm.getTransaction().begin();
			}
			IExtendedIterator<IStatement> stmts = dm.match(before, null, null,
					false, contexts);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference pred = stmt.getPredicate();
					Object obj = stmt.getObject();
					dm.remove(new Statement(before, pred, obj), contexts);
					dm.add(new Statement(after, pred, obj), contexts);
				}
			} finally {
				stmts.close();
			}
			stmts = dm.match(null, before, null, false, contexts);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference subj = stmt.getSubject();
					Object obj = stmt.getObject();
					dm.remove(new Statement(subj, before, obj), contexts);
					dm.add(new Statement(subj, after, obj), contexts);
				}
			} finally {
				stmts.close();
			}
			stmts = dm.match(null, null, before, false, contexts);
			try {
				while (stmts.hasNext()) {
					IStatement stmt = stmts.next();
					IReference subj = stmt.getSubject();
					IReference pred = stmt.getPredicate();
					dm.remove(new Statement(subj, pred, before), contexts);
					dm.add(new Statement(subj, pred, after), contexts);
				}
			} finally {
				stmts.close();
			}
			if (!active) {
				dm.getTransaction().commit();
			}
		} catch (Exception e) {
			if (!active && dm.getTransaction().isActive()) {
				dm.getTransaction().rollback();
			}
			throw new KommaException(e);
		}
	}
}
