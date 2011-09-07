/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 1997-2007.
 *
 * Licensed under the Aduna BSD-style license.
 */
package net.enilink.komma.model.sesame;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Stack;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.RDFHandlerException;

/**
 * An extension of RDFXMLWriter that outputs a more concise form of RDF/XML. The
 * resulting output is semantically equivalent to the output of an RDFXMLWriter
 * (it produces the same set of statements), but it is usually easier to read
 * for humans.
 * <p>
 * This is a quasi-streaming RDFWriter. Statements are cached as long as the
 * striped syntax is followed (i.e. the subject of the next statement is the
 * object of the previous statement) and written to the output when the stripe
 * is broken.
 * <p>
 * The abbreviations used are <a
 * href="http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-typed-nodes"
 * >typed node elements</a>, <a href="http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-empty-property-elements"
 * >empty property elements</a> and <a href=
 * "http://www.w3.org/TR/rdf-syntax-grammar/#section-Syntax-node-property-elements"
 * >striped syntax</a>. Note that these abbreviations require that statements
 * are written in the appropriate order.
 * <p>
 * Striped syntax means that when the object of a statement is the subject of
 * the next statement we can nest the descriptions in each other.
 * <p>
 * Example:
 * 
 * <pre>
 * &lt;rdf:Seq&gt;
 *    &lt;rdf:li&gt;
 *       &lt;foaf:Person&gt;
 *          &lt;foaf:knows&gt;
 *             &lt;foaf:Person&gt;
 *               &lt;foaf:mbox rdf:resource=&quot;...&quot;/&gt;
 *             &lt;/foaf:Person&gt;
 *          &lt;/foaf:knows&gt;
 *       &lt;/foaf:Person&gt;
 *    &lt;/rdf:li&gt;
 * &lt;/rdf:Seq&gt;
 * </pre>
 * 
 * Typed node elements means that we write out type information in the short
 * form of
 * 
 * <pre>
 * &lt;foaf:Person rdf:about=&quot;...&quot;&gt;
 *     ...
 *  &lt;/foaf:Person&gt;
 * </pre>
 * 
 * instead of
 * 
 * <pre>
 * &lt;rdf:Description rdf:about=&quot;...&quot;&gt;
 *    &lt;rdf:type rdf:resource=&quot;http://xmlns.com/foaf/0.1/Person&quot;/&gt;
 *     ...
 *  &lt;/rdf:Description&gt;
 * </pre>
 * 
 * Empty property elements are of the form
 * 
 * <pre>
 * &lt;foaf:Person&gt;
 *    &lt;foaf:homepage rdf:resource=&quot;http://www.cs.vu.nl/&tilde;marta&quot;/&gt;
 * &lt;/foaf:Person&gt;
 * </pre>
 * 
 * instead of
 * 
 * <pre>
 * &lt;foaf:Person&gt;
 *    &lt;foaf:homepage&gt;
 *       &lt;rdf:Description rdf:about=&quot;http://www.cs.vu.nl/&tilde;marta&quot;/&gt;
 *    &lt;foaf:homepage&gt;
 * &lt;/foaf:Person&gt;
 * </pre>
 * 
 * @author Peter Mika (pmika@cs.vu.nl)
 */
public class RDFXMLPrettyWriter extends RDFXMLWriter implements Closeable, Flushable {

	/*-----------*
	 * Variables *
	 *-----------*/

	/*
	 * We implement striped syntax by using two stacks, one for predicates and
	 * one for subjects/objects.
	 */

	private java.net.URI relativeURI;

	/**
	 * Stack for remembering the nodes (subjects/objects) of statements at each
	 * level.
	 */
	private Stack<Node> nodeStack = new Stack<Node>();

	/**
	 * Stack for remembering the predicate of statements at each level.
	 */
	private Stack<URI> predicateStack = new Stack<URI>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFXMLPrintWriter that will write to the supplied
	 * OutputStream.
	 * 
	 * @param out
	 *        The OutputStream to write the RDF/XML document to.
	 */
	public RDFXMLPrettyWriter(OutputStream out) {
		super(out);
	}

	/**
	 * Creates a new RDFXMLPrintWriter that will write to the supplied Writer.
	 * 
	 * @param out
	 *        The Writer to write the RDF/XML document to.
	 */
	public RDFXMLPrettyWriter(Writer out) {
		super(out);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void writeHeader()
		throws IOException
	{
		// This export format needs the RDF Schema namespace to be defined:
		setNamespace("rdfs", RDFS.NAMESPACE, false);

		super.writeHeader();
	}

	@Override
	public void setBaseURI(String baseURI) {
		super.setBaseURI(baseURI);
		try {
			if (baseURI == null) {
				relativeURI = null;
			}
			else if (baseURI.charAt(baseURI.length() - 1) == '/') {
				relativeURI = new java.net.URI(baseURI);
			}
			else {
				String ns = new URIImpl(baseURI).getNamespace();
				if (ns.charAt(ns.length() - 1) == '/') {
					relativeURI = new java.net.URI(ns);
				}
				else {
					relativeURI = null;
				}
			}
		}
		catch (URISyntaxException e) {
			relativeURI = null;
		}
	}

	public void flush()
		throws IOException
	{
		if (writingStarted) {
			if (!headerWritten) {
				writeHeader();
			}

			flushPendingStatements();

			writer.flush();
		}
	}

	public void close()
		throws IOException
	{
		try {
			if (writingStarted) {
				endRDF();
			}
		}
		catch (RDFHandlerException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException)e.getCause();
			}
			else {
				IOException ioe = new IOException(e.getMessage());
				ioe.initCause(e);
				throw ioe;
			}
		}
		finally {
			writer.close();
		}
	}

	@Override
	protected void flushPendingStatements()
		throws IOException
	{
		if (!nodeStack.isEmpty()) {
			popStacks(null);
		}
	}

	/**
	 * Write out the stacks until we find subject. If subject == null, write out
	 * the entire stack
	 * 
	 * @param newSubject
	 */
	private void popStacks(Resource newSubject)
		throws IOException
	{
		// Write start tags for the part of the stacks that are not yet
		// written
		for (int i = 0; i < nodeStack.size() - 1; i++) {
			Node node = nodeStack.get(i);

			if (!node.isWritten()) {
				if (i > 0) {
					writeIndents(i * 2 - 1);

					URI predicate = predicateStack.get(i - 1);

					writeStartTag(predicate.getNamespace(), predicate.getLocalName());
					writeNewLine();
				}

				writeIndents(i * 2);
				writeNodeStartTag(node);
				node.setIsWritten(true);
			}
		}

		// Write tags for the top subject
		Node topNode = nodeStack.pop();

		if (predicateStack.isEmpty()) {
			// write out an empty subject
			writeIndents(nodeStack.size() * 2);
			writeNodeEmptyTag(topNode);
			writeNewLine();
		}
		else {
			URI topPredicate = predicateStack.pop();

			if (!topNode.hasType()) {
				// we can use an abbreviated predicate
				writeIndents(nodeStack.size() * 2 - 1);
				writeAbbreviatedPredicate(topPredicate, topNode.getValue());
			}
			else {
				// we cannot use an abbreviated predicate because the type needs to
				// written out as well

				writeIndents(nodeStack.size() * 2 - 1);
				writeStartTag(topPredicate.getNamespace(), topPredicate.getLocalName());
				writeNewLine();

				// write out an empty subject
				writeIndents(nodeStack.size() * 2);
				writeNodeEmptyTag(topNode);
				writeNewLine();

				writeIndents(nodeStack.size() * 2 - 1);
				writeEndTag(topPredicate.getNamespace(), topPredicate.getLocalName());
				writeNewLine();
			}
		}

		// Write out the end tags until we find the subject
		while (!nodeStack.isEmpty()) {
			Node nextElement = nodeStack.peek();

			if (nextElement.getValue().equals(newSubject)) {
				break;
			}
			else {
				nodeStack.pop();

				// We have already written out the subject/object,
				// but we still need to close the tag
				writeIndents(predicateStack.size() + nodeStack.size());

				writeNodeEndTag(nextElement);

				if (predicateStack.size() > 0) {
					URI nextPredicate = predicateStack.pop();

					writeIndents(predicateStack.size() + nodeStack.size());

					writeEndTag(nextPredicate.getNamespace(), nextPredicate.getLocalName());

					writeNewLine();
				}
			}
		}
	}

	@Override
	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
		if (!writingStarted) {
			throw new RuntimeException("Document writing has not yet been started");
		}

		Resource subj = st.getSubject();
		URI pred = st.getPredicate();
		Value obj = st.getObject();

		try {
			if (!headerWritten) {
				writeHeader();
			}

			if (!nodeStack.isEmpty() && !subj.equals(nodeStack.peek().getValue())) {
				// Different subject than we had before, empty the stack
				// until we find it
				popStacks(subj);
			}

			// Stack is either empty or contains the same subject at top

			if (nodeStack.isEmpty()) {
				// Push subject
				nodeStack.push(new Node(subj));
			}

			// Stack now contains at least one element
			Node topSubject = nodeStack.peek();

			// Check if current statement is a type statement and use a typed node
			// element is possible
			// FIXME: verify that an XML namespace-qualified name can be created
			// for the type URI
			if (pred.equals(RDF.TYPE) && obj instanceof URI && !topSubject.hasType() && !topSubject.isWritten())
			{
				// Use typed node element
				topSubject.setType((URI)obj);
			}
			else {
				// Push predicate and object
				predicateStack.push(pred);
				nodeStack.push(new Node(obj));
			}
		}
		catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	/**
	 * Write out the opening tag of the subject or object of a statement up to
	 * (but not including) the end of the tag. Used both in writeStartSubject and
	 * writeEmptySubject.
	 */
	private void writeNodeStartOfStartTag(Node node)
		throws IOException
	{
		Value value = node.getValue();

		if (node.hasType()) {
			// We can use abbreviated syntax
			writeStartOfStartTag(node.getType().getNamespace(), node.getType().getLocalName());
		}
		else {
			// We cannot use abbreviated syntax
			writeStartOfStartTag(RDF.NAMESPACE, "Description");
		}

		if (value instanceof URI) {
			URI uri = (URI)value;
			writeAttribute(RDF.NAMESPACE, "about", relativize(uri.stringValue()));
		}
		else {
			BNode bNode = (BNode)value;
			writeAttribute(RDF.NAMESPACE, "nodeID", bNode.getID());
		}
	}

	private String relativize(String stringValue) {
		if (baseURI == null) {
			return stringValue;
		}
		if (stringValue.startsWith(baseURI)) {
			if (stringValue.length() == baseURI.length()) {
				return "";
			}
			if (stringValue.length() > baseURI.length() && '#' == stringValue.charAt(baseURI.length())) {
				return stringValue.substring(baseURI.length());
			}
		}
		if (relativeURI == null) {
			return stringValue;
		}
		try {
			if (stringValue.startsWith(relativeURI.toString())) {
				return relativeURI.relativize(new java.net.URI(stringValue)).toString();
			}
		}
		catch (URISyntaxException e) {
			// can't create a relative URI
		}
		return stringValue;
	}

	/**
	 * Write out the opening tag of the subject or object of a statement.
	 */
	private void writeNodeStartTag(Node node)
		throws IOException
	{
		writeNodeStartOfStartTag(node);
		writeEndOfStartTag();
		writeNewLine();
	}

	/**
	 * Write out the closing tag for the subject or object of a statement.
	 */
	private void writeNodeEndTag(Node node)
		throws IOException
	{
		if (node.getType() != null) {
			writeEndTag(node.getType().getNamespace(), node.getType().getLocalName());
		}
		else {
			writeEndTag(RDF.NAMESPACE, "Description");
		}
		writeNewLine();
	}

	/**
	 * Write out an empty tag for the subject or object of a statement.
	 */
	private void writeNodeEmptyTag(Node node)
		throws IOException
	{
		writeNodeStartOfStartTag(node);
		writeEndOfEmptyTag();
	}

	/**
	 * Write out an empty property element.
	 */
	private void writeAbbreviatedPredicate(URI pred, Value obj)
		throws IOException
	{
		writeStartOfStartTag(pred.getNamespace(), pred.getLocalName());

		if (obj instanceof Resource) {
			Resource objRes = (Resource)obj;

			if (objRes instanceof URI) {
				URI uri = (URI)objRes;
				writeAttribute(RDF.NAMESPACE, "resource", relativize(uri.stringValue()));
			}
			else {
				BNode bNode = (BNode)objRes;
				writeAttribute(RDF.NAMESPACE, "nodeID", bNode.getID());
			}

			writeEndOfEmptyTag();
		}
		else if (obj instanceof Literal) {
			Literal objLit = (Literal)obj;

			// language attribute
			if (objLit.getLanguage() != null) {
				writeAttribute("xml:lang", objLit.getLanguage());
			}

			// datatype attribute
			boolean isXmlLiteral = false;
			URI datatype = objLit.getDatatype();
			if (datatype != null) {
				// Check if datatype is rdf:XMLLiteral
				isXmlLiteral = datatype.equals(RDF.XMLLITERAL);

				if (isXmlLiteral) {
					writeAttribute(RDF.NAMESPACE, "parseType", "Literal");
				}
				else {
					writeAttribute(RDF.NAMESPACE, "datatype", datatype.toString());
				}
			}

			writeEndOfStartTag();

			// label
			if (isXmlLiteral) {
				// Write XML literal as plain XML
				writer.write(objLit.getLabel());
			}
			else {
				writeCharacterData(objLit.getLabel());
			}

			writeEndTag(pred.getNamespace(), pred.getLocalName());
		}

		writeNewLine();
	}

	protected void writeStartTag(String namespace, String localName)
		throws IOException
	{
		writeStartOfStartTag(namespace, localName);
		writeEndOfStartTag();
	}

	/**
	 * Writes <tt>n</tt> indents.
	 */
	protected void writeIndents(int n)
		throws IOException
	{
		for (int i = 0; i < n; i++) {
			writeIndent();
		}
	}

	/*------------------*
	 * Inner class Node *
	 *------------------*/

	private static class Node {

		private Value value;

		// type == null means that we use <rdf:Description>
		private URI type = null;

		private boolean isWritten = false;

		/**
		 * Creates a new Node for the supplied Value.
		 */
		public Node(Value value) {
			this.value = value;
		}

		public Value getValue() {
			return value;
		}

		public void setType(URI type) {
			this.type = type;
		}

		public URI getType() {
			return type;
		}

		public boolean hasType() {
			return type != null;
		}

		public void setIsWritten(boolean isWritten) {
			this.isWritten = isWritten;
		}

		public boolean isWritten() {
			return isWritten;
		}
	}
}
