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
package net.enilink.composition.properties;

import java.util.Collection;
import java.util.Set;

import net.enilink.composition.properties.traits.Refreshable;

/**
 * Internal interface for mapping roles. Allows access to property values as a
 * Set or as a single value.
 * 
 * @author James Leigh
 * 
 * @param <E>
 *            property type
 */
public interface PropertySet<E> extends Refreshable {
	/**
	 * Get all values for property.
	 * 
	 * @return set of all values
	 */
	Set<E> getAll();

	/**
	 * Replaces all values with the values given.
	 * 
	 * @param all
	 */
	void setAll(Set<E> all);

	/**
	 * Assumes there is zero or one value and return null or the value.
	 * 
	 * @return null or the single value
	 */
	Object getSingle();

	/**
	 * Replace all values with this value
	 * 
	 * @param single
	 */
	void setSingle(E single);

	/**
	 * Append all values with the values given.
	 * 
	 * @param all
	 */
	boolean addAll(Collection<? extends E> all);

	/**
	 * Append values with this value
	 * 
	 * @param single
	 */
	boolean add(E single);

	/**
	 * Sets the internal cache of the property set to the given values to
	 * support pre-initialization. This is an optional operation.
	 * 
	 * @param values
	 */
	void init(Collection<? extends E> values);
}
