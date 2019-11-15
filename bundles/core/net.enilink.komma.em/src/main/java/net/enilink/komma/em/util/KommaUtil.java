/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.util;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.IMap;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.commons.iterator.UniqueExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.TemporalType;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.KommaEM;
import net.enilink.komma.em.concepts.ClassSupport;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IOntology;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.concepts.OntologySupport;
import net.enilink.komma.em.concepts.PropertySupport;
import net.enilink.komma.em.concepts.ResourceSupport;
import net.enilink.vocab.komma.KommaConceptsModule;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.OwlModule;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.rdfs.RdfsModule;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class KommaUtil implements ISparqlConstants {
	public static KommaModule getCoreModule() {
		KommaModule module = new KommaCoreModule();
		module.includeModule(new OwlModule());
		module.includeModule(new RdfsModule());
		module.includeModule(new KommaConceptsModule());

		RoleClassLoader roleClassLoader = new RoleClassLoader(module);
		roleClassLoader.load();

		module.addBehaviour(ResourceSupport.class);
		module.addBehaviour(ClassSupport.class);
		module.addBehaviour(PropertySupport.class);
		module.addBehaviour(OntologySupport.class);

		module.addConcept(IClass.class);
		module.addConcept(IResource.class);
		module.addConcept(IOntology.class);
		module.addConcept(IProperty.class);

		return module;
	}

	public static IExtendedIterator<URL> getConceptLibraries(String bundleName) {
		Enumeration<URL> libraries;
		if (AbstractKommaPlugin.IS_ECLIPSE_RUNNING) {
			Bundle bundle = Platform.getBundle(bundleName);
			libraries = bundle.findEntries("lib", "*.jar", true);
		} else {
			try {
				libraries = KommaUtil.class.getClassLoader().getResources(
						"lib/*.jar");
			} catch (IOException e) {
				KommaEM.INSTANCE.log(e);
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
								return FileLocator.resolve(new URL(URIs
										.createURI(value.toString())
										.trimSegments(2).toString()));
							} catch (Exception e) {
								KommaEM.INSTANCE.log(e);
								return value;
							}
						}
					}).andThen(librarySet.iterator());
		}

		return WrappedIterator.emptyIterator();
	}

	public static boolean isW3cNamespace(URI namespace) {
		if (namespace == null) {
			return false;
		}
		return namespace.equals(OWL.NAMESPACE_URI)
				|| namespace.equals(RDFS.NAMESPACE_URI)
				|| namespace.equals(RDF.NAMESPACE_URI)
				|| namespace.equals(XMLSCHEMA.NAMESPACE_URI);
	}

	/**
	 * Returns all instances within <code>model</code> that have one or more
	 * types contained in <code>classes</code>
	 * 
	 * @param manager
	 * @param classes
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Collection<IResource> getInstances(IEntityManager manager,
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

		IQuery<?> query = manager.createQuery(sb.toString());

		i = 0;
		for (IReference clazz : classes) {
			query.setParameter("c" + i++, clazz);
		}

		return (Collection<IResource>) query.getResultList();
	}

	public static Object convertToType(IEntityManager manager, Object value,
			URI typeName) {
		if (value instanceof ILiteral) {
			value = ((ILiteral) value).getLabel();
		}
		if (RDFS.TYPE_LITERAL.equals(typeName)) {
			typeName = XMLSCHEMA.TYPE_STRING;
		}

		return manager.toInstance(manager.createLiteral(String.valueOf(value),
				typeName, null));
	}

	public static ILiteral createStringLiteral(IEntityManager manager,
			Object value, String languageCode) {
		if (value instanceof ILiteral) {
			value = ((ILiteral) value).getLabel();
		}
		return manager.createLiteral(String.valueOf(value), null, languageCode);
	}

	public static Object convertToRange(IEntityManager manager,
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

	public static Collection<String> getDefaultLanguages() {
		return Arrays.asList("en", "de", "es");
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(Calendar value,
			TemporalType temporalType) {
		assert value instanceof GregorianCalendar : value;
		GregorianCalendar cal = (GregorianCalendar) value;
		try {
			DatatypeFactory factory = DatatypeFactory.newInstance();
			XMLGregorianCalendar xcal = factory.newXMLGregorianCalendar(cal);
			switch (temporalType) {
			case DATE:
				xcal.setHour(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMinute(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setSecond(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
				break;
			case TIME:
				xcal.setYear(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setMonth(DatatypeConstants.FIELD_UNDEFINED);
				xcal.setDay(DatatypeConstants.FIELD_UNDEFINED);
				break;
			case TIMESTAMP:
				break;
			}
			return xcal;
		} catch (DatatypeConfigurationException e) {
			throw new KommaException(e);
		}
	}

	public static XMLGregorianCalendar toXMLGregorianCalendar(Date value,
			TemporalType temporalType) {
		int y, M, d, h, m, s, i, z;
		try {
			z = DatatypeConstants.FIELD_UNDEFINED;
			DatatypeFactory factory = DatatypeFactory.newInstance();

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(value);

			XMLGregorianCalendar xcal;
			switch (temporalType) {
			case DATE:
				y = calendar.get(Calendar.YEAR);
				M = calendar.get(Calendar.MONTH) + 1;
				d = calendar.get(Calendar.DATE);
				xcal = factory.newXMLGregorianCalendarDate(y, M, d, z);
				break;
			case TIME:
				h = calendar.get(Calendar.HOUR);
				m = calendar.get(Calendar.MINUTE);
				s = calendar.get(Calendar.SECOND);
				i = (int) (value.getTime() % 1000);
				xcal = factory.newXMLGregorianCalendarTime(h, m, s, i, z);
				break;
			case TIMESTAMP:
				y = calendar.get(Calendar.YEAR);
				M = calendar.get(Calendar.MONTH) + 1;
				d = calendar.get(Calendar.DATE);
				h = calendar.get(Calendar.HOUR);
				m = calendar.get(Calendar.MINUTE);
				s = calendar.get(Calendar.SECOND);
				i = (int) (value.getTime() % 1000);
				xcal = factory.newXMLGregorianCalendar(y, M, d, h, m, s, i, z);
				break;
			default:
				throw new AssertionError();
			}
			return xcal;
		} catch (DatatypeConfigurationException e) {
			throw new KommaException(e);
		}
	}

}
