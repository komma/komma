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

import org.openrdf.model.Resource;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.openrdf.store.StoreException;

/**
 * Prints RDF ontology data is an grouped by subject and type.
 * 
 * @author James Leigh
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
	public void startRDF() throws RDFHandlerException {
		super.startRDF();
		handleNamespace("rdf", RDF.NAMESPACE);
		handleNamespace("rdfs", RDFS.NAMESPACE);
		handleNamespace("owl", OWL.NAMESPACE);
	}

	public void printOntology(Resource ontology) throws StoreException,
			RDFHandlerException {
		print(ontology);
		print(RDFS.ISDEFINEDBY, ontology);
	}

	@Override
	public void print() throws RDFHandlerException {
		print(RDF.TYPE, OWL.ONTOLOGY);
		super.print();
		printReferenced();
	}

}
