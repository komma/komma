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

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriter;

import net.enilink.komma.core.IReference;
import net.enilink.komma.core.Namespace;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;

/**
 * Prints RDF ontology data is an grouped by subject and type.
 * 
 */
public class OntologyWriter extends OrganizedRDFWriter {
	private static RDFWriter createWriter(RDFFormat format, OutputStream out) {
		if (format.equals(RDFFormat.RDFXML)) {
			return new RDFXMLPrettyWriter(out);
		}
		return Rio.createWriter(format, out);
	}

	private static RDFWriter createWriter(RDFFormat format, Writer writer) {
		if (format.equals(RDFFormat.RDFXML)) {
			return new RDFXMLPrettyWriter(writer);
		}
		return Rio.createWriter(format, writer);
	}

	public OntologyWriter(File file, String enc) throws FileNotFoundException,
			UnsupportedEncodingException {
		this(new PrintWriter(file, enc));
	}

	public OntologyWriter(File file) throws FileNotFoundException {
		this(new PrintWriter(file));
	}

	public OntologyWriter(String filename, String enc)
			throws FileNotFoundException, UnsupportedEncodingException {
		this(new PrintWriter(filename, enc));
	}

	public OntologyWriter(String filename) throws FileNotFoundException {
		this(new PrintWriter(filename));
	}

	public OntologyWriter(OutputStream writer) {
		super(new RDFXMLPrettyWriter(writer));
	}

	public OntologyWriter(Writer writer) {
		super(new RDFXMLPrettyWriter(writer));
	}

	public OntologyWriter(RDFFormat format, OutputStream out) {
		super(createWriter(format, out));
	}

	public OntologyWriter(RDFFormat format, Writer writer) {
		super(createWriter(format, writer));
	}

	@Override
	public Void visitBegin() {
		super.visitBegin();
		visitNamespace(new Namespace("rdf", RDF.NAMESPACE_URI));
		visitNamespace(new Namespace("rdfs", RDFS.NAMESPACE_URI));
		visitNamespace(new Namespace("owl", OWL.NAMESPACE_URI));
		return null;
	}

	public void printOntology(IReference ontology) {
		print(ontology);
		print(RDFS.PROPERTY_ISDEFINEDBY, ontology);
	}

	@Override
	public void print() {
		print(RDF.PROPERTY_TYPE, OWL.TYPE_ONTOLOGY);
		super.print();
		printReferenced();
	}
}
