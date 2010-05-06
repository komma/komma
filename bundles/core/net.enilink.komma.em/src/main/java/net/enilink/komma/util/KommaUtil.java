/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.osgi.framework.Bundle;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.rdfs.Resource;
import net.enilink.vocab.xmlschema.XMLSCHEMA;
import net.enilink.komma.KommaCore;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.concepts.ClassSupport;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IOntology;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.concepts.OntologySupport;
import net.enilink.komma.concepts.PropertySupport;
import net.enilink.komma.concepts.ResourceSupport;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.core.IKommaManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class KommaUtil implements ISparqlConstants {
	public static void initModule(KommaModule module) {
		// install basic RDF(S) and OWL support
		for (URL url : KommaUtil
				.getBundleMetaInfLocations("net.enilink.vocab.owl")
				.andThen(
						KommaUtil
								.getBundleMetaInfLocations("net.enilink.vocab.rdfs"))) {
			module.addLibrary(url);
		}

		module.addBehaviour(ResourceSupport.class);
		module.addBehaviour(ClassSupport.class);
		module.addBehaviour(PropertySupport.class);
		module.addBehaviour(OntologySupport.class);

		module.addConcept(IClass.class);
		module.addConcept(IResource.class);
		module.addConcept(IOntology.class);
		module.addConcept(IProperty.class);
	}

	@SuppressWarnings("unchecked")
	public static IExtendedIterator<URL> getConceptLibraries(String bundleName) {
		Enumeration<URL> libraries;
		if (KommaCore.IS_ECLIPSE_RUNNING) {
			Bundle bundle = Platform.getBundle(bundleName);
			libraries = bundle.findEntries("lib", "*.jar", true);
		} else {
			try {
				libraries = KommaUtil.class.getClassLoader().getResources(
						"lib/*.jar");
			} catch (IOException e) {
				KommaCore.log(e);
				return NiceIterator.emptyIterator();
			}
		}

		if (libraries != null) {
			Set<URL> librarySet = WrappedIterator.create(libraries).toSet();
			return UniqueExtendedIterator.create(librarySet.iterator())
					.mapWith(new IMap<URL, URL>() {
						@Override
						public URL map(URL value) {
							try {
								return FileLocator.resolve(new URL(URIImpl
										.createURI(value.toString())
										.trimSegments(2).toString()));
							} catch (Exception e) {
								KommaCore.log(e);
								return value;
							}
						}
					}).andThen(librarySet.iterator());
		}

		return WrappedIterator.emptyIterator();
	}

	@SuppressWarnings("unchecked")
	public static IExtendedIterator<URL> getBundleMetaInfLocations(Bundle bundle) {
		if (bundle != null) {
			Enumeration<URL> libraries = bundle.findEntries("/", "META-INF",
					true);

			if (libraries != null) {
				return WrappedIterator.create(libraries).mapWith(
						new IMap<URL, URL>() {
							@Override
							public URL map(URL value) {
								try {
									return FileLocator.resolve(new URL(URIImpl
											.createURI(value.toString())
											.trimSegments(2).toString()));
								} catch (Exception e) {
									KommaCore.log(e);
									return value;
								}
							}
						});
			}
		}

		return WrappedIterator.emptyIterator();
	}

	public static IExtendedIterator<URL> getBundleMetaInfLocations(
			String symbolicName) {
		return getBundleMetaInfLocations(Platform.getBundle(symbolicName));
	}

	public static boolean isW3CLanguageTerm(String namespace) {
		if (namespace == null) {
			return false;
		}

		return namespace.equals(OWL.NAMESPACE)
				|| namespace.equals(RDFS.NAMESPACE)
				|| namespace.equals(RDF.NAMESPACE)
				|| namespace.equals(XMLSCHEMA.NAMESPACE);
	}

	/**
	 * Returns all instances within <code>model</code> that have one or more
	 * types contained in <code>classes</code>
	 * 
	 * @param model
	 * @param classes
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Collection<IObject> getInstances(IModel model,
			Collection<? extends IReference> classes) {
		StringBuilder sb = new StringBuilder("SELECT DISTINCT ?r WHERE {");

		int i = 0;
		for (Iterator<? extends IReference> iterator = classes.iterator(); iterator
				.hasNext();) {
			iterator.next();

			sb.append("{ ?r a" + " ?c").append(i++).append(" }");

			if (iterator.hasNext()) {
				sb.append(" UNION ");
			}
		}
		sb.append("}");

		IQuery<?> query = model.getManager().createQuery(sb.toString());

		i = 0;
		for (IReference clazz : classes) {
			query.setParameter("c" + i++, clazz);
		}

		return (Collection<IObject>) query.getResultList();
	}

	public static Object convertToType(IKommaManager manager, Object value,
			URI typeName) {
		if (value instanceof ILiteral) {
			value = ((ILiteral) value).getLabel();
		}
		if (RDFS.TYPE_LITERAL.equals(typeName)) {
			typeName = XMLSCHEMA.TYPE_STRING;
		}

		return manager.convertValue(manager.createLiteral(
				String.valueOf(value), typeName, null));
	}

	public static ILiteral createStringLiteral(IModel model, Object value,
			String languageCode) {
		if (value instanceof ILiteral) {
			value = ((ILiteral) value).getLabel();
		}

		return model.getManager().createLiteral(String.valueOf(value), null,
				languageCode);
	}

	public static Object convertToRange(IKommaManager manager,
			Collection<? extends IReference> range, Object value) {
		URI rangeName = null;
		Iterator<? extends IReference> rangeIt = range.iterator();
		while (rangeIt.hasNext()) {
			IReference rangeClass = rangeIt.next();
			rangeName = rangeClass.getURI();

			if (rangeName != null) {
				return convertToType(manager, value, rangeName);
			}
		}
		return value;
	}

	public static String getLabel(Object element) {
		StringBuilder text = new StringBuilder();
		if (element instanceof IStatement) {
			element = ((IStatement) element).getObject();
		}
		if (element instanceof Resource) {
			Resource resource = (Resource) element;

			String label = null;
			if (!(resource instanceof IObject && ((IObject) resource)
					.isOntLanguageTerm())) {
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
									.namespace().equals(uri.namespace())) {
						prefix = null;
					} else {
						prefix = resource.getKommaManager().getPrefix(
								uri.namespace());
					}

					if (prefix != null && prefix.length() > 0) {
						text.append(prefix).append(":");
					}

					String fragment = uri.localPart();

					text.append(fragment != null ? fragment : uri.toString());
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

	public static void removeSuperClasses(Set<IClass> classes) {
		for (IClass typeClass : classes.toArray(new IClass[classes.size()])) {
			for (Iterator<IClass> it = classes.iterator(); it.hasNext();) {
				IClass otherClass = it.next();

				if (typeClass != otherClass
						&& typeClass.getRdfsSubClassOf().contains(otherClass)) {
					it.remove();
				}
			}
		}
	}

	public static void unfoldSubClasses(Set<IClass> classes) {
		for (IClass typeClass : classes.toArray(new IClass[classes.size()])) {
			IExtendedIterator<? extends IClass> leafSubClasses = typeClass
					.getNamedLeafSubClasses(true);
			if (leafSubClasses.hasNext()) {
				classes.remove(typeClass);

				classes.addAll(leafSubClasses.toSet());
			}
		}
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

	public static Collection<String> getDefaultLanguages() {
		return Arrays.asList("en", "de", "es");
	}
}
