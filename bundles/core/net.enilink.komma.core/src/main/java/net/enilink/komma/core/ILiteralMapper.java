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

/**
 * Interface used to convert between Java literal objects and {@link ILiteral}s.
 *
 * @param <T> Associated Java class type that can be serialized and deserialized by this converter.
 */
public interface ILiteralMapper<T> {
	/**
	 * Returns the RDF datatype URI of the literal.
	 *
	 * @return the literal's datatype
	 */
	URI getDatatype();

	/**
	 * Sets the RDF datatype URI of the literal.
	 *
	 * @param datatype the literal's datatype
	 */
	void setDatatype(URI datatype);

	/**
	 * Converts the label of a literal to a Java object
	 *
	 * @param literal the literal label
	 * @return a Java object representing the literal
	 */
	T deserialize(ILiteral literal);

	/**
	 * Converts a Java object into an RDF literal.
	 *
	 * @param object The Java object to convert
	 * @return an RDF literal representing the Java object
	 */
	ILiteral serialize(T object);
}
