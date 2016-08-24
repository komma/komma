/*
 * Copyright James Leigh (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriter;

/**
 * Prints RDF/XML data is an grouped by subject and type.
 * 
 * @author James Leigh
 * @see OrganizedRDFWriter
 * 
 */
public class OrganizedRDFXMLWriter extends OrganizedRDFWriter {
	public OrganizedRDFXMLWriter(File file, String enc)
			throws FileNotFoundException, UnsupportedEncodingException {
		this(new PrintWriter(file, enc));
	}

	public OrganizedRDFXMLWriter(File file) throws FileNotFoundException {
		this(new PrintWriter(file));
	}

	public OrganizedRDFXMLWriter(String filename, String enc)
			throws FileNotFoundException, UnsupportedEncodingException {
		this(new PrintWriter(filename, enc));
	}

	public OrganizedRDFXMLWriter(String filename) throws FileNotFoundException {
		this(new PrintWriter(filename));
	}

	public OrganizedRDFXMLWriter(OutputStream writer) {
		super(new RDFXMLPrettyWriter(writer));
	}

	public OrganizedRDFXMLWriter(Writer writer) {
		super(new RDFXMLPrettyWriter(writer));
	}
}
