package net.enilink.komma.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;

import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.Class;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URI;

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

	public static String findOntology(InputStream in, String baseURI)
			throws Exception {
		final org.openrdf.model.URI[] ontology = { null };

		RDFParser parser = Rio.createParser(RDFFormat.RDFXML);
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
}
