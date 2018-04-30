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
package net.enilink.komma.core;

import java.util.Locale;

/**
 * Factory interface to create {@link IEntityManager}s.
 * 
 */
public interface IEntityManagerFactory extends AutoCloseable {
	/**
	 * If this factory should be able to create an {@link IEntityManager}.
	 * 
	 * @return <code>true</code> if this factory is ready.
	 */
	boolean isOpen();

	/**
	 * Closes the factory preventing any more {@link IEntityManager}s from being
	 * created.
	 * 
	 * @throws Exception
	 * 
	 */
	void close();

	/**
	 * Creates a new {@link IEntityManager}. Its the user's responsibility to
	 * close this manager when the work is finished;
	 * 
	 * @return A new {@link IEntityManager}.
	 */
	IEntityManager create();

	/**
	 * Creates a new {@link IEntityManager} to used within the manager provided
	 * as <code>scope</code>. Its the user's responsibility to close this
	 * manager when the work is finished;
	 * 
	 * @param scope
	 *            The entity manager that should be used as shared entity
	 *            manager between different beans.
	 * @return A new {@link IEntityManager}.
	 */
	IEntityManager create(IEntityManager scope);

	/**
	 * Creates a new {@link IEntityManagerFactory} with the default Locale.
	 * 
	 * @param modules
	 *            Set of modules to configure the {@link IEntityManagerFactory}.
	 * 
	 * @return A new {@link IEntityManagerFactory}.
	 */
	IEntityManagerFactory createChildFactory(KommaModule... modules);

	/**
	 * Creates a new {@link IEntityManagerFactory} with the given Locale.
	 * 
	 * @param modules
	 *            Set of modules to configure the {@link IEntityManagerFactory}.
	 * 
	 * @return A new {@link IEntityManagerFactory}.
	 */
	IEntityManagerFactory createChildFactory(IProvider<Locale> locale,
			KommaModule... modules);

	/**
	 * Returns an {@link IEntityManager} instance that is open within the
	 * currently active unit of work.
	 * 
	 * @return An {@link IEntityManager}.
	 */
	IEntityManager get();

	/**
	 * Return the SPARQL dialect that is specific for the underlying store.
	 * 
	 * @return The specific dialect.
	 */
	IDialect getDialect();

	/**
	 * Returns the parent factory or <code>null</code> if this is the root
	 * factory.
	 * 
	 * @return The parent factory or <code>null</code>.
	 */
	IEntityManagerFactory getParent();

	/**
	 * Returns the {@link KommaModule} that is used by {@link IEntityManager}s
	 * created by this factory.
	 * 
	 * @return {@link KommaModule} used by this factory.
	 */
	KommaModule getModule();

	/**
	 * Get the unit of work that is used to manage thread-local entity managers.
	 * 
	 * @return unit of work
	 */
	IUnitOfWork getUnitOfWork();
}