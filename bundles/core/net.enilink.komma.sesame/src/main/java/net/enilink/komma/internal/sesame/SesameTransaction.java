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

import org.openrdf.repository.RepositoryException;

import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.core.ITransaction;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.RollbackException;

/**
 * {@link ITransaction} interface for {@link SesameRepositoryDataManager}.
 */
public class SesameTransaction implements ITransaction {
	private SesameRepositoryDataManager dm;
	private IDataChangeSupport changeSupport;

	private boolean rollbackOnly = false;

	public SesameTransaction(SesameRepositoryDataManager dm,
			IDataChangeSupport changeSupport) {
		this.dm = dm;
		this.changeSupport = changeSupport;
	}

	public void begin() {
		try {
			if (isActive())
				throw new IllegalStateException("Transaction already started");
			dm.getConnection().begin();
		} catch (RepositoryException e) {
			throw new KommaException(e);
		}
	}

	public void commit() {
		try {
			if (!isActive())
				throw new IllegalStateException(
						"Transaction has not been started");
			dm.getConnection().commit();
			rollbackOnly = false;

			if (changeSupport.isEnabled(dm)) {
				changeSupport.commit(dm);
			}
		} catch (RepositoryException e) {
			throw new RollbackException(e);
		}
	}

	public void rollback() {
		try {
			if (!isActive())
				throw new IllegalStateException(
						"Transaction has not been started");
			dm.getConnection().rollback();
			rollbackOnly = false;

			if (changeSupport.isEnabled(dm)) {
				changeSupport.rollback(dm);
			}
		} catch (RepositoryException e) {
			throw new KommaException(e);
		}
	}

	public void setRollbackOnly() {
		if (!isActive())
			throw new IllegalStateException("Transaction has not been started");
		rollbackOnly = true;
	}

	public boolean getRollbackOnly() {
		if (!isActive())
			throw new IllegalStateException("Transaction has not been started");
		return rollbackOnly;
	}

	public boolean isActive() {
		try {
			return dm.getConnection().isActive();
		} catch (RepositoryException e) {
			throw new KommaException(e);
		}
	}
}