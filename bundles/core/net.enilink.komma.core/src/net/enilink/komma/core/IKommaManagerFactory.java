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

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.inject.AbstractModule;

/**
 * Factory interface to create IKommaManagers.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 * 
 */
public interface IKommaManagerFactory {
	/**
	 * If this factory should be able to create an ElmoManager.
	 * 
	 * @return <code>true</code> if this factory is ready.
	 */
	boolean isOpen();

	/**
	 * Closes the factory preventing any more ElmoManagers from being created.
	 * 
	 * @throws Exception
	 * 
	 */
	void close();

	/**
	 * Creates a new KommaManager with the default Locale.
	 * 
	 * @return A new KommaManager.
	 */
	IKommaManager createKommaManager();

	/**
	 * Creates a new KommaManager with the given Locale.
	 * 
	 * @param locale
	 *            Maybe null to indicate no localization.
	 * @return A new KommaManager.
	 */
	IKommaManager createKommaManager(Locale locale);

	/**
	 * Creates modules for composition by guice .
	 * 
	 * @param locale
	 *            Maybe null to indicate no localization.
	 * @return Composition modules
	 */
	Collection<AbstractModule> createGuiceModules(Locale locale);

	/**
	 * Get the properties and associated values that are in effect for the
	 * entity manager factory. Changing the contents of the map does not change
	 * the configuration in effect.
	 * 
	 * @return properties
	 */
	Map<String, Object> getProperties();

	/**
	 * Get the names of the properties that are supported for use with the
	 * entity manager factory. These correspond to properties that may be passed
	 * to the methods of the EntityManagerFactory interface that take a
	 * properties argument. These include all standard properties as well as
	 * vendor-specific properties supported by the provider. These properties
	 * may or may not currently be in effect.
	 * 
	 * @return properties and hints
	 */
	Set<String> getSupportedProperties();
}