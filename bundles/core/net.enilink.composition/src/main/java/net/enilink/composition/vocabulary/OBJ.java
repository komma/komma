/*
 * Copyright (c) 2009, 2010, James Leigh All rights reserved.
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
package net.enilink.composition.vocabulary;

/**
 * Static vocabulary for the object ontology.
 * 
 * @author James Leigh
 * 
 */
public class OBJ {
	public static final String NAMESPACE = "http://www.openrdf.org/rdf/2009/object#";
	
	public static final String COMPONENT_TYPE = NAMESPACE + "componentType";
	public static final String DATATYPE_TRIGGER = NAMESPACE + "DatatypeTrigger";
	public static final String FUNCITONAL_LITERAL_RESPONSE = NAMESPACE
			+ "functionalLiteralResponse";
	public static final String FUNCTIONAL_OBJECT_RESPONSE = NAMESPACE
			+ "functionalObjectResponse";
	public static final String GROOVY = NAMESPACE + "groovy";
	public static final String IMPORTS = NAMESPACE + "imports";
	public static final String JAVA = NAMESPACE + "java";
	public static final String LITERAL_RESPONSE = NAMESPACE + "literalResponse";
	public static final String LOCALIZED = NAMESPACE + "localized";
	public static final String MESSAGE = NAMESPACE + "Message";
	public static final String METHOD = NAMESPACE + "Method";
	public static final String CLASS_NAME = NAMESPACE + "className";
	public static final String MATCHES = NAMESPACE + "matches";
	public static final String NAME = NAMESPACE + "name";
	public static final String OBJECT_RESPONSE = NAMESPACE + "objectResponse";
	public static final String OBJECT_TRIGGER = NAMESPACE + "ObjectTrigger";
	public static final String PRECEDES = NAMESPACE + "precedes";
	public static final String PROCEED = NAMESPACE + "proceed";
	public static final String READ_ONLY = NAMESPACE + "readOnly";
	public static final String SPARQL = NAMESPACE + "sparql";
	public static final String TARGET = NAMESPACE + "target";

	private OBJ() {
		// prevent instantiation
	}

}
