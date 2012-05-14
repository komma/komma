package net.enilink.komma.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.openrdf.model.Statement;
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
	public static String getLabel(Object element) {
		StringBuilder text = new StringBuilder();
		if (element instanceof IStatement) {
			element = ((IStatement) element).getObject();
		}
		if (element instanceof Resource) {
			Resource resource = (Resource) element;

			String label = null;
			if (!(resource instanceof Class || resource instanceof Property || resource instanceof IObject
					&& ((IObject) resource).isOntLanguageTerm())) {
				label = resource.getRdfsLabel();
			}
			if (label != null) {
				text.append(label);
			} else {
				URI uri = resource.getURI();
				if (uri != null) {
					String prefix;
					if (resource instanceof IObject
							&& ((IObject) resource).getModel().getURI()
									.trimFragment()
									.equals(uri.namespace().trimFragment())) {
						prefix = "";
					} else {
						prefix = resource.getEntityManager().getPrefix(
								uri.namespace());
					}

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

	private static RDFParser createParser(IContentDescription contentDescription) {
		RDFFormat format = RDFFormat.RDFXML;
		if (contentDescription != null) {
			String mimeType = (String) contentDescription
					.getProperty(new QualifiedName(ModelCore.PLUGIN_ID,
							"mimeType"));
			format = RDFFormat.forMIMEType(mimeType, format);
		}
		return Rio.createParser(format);
	}

	public static String findOntology(InputStream in, String baseURI,
			IContentDescription contentDescription) throws Exception {
		final org.openrdf.model.URI[] ontology = { null };
		RDFParser parser = createParser(contentDescription);
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

	public static <V extends IDataVisitor<?>> void readData(InputStream in,
			String baseURI, IContentDescription contentDescription,
			final V dataVisitor) {
		// generate unique BNode IDs by default
		readData(in, baseURI, contentDescription, false, dataVisitor);
	}

	public static <V extends IDataVisitor<?>> void readData(InputStream in,
			String baseURI, IContentDescription contentDescription,
			boolean preserveBNodeIDs, final V dataVisitor) {
		final SesameValueConverter valueConverter = new SesameValueConverter(
				new ValueFactoryImpl());
		final boolean handleNamespaces = dataVisitor instanceof IDataAndNamespacesVisitor<?>;
		RDFParser parser = createParser(contentDescription);
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
			throw new KommaException("Invalid RDF data", e);
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
			IContentDescription contentDescription) throws IOException {
		RDFFormat format = RDFFormat.RDFXML;
		if (contentDescription != null) {
			String mimeType = (String) contentDescription
					.getProperty(new QualifiedName(ModelCore.PLUGIN_ID,
							"mimeType"));
			format = RDFFormat.forMIMEType(mimeType, format);
		}
		if (RDFFormat.RDFXML.equals(format)) {
			// use a special pretty writer in case of RDF/XML
			RDFXMLPrettyWriter rdfXmlWriter = new RDFXMLPrettyWriter(
					new OutputStreamWriter(os, "UTF-8"));
			rdfXmlWriter.setBaseURI(baseURI);
			return rdfXmlWriter;
		} else {
			RDFWriterFactory factory = RDFWriterRegistry.getInstance().get(
					format);
			return factory.getWriter(os);
		}
	}

	public static IDataAndNamespacesVisitor<Void> writeData(OutputStream os,
			String baseURI, IContentDescription contentDescription) {
		try {
			final RDFWriter writer = createWriter(os, baseURI,
					contentDescription);
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
