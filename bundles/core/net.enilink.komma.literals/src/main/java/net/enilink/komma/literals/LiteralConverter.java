/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
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
package net.enilink.komma.literals;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.properties.exceptions.ObjectConversionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.literals.internal.BigDecimalConverter;
import net.enilink.komma.literals.internal.BigIntegerConverter;
import net.enilink.komma.literals.internal.BooleanConverter;
import net.enilink.komma.literals.internal.ByteConverter;
import net.enilink.komma.literals.internal.CharacterConverter;
import net.enilink.komma.literals.internal.ClassConverter;
import net.enilink.komma.literals.internal.DateConverter;
import net.enilink.komma.literals.internal.DoubleConverter;
import net.enilink.komma.literals.internal.DurationConverter;
import net.enilink.komma.literals.internal.FloatConverter;
import net.enilink.komma.literals.internal.GregorianCalendarConverter;
import net.enilink.komma.literals.internal.IntegerConverter;
import net.enilink.komma.literals.internal.LocaleConverter;
import net.enilink.komma.literals.internal.LongConverter;
import net.enilink.komma.literals.internal.ObjectConstructorConverter;
import net.enilink.komma.literals.internal.ObjectSerializationConverter;
import net.enilink.komma.literals.internal.PatternConverter;
import net.enilink.komma.literals.internal.QNameConverter;
import net.enilink.komma.literals.internal.ShortConverter;
import net.enilink.komma.literals.internal.SqlDateConverter;
import net.enilink.komma.literals.internal.SqlTimeConverter;
import net.enilink.komma.literals.internal.SqlTimestampConverter;
import net.enilink.komma.literals.internal.StringConverter;
import net.enilink.komma.literals.internal.ValueOfConverter;
import net.enilink.komma.literals.internal.XMLGregorianCalendarConverter;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

/**
 * Converts between simple Java Objects and Strings.
 * 
 */
public class LiteralConverter implements Cloneable {
	private static final String DATATYPES_PROPERTIES = "META-INF/net.enilink.komma.datatypes";

	private static final String JAVA_SCHEME = "java";

	private ClassLoader cl;
	private ConcurrentMap<String, IConverter<?>> converters = new ConcurrentHashMap<String, IConverter<?>>();

	Injector injector;

	private ConcurrentMap<URI, Class<?>> javaClasses = new ConcurrentHashMap<URI, Class<?>>();
	@Inject
	private ILiteralFactory literalFactory;;
	private final Logger logger = LoggerFactory
			.getLogger(LiteralConverter.class);
	private ConcurrentMap<Class<?>, URI> rdfTypes = new ConcurrentHashMap<Class<?>, URI>();

	public void addDatatype(Class<?> javaClass, URI datatype) {
		recordType(javaClass, datatype);
	}

	public LiteralConverter clone() {
		try {
			LiteralConverter cloned = (LiteralConverter) super.clone();
			cloned.javaClasses = new ConcurrentHashMap<URI, Class<?>>(
					javaClasses);
			cloned.converters = new ConcurrentHashMap<String, IConverter<?>>(
					converters);
			cloned.rdfTypes = new ConcurrentHashMap<Class<?>, URI>(rdfTypes);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	@SuppressWarnings("unchecked")
	public ILiteral createLiteral(Object object, URI datatype) {
		if (object instanceof String) {
			return literalFactory.createLiteral(object, (String) object,
					datatype, null);
		}
		IConverter<Object> converter = null;
		if (null != datatype) {
			converter = (IConverter<Object>) findConverter(datatype);
		} else {
			converter = (IConverter<Object>) findConverter(object.getClass());
		}
		return converter.serialize(object);
	}

	public Object createObject(String label, URI datatype) {
		if (datatype == null) {
			return label;
		}
		IConverter<?> converter = findConverter(datatype);
		return converter.deserialize(label);
	}

	public Class<?> findClass(URI datatype) {
		if (javaClasses.containsKey(datatype)) {
			return javaClasses.get(datatype);
		}
		try {
			if (datatype.scheme().equals(JAVA_SCHEME)) {
				return Class.forName(datatype.localPart(), true, cl);
			}
		} catch (ClassNotFoundException e) {
			throw new ObjectConversionException(e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T> IConverter<T> findConverter(Class<T> type) {
		String name = type.getName();
		if (converters.containsKey(name)) {
			return (IConverter<T>) converters.get(name);
		}
		IConverter<T> converter;
		try {
			converter = new ValueOfConverter<T>(type);
		} catch (NoSuchMethodException e1) {
			try {
				converter = new ObjectConstructorConverter<T>(type);
			} catch (NoSuchMethodException e2) {
				if (Serializable.class.isAssignableFrom(type)) {
					converter = new ObjectSerializationConverter<T>(type);
				} else {
					throw new ObjectConversionException(e1);
				}
			}
		}
		injector.injectMembers(converter);

		IConverter<?> o = converters.putIfAbsent(name, converter);
		if (o != null) {
			converter = (IConverter<T>) o;
		}
		return converter;
	}

	private IConverter<?> findConverter(URI datatype) {
		Class<?> type;
		if (javaClasses.containsKey(datatype)) {
			type = javaClasses.get(datatype);
		} else if (datatype.scheme().equals(JAVA_SCHEME)) {
			try {
				type = Class.forName(datatype.localPart(), true, cl);
			} catch (ClassNotFoundException e) {
				throw new ObjectConversionException(e);
			}
		} else {
			throw new ObjectConversionException("Unknown datatype: " + datatype);
		}
		return findConverter(type);
	}

	public URI findDatatype(Class<?> type) {
		if (type.equals(String.class))
			return null;
		if (rdfTypes.containsKey(type))
			return rdfTypes.get(type);
		URI datatype = URIImpl.createURI(JAVA_SCHEME + ":" + type.getName());
		recordType(type, datatype);
		return datatype;
	}

	// TODO: check if this two-part test is required
	public boolean isDatatype(Class<?> type) {
		return rdfTypes.containsKey(type) || null != findConverter(type);
	}

	public boolean isRecordedeType(URI datatype) {
		return findClass(datatype) != null;
	}

	private void loadDatatypes(ClassLoader cl, String properties)
			throws IOException, ClassNotFoundException {
		if (cl == null) {
			return;
		}
		Enumeration<URL> resources = cl.getResources(properties);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			try {
				Properties p = new Properties();
				p.load(url.openStream());
				for (Map.Entry<?, ?> e : p.entrySet()) {
					String className = (String) e.getKey();
					String types = (String) e.getValue();
					Class<?> lc = Class.forName(className, true, cl);
					boolean present = lc.isAnnotationPresent(Iri.class);
					for (String rdf : types.split("\\s+")) {
						if (rdf.length() == 0 && present) {
							rdf = lc.getAnnotation(Iri.class).value();
							recordType(lc, URIImpl.createURI(rdf));
						} else if (rdf.length() == 0) {
							logger.warn("Unkown datatype mapping {}", className);
						} else {
							recordType(lc, URIImpl.createURI(rdf));
						}
					}
				}
			} catch (IOException e) {
				String msg = e.getMessage() + " in: " + url;
				IOException ioe = new IOException(msg);
				ioe.initCause(e);
				throw ioe;
			}
		}
	}

	public void recordType(Class<?> javaClass, URI datatype) {
		if (!javaClasses.containsKey(datatype)) {
			javaClasses.putIfAbsent(datatype, javaClass);
		}
		if (rdfTypes.putIfAbsent(javaClass, datatype) == null) {
			IConverter<?> converter = findConverter(javaClass);
			converter.setDatatype(datatype);
		}
	}

	public void registerConverter(Class<?> javaClass, IConverter<?> converter) {
		registerConverter(javaClass.getName(), converter);
	}

	private void registerConverter(IConverter<?> converter) {
		registerConverter(converter.getJavaClassName(), converter);
	}

	public void registerConverter(String javaClassName, IConverter<?> converter) {
		injector.injectMembers(converter);

		converters.put(javaClassName, converter);
	}

	@Inject
	protected void setClassLoaderAndInjector(final ClassLoader cl,
			Injector injector) {
		this.cl = cl;
		this.injector = injector = injector
				.createChildInjector(new AbstractModule() {
					@Override
					protected void configure() {
					}

					@Provides
					@SuppressWarnings("unused")
					protected DatatypeFactory provideDatatypeFactory()
							throws DatatypeConfigurationException {

						// workaround for classloading issues w/ factory methods
						// http://community.jboss.org/wiki/ModuleCompatibleClassloadingGuide

						ClassLoader oldTCCL = Thread.currentThread()
								.getContextClassLoader();
						try {
							Thread.currentThread().setContextClassLoader(cl);
							return DatatypeFactory.newInstance();
						} finally {
							Thread.currentThread().setContextClassLoader(
									oldTCCL);
						}
					}
				});

		try {
			registerConverter(new BigDecimalConverter());
			registerConverter(new BigIntegerConverter());
			registerConverter(new BooleanConverter());
			registerConverter(new ByteConverter());
			registerConverter(new DoubleConverter());
			registerConverter(new FloatConverter());
			registerConverter(new IntegerConverter());
			registerConverter(new LongConverter());
			registerConverter(new ShortConverter());
			registerConverter(new CharacterConverter());
			registerConverter(new DateConverter());
			registerConverter(new LocaleConverter());
			registerConverter(new PatternConverter());
			registerConverter(new QNameConverter());
			registerConverter(new GregorianCalendarConverter());
			registerConverter(new SqlDateConverter());
			registerConverter(new SqlTimeConverter());
			registerConverter(new SqlTimestampConverter());
			registerConverter(new ClassConverter());

			DurationConverter dm = injector
					.getInstance(DurationConverter.class);
			registerConverter(dm.getJavaClassName(), dm);
			registerConverter(Duration.class, dm);
			XMLGregorianCalendarConverter xgcm = injector
					.getInstance(XMLGregorianCalendarConverter.class);
			registerConverter(xgcm.getJavaClassName(), xgcm);
			registerConverter(XMLGregorianCalendar.class, xgcm);
			registerConverter(new StringConverter(
					"org.codehaus.groovy.runtime.GStringImpl"));
			registerConverter(new StringConverter("groovy.lang.GString$1"));
			registerConverter(new StringConverter("groovy.lang.GString$2"));

			registerConverter(new StringConverter("java.lang.String",
					RDF.TYPE_XMLLITERAL));

			loadDatatypes(getClass().getClassLoader(), DATATYPES_PROPERTIES);
			loadDatatypes(cl, DATATYPES_PROPERTIES);
		} catch (Exception e) {
			throw new ObjectConversionException(e);
		}
	}
}
