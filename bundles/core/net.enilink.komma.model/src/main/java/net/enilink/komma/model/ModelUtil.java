package net.enilink.komma.model;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.RDFWriterRegistry;
import org.openrdf.rio.Rio;

import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.Class;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.model.sesame.RDFXMLPrettyWriter;
import net.enilink.komma.core.BlankNode;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.INamespace;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.Namespace;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.core.visitor.IDataAndNamespacesVisitor;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.sesame.SesameValueConverter;

public class ModelUtil {
	/**
	 * Collator which can be used to compares resource labels.
	 */
	public static final Collator LABEL_COLLATOR;
	static {
		// add support for umlauts
		// in a multi-user environment (webapp) the locale
		// may need to be determined dynamically
		LABEL_COLLATOR = Collator.getInstance(Locale.GERMAN);
		LABEL_COLLATOR.setStrength(Collator.SECONDARY);
	}

	/**
	 * Compute a label for the given element.
	 */
	public static String getLabel(Object element) {
		return getLabel(element, false);
	}

	/**
	 * Compute a label for the given element.
	 */
	public static String getLabel(Object element, boolean useLabelForVocab) {
		StringBuilder text = new StringBuilder();
		if (element instanceof IStatement) {
			element = ((IStatement) element).getObject();
		}
		if (element instanceof Resource) {
			Resource resource = (Resource) element;

			String label = null;
			if (useLabelForVocab
					|| !(resource instanceof Class
							|| resource instanceof Property || resource instanceof IObject
							&& ((IObject) resource).isOntLanguageTerm())) {
				label = resource.getRdfsLabel();
			}
			if (label != null) {
				text.append(label);
			} else {
				URI uri = resource.getURI();
				if (uri != null) {
					String prefix = resource.getEntityManager().getPrefix(
							uri.namespace());
					String localPart = uri.localPart();
					boolean hasLocalPart = localPart != null
							&& localPart.length() > 0;
					if (prefix != null && prefix.length() > 0 && hasLocalPart) {
						text.append(prefix).append(":");
					}
					text.append(hasLocalPart && prefix != null ? localPart
							: uri.toString());
				} else {
					text.append(resource.toString());
				}
			}
		} else if (element instanceof ILiteral) {
			text.append(((ILiteral) element).getLabel());
		} else {
			text.append(String.valueOf(element));
		}
		return text.toString();
	}

	/**
	 * Returns a subset of the objects such that no object in the result is an
	 * transitive container of any other object in the result.
	 * 
	 * @param objects
	 *            the objects to be filtered.
	 * @return a subset of the objects such that no object in the result is an
	 *         ancestor of any other object in the result.
	 */
	public static List<IObject> filterDescendants(
			Collection<? extends IObject> objects) {
		List<IObject> result = new ArrayList<IObject>(objects.size());

		LOOP: for (IObject object : objects) {
			for (int i = 0, size = result.size(); i < size;) {
				IObject rootObject = result.get(i);
				if (rootObject.equals(object)
						|| rootObject.getAllContents().contains(object)) {
					continue LOOP;
				}

				if (object.getAllContents().contains(rootObject)) {
					result.remove(i);
					--size;
				} else {
					++i;
				}
			}
			result.add(object);
		}

		return result;
	}

	/**
	 * Computes a {@link Diagnostic} from the errors and warnings stored in the
	 * specified resource.
	 * 
	 * @param model
	 * @param includeWarnings
	 * @return {@link Diagnostic}
	 */
	public static Diagnostic computeDiagnostic(IModel model,
			boolean includeWarnings) {
		if (model.getErrors().isEmpty()
				&& (!includeWarnings || model.getWarnings().isEmpty())) {
			return Diagnostic.OK_INSTANCE;
		} else {
			BasicDiagnostic basicDiagnostic = new BasicDiagnostic();
			for (IModel.IDiagnostic modelDiagnostic : model.getErrors()) {
				Diagnostic diagnostic = null;
				if (modelDiagnostic instanceof Throwable) {
					diagnostic = BasicDiagnostic
							.toDiagnostic((Throwable) modelDiagnostic);
				} else {
					diagnostic = new BasicDiagnostic(Diagnostic.ERROR,
							"org.eclipse.emf.ecore.resource", 0,
							modelDiagnostic.getMessage(),
							new Object[] { modelDiagnostic });
				}
				basicDiagnostic.add(diagnostic);
			}

			if (includeWarnings) {
				for (IModel.IDiagnostic modelDiagnostic : model.getWarnings()) {
					Diagnostic diagnostic = null;
					if (modelDiagnostic instanceof Throwable) {
						diagnostic = BasicDiagnostic
								.toDiagnostic((Throwable) modelDiagnostic);
					} else {
						diagnostic = new BasicDiagnostic(Diagnostic.WARNING,
								"org.eclipse.emf.ecore.resource", 0,
								modelDiagnostic.getMessage(),
								new Object[] { modelDiagnostic });
					}
					basicDiagnostic.add(diagnostic);
				}
			}

			return basicDiagnostic;
		}
	}

	public static IContentDescription contentDescription(
			IURIConverter uriConverter, URI modelUri) throws IOException {
		String contentTypeId = (String) uriConverter.contentDescription(
				modelUri, null).get(IContentHandler.CONTENT_TYPE_PROPERTY);
		if (contentTypeId != null) {
			IContentType contentType = Platform.getContentTypeManager()
					.getContentType(contentTypeId);
			if (contentType != null) {
				return contentType.getDefaultDescription();
			}
		}
		return null;
	}

	private static RDFFormat determineFormat(String mimeType, InputStream in) {
		RDFFormat format = RDFFormat.RDFXML;
		if (mimeType != null) {
			format = RDFFormat.forMIMEType(mimeType, format);
		} else if (in.markSupported()) {
			// try to distinguish RDF/XML and Turtle
			in.mark(2048);
			try {
				Reader r = new InputStreamReader(in, "UTF-8");
				while (r.ready()) {
					int ch = r.read();
					if (!Character.isWhitespace(ch) &&
					// not the BOM character
							ch != 0xFEFF) {
						if (ch == '<') {
							if (r.ready() && (ch = r.read()) == '?') {
								// <?xml ...>
								// this is RDF/XML
								break;
							}
							StringBuilder tag = new StringBuilder();
							tag.append((char) ch);
							int charsAfterColon = -1;
							while (r.ready() && (ch = r.read()) != '>') {
								tag.append((char) ch);
								if (charsAfterColon >= 0) {
									charsAfterColon++;
								}
								// read up to 4 chars after namespace separator
								// <ns:rdf [stop here]
								if (charsAfterColon > 3
										|| Character.isWhitespace(ch)) {
									break;
								} else if (ch == ':' && charsAfterColon < 0) {
									charsAfterColon = 0;
								}
							}
							// test for content like <ns:RDF ...> or <RDF ...>
							boolean isRdfXml = Pattern
									.compile("(^|[^:]+:)rdf\\s+$",
											Pattern.CASE_INSENSITIVE)
									.matcher(tag.toString()).matches();
							if (isRdfXml) {
								break;
							}
						}
						format = RDFFormat.TURTLE;
						break;
					}
				}
			} catch (UnsupportedEncodingException e) {
				// should never happend
			} catch (IOException e) {
				throw new KommaException("Reading RDF data failed.", e);
			}
			try {
				in.reset();
			} catch (IOException e) {
				throw new KommaException("Detection of RDF format failed.");
			}
		}
		return format;
	}

	public static String findOntology(InputStream in, String baseURI,
			String mimeType) throws Exception {
		if (mimeType == null && !in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		final org.openrdf.model.URI[] ontology = { null };
		RDFParser parser = Rio.createParser(determineFormat(mimeType, in));
		parser.setRDFHandler(new RDFHandler() {
			@Override
			public void startRDF() throws RDFHandlerException {
			}

			@Override
			public void handleStatement(Statement stmt)
					throws RDFHandlerException {
				if (org.openrdf.model.vocabulary.RDF.TYPE.equals(stmt
						.getPredicate())
						&& org.openrdf.model.vocabulary.OWL.ONTOLOGY
								.equals(stmt.getObject())) {
					if (stmt.getSubject() instanceof org.openrdf.model.URI) {
						ontology[0] = (org.openrdf.model.URI) stmt.getSubject();
					}
				}

				if (ontology[0] != null) {
					throw new RDFHandlerException("found ontology URI");
				}
			}

			@Override
			public void handleNamespace(String prefx, String uri)
					throws RDFHandlerException {

			}

			@Override
			public void handleComment(String text) throws RDFHandlerException {

			}

			@Override
			public void endRDF() throws RDFHandlerException {

			}
		});

		try {
			parser.parse(in, baseURI);
		} catch (RDFHandlerException rhe) {
			// ignore, ontology was found
		} finally {
			in.close();
		}

		if (ontology[0] != null) {
			return ontology[0].stringValue();
		}
		return null;
	}

	public static String mimeType(IContentDescription contentDescription) {
		if (contentDescription != null) {
			return (String) contentDescription.getProperty(new QualifiedName(
					ModelPlugin.PLUGIN_ID, "mimeType"));
		}
		return null;
	}

	public static IContentDescription determineContentDescription(
			URI resourceUri, IURIConverter uriConverter, Map<?, ?> options)
			throws IOException {
		IContentTypeManager contentTypeManager = Platform
				.getContentTypeManager();
		if (contentTypeManager == null) {
			return null;
		}
		if (options == null) {
			options = Collections.emptyMap();
		}
		IContentDescription contentDescription = null;
		String contentTypeId = (String) uriConverter.contentDescription(
				resourceUri, options)
				.get(IContentHandler.CONTENT_TYPE_PROPERTY);
		IContentType contentType = null;
		if (contentTypeId != null) {
			contentType = contentTypeManager.getContentType(contentTypeId);
		}
		if (contentType != null) {
			contentDescription = contentType.getDefaultDescription();
		}
		if (contentDescription == null) {
			// use file name with extension as fall back
			URI normalizedUri = uriConverter.normalize(resourceUri);
			// simply use the filename to detect the correct RDF format
			String lastSegment = normalizedUri.fileExtension();
			if (lastSegment != null) {
				IContentType[] matchingTypes = contentTypeManager
						.findContentTypesFor(lastSegment);
				QualifiedName mimeType = new QualifiedName(ModelPlugin.PLUGIN_ID,
						"mimeType");
				for (IContentType matchingType : matchingTypes) {
					IContentDescription desc = matchingType
							.getDefaultDescription();
					if (desc.getProperty(mimeType) != null) {
						contentDescription = desc;
					}
				}
			}
		}
		return contentDescription;
	}

	public static <V extends IDataVisitor<?>> void readData(InputStream in,
			String baseURI, String mimeType, final V dataVisitor) {
		// generate unique BNode IDs by default
		readData(in, baseURI, mimeType, false, dataVisitor);
	}

	public static <V extends IDataVisitor<?>> void readData(InputStream in,
			String baseURI, String mimeType, boolean preserveBNodeIDs,
			final V dataVisitor) {
		if (mimeType == null && !in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		ValueFactory valueFactory = new ValueFactoryImpl() {
			@Override
			public synchronized BNode createBNode() {
				return createBNode(BlankNode.generateId("new-").substring(2));
			}
		};
		final SesameValueConverter valueConverter = new SesameValueConverter(
				valueFactory);
		final boolean handleNamespaces = dataVisitor instanceof IDataAndNamespacesVisitor<?>;
		RDFParser parser = Rio.createParser(determineFormat(mimeType, in),
				valueFactory);
		parser.setPreserveBNodeIDs(preserveBNodeIDs);
		parser.setRDFHandler(new RDFHandler() {
			@Override
			public void startRDF() throws RDFHandlerException {
				dataVisitor.visitBegin();
			}

			@Override
			public void handleStatement(Statement stmt)
					throws RDFHandlerException {
				dataVisitor
						.visitStatement(new net.enilink.komma.core.Statement(
								(IReference) valueConverter.fromSesame(stmt
										.getSubject()),
								(IReference) valueConverter.fromSesame(stmt
										.getPredicate()), valueConverter
										.fromSesame(stmt.getObject())));
			}

			@Override
			public void handleNamespace(String prefix, String uri)
					throws RDFHandlerException {
				if (handleNamespaces) {
					((IDataAndNamespacesVisitor<?>) dataVisitor)
							.visitNamespace(new Namespace(prefix, URIImpl
									.createURI(uri)));
				}
			}

			@Override
			public void handleComment(String text) throws RDFHandlerException {
			}

			@Override
			public void endRDF() throws RDFHandlerException {
				dataVisitor.visitEnd();
			}
		});

		try {
			parser.parse(in, baseURI);
		} catch (RDFParseException e) {
			throw new KommaException("Invalid RDF data:\n" + e.getMessage(), e);
		} catch (RDFHandlerException e) {
			throw new KommaException("Loading RDF failed", e);
		} catch (IOException e) {
			throw new KommaException("Cannot access RDF data", e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				throw new KommaException("Unable to close input stream", e);
			}
		}
	}

	private static RDFWriter createWriter(OutputStream os, String baseURI,
			String mimeType, String charset) throws IOException {
		RDFFormat format = RDFFormat.RDFXML;
		if (mimeType != null) {
			format = RDFFormat.forMIMEType(mimeType, format);
		}
		if (RDFFormat.RDFXML.equals(format)) {
			// use a special pretty writer in case of RDF/XML
			RDFXMLPrettyWriter rdfXmlWriter = new RDFXMLPrettyWriter(
					new OutputStreamWriter(os, charset != null ? charset
							: "UTF-8"));
			rdfXmlWriter.setBaseURI(baseURI);
			return rdfXmlWriter;
		} else {
			RDFWriterFactory factory = RDFWriterRegistry.getInstance().get(
					format);
			return factory.getWriter(os);
		}
	}

	public static IDataAndNamespacesVisitor<Void> writeData(OutputStream os,
			String baseURI, String mimeType, String charset) {
		try {
			final RDFWriter writer = createWriter(os, baseURI, mimeType,
					charset);
			return new IDataAndNamespacesVisitor<Void>() {
				final SesameValueConverter valueConverter = new SesameValueConverter(
						new ValueFactoryImpl());

				void throwException(Exception e) {
					throw new KommaException("Saving RDF failed", e);
				}

				@Override
				public Void visitBegin() {
					try {
						writer.startRDF();
					} catch (RDFHandlerException e) {
						throwException(e);
					}
					return null;
				}

				@Override
				public Void visitEnd() {
					try {
						writer.endRDF();
					} catch (RDFHandlerException e) {
						throwException(e);
					}
					return null;
				}

				@Override
				public Void visitStatement(IStatement stmt) {
					try {
						writer.handleStatement(valueConverter.toSesame(stmt));
					} catch (RDFHandlerException e) {
						throwException(e);
					}
					return null;
				}

				@Override
				public Void visitNamespace(INamespace namespace) {
					try {
						writer.handleNamespace(namespace.getPrefix(), namespace
								.getURI().toString());
					} catch (RDFHandlerException e) {
						throwException(e);
					}
					return null;
				}
			};
		} catch (IOException e) {
			throw new KommaException("Creating RDF writer failed", e);
		}
	}
}
