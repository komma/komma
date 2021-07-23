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
import net.enilink.komma.core.ILiteralMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.literals.internal.BigDecimalLiteralMapper;
import net.enilink.komma.literals.internal.BigIntegerLiteralMapper;
import net.enilink.komma.literals.internal.BooleanLiteralMapper;
import net.enilink.komma.literals.internal.ByteLiteralMapper;
import net.enilink.komma.literals.internal.CharacterLiteralMapper;
import net.enilink.komma.literals.internal.ClassLiteralMapper;
import net.enilink.komma.literals.internal.DateLiteralMapper;
import net.enilink.komma.literals.internal.DoubleLiteralMapper;
import net.enilink.komma.literals.internal.DurationLiteralMapper;
import net.enilink.komma.literals.internal.FloatLiteralMapper;
import net.enilink.komma.literals.internal.GregorianCalendarLiteralMapper;
import net.enilink.komma.literals.internal.IntegerLiteralMapper;
import net.enilink.komma.literals.internal.LocaleLiteralMapper;
import net.enilink.komma.literals.internal.LongLiteralMapper;
import net.enilink.komma.literals.internal.ObjectConstructorLiteralMapper;
import net.enilink.komma.literals.internal.ObjectSerializationLiteralMapper;
import net.enilink.komma.literals.internal.PatternLiteralMapper;
import net.enilink.komma.literals.internal.QNameLiteralMapper;
import net.enilink.komma.literals.internal.ShortLiteralMapper;
import net.enilink.komma.literals.internal.SqlDateLiteralMapper;
import net.enilink.komma.literals.internal.SqlTimeLiteralMapper;
import net.enilink.komma.literals.internal.SqlTimestampLiteralMapper;
import net.enilink.komma.literals.internal.StringLiteralMapper;
import net.enilink.komma.literals.internal.ValueOfLiteralMapper;
import net.enilink.komma.literals.internal.XMLGregorianCalendarLiteralMapper;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.ILiteralFactory;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

/**
 * Converts between simple Java Objects and Strings.
 * 
 */
public class LiteralConverter implements Cloneable {
	private static final String DATATYPES_PROPERTIES = "META-INF/net.enilink.komma.datatypes";

	private static final String JAVA_SCHEME = "java";

	private ClassLoader cl;
	private ConcurrentMap<String, ILiteralMapper<?>> converters = new ConcurrentHashMap<String, ILiteralMapper<?>>();

	Injector injector;

	private ConcurrentMap<URI, Class<?>> javaClasses = new ConcurrentHashMap<URI, Class<?>>();
	@Inject
	private ILiteralFactory literalFactory;
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
			cloned.converters = new ConcurrentHashMap<String, ILiteralMapper<?>>(
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
			return literalFactory
					.createLiteral((String) object, datatype, null);
		}
		ILiteralMapper<Object> converter = null;
		if (null != datatype) {
			converter = (ILiteralMapper<Object>) findConverter(datatype);
		} else {
			converter = (ILiteralMapper<Object>) findConverter(object.getClass());
		}
		return converter.serialize(object);
	}

	public Object createObject(ILiteral literal) {
		URI datatype = literal.getDatatype();
		if (datatype == null) {
			return literal.getLabel();
		}
		ILiteralMapper<?> converter = findConverter(datatype);
		try {
			return converter.deserialize(literal.getLabel());
		} catch (Exception e) {
			logger.warn("Conversion of literal " + literal + " failed.", e);
			return literal;
		}
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
	private <T> ILiteralMapper<T> findConverter(Class<T> type) {
		String name = type.getName();
		if (converters.containsKey(name)) {
			return (ILiteralMapper<T>) converters.get(name);
		}
		ILiteralMapper<T> converter;
		try {
			converter = new ValueOfLiteralMapper<T>(type);
		} catch (NoSuchMethodException e1) {
			try {
				converter = new ObjectConstructorLiteralMapper<T>(type);
			} catch (NoSuchMethodException e2) {
				if (Serializable.class.isAssignableFrom(type)) {
					converter = new ObjectSerializationLiteralMapper<T>(type);
				} else {
					throw new ObjectConversionException(e1);
				}
			}
		}
		injector.injectMembers(converter);

		ILiteralMapper<?> o = converters.putIfAbsent(name, converter);
		if (o != null) {
			converter = (ILiteralMapper<T>) o;
		}
		return converter;
	}

	private ILiteralMapper<?> findConverter(URI datatype) {
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
		URI datatype = URIs.createURI(JAVA_SCHEME + ":" + type.getName());
		recordType(type, datatype);
		return datatype;
	}

	// TODO: check if this two-part test is required
	public boolean isDatatype(Class<?> type) {
		return rdfTypes.containsKey(type) || null != findConverter(type);
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
							recordType(lc, URIs.createURI(rdf));
						} else if (rdf.length() == 0) {
							logger.warn("Unkown datatype mapping {}", className);
						} else {
							recordType(lc, URIs.createURI(rdf));
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
			ILiteralMapper<?> converter = findConverter(javaClass);
			converter.setDatatype(datatype);
		}
	}

	public void registerConverter(Class<?> javaClass, ILiteralMapper<?> converter) {
		registerConverter(javaClass.getName(), converter);
	}

	private void registerConverter(ILiteralMapper<?> converter) {
		registerConverter(converter.getJavaClassName(), converter);
	}

	public void registerConverter(String javaClassName, ILiteralMapper<?> converter) {
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
			registerConverter(new BigDecimalLiteralMapper());
			registerConverter(new BigIntegerLiteralMapper());
			registerConverter(new BooleanLiteralMapper());
			registerConverter(new ByteLiteralMapper());
			registerConverter(new DoubleLiteralMapper());
			registerConverter(new FloatLiteralMapper());
			registerConverter(new IntegerLiteralMapper());
			registerConverter(new LongLiteralMapper());
			registerConverter(new ShortLiteralMapper());
			registerConverter(new CharacterLiteralMapper());
			registerConverter(new DateLiteralMapper());
			registerConverter(new LocaleLiteralMapper());
			registerConverter(new PatternLiteralMapper());
			registerConverter(new QNameLiteralMapper());
			registerConverter(new GregorianCalendarLiteralMapper());
			registerConverter(new SqlDateLiteralMapper());
			registerConverter(new SqlTimeLiteralMapper());
			registerConverter(new SqlTimestampLiteralMapper());
			registerConverter(new ClassLiteralMapper());

			DurationLiteralMapper dm = injector
					.getInstance(DurationLiteralMapper.class);
			registerConverter(dm.getJavaClassName(), dm);
			registerConverter(Duration.class, dm);
			XMLGregorianCalendarLiteralMapper xgcm = injector
					.getInstance(XMLGregorianCalendarLiteralMapper.class);
			registerConverter(xgcm.getJavaClassName(), xgcm);
			registerConverter(XMLGregorianCalendar.class, xgcm);
			registerConverter(new StringLiteralMapper(
					"org.codehaus.groovy.runtime.GStringImpl"));
			registerConverter(new StringLiteralMapper("groovy.lang.GString$1"));
			registerConverter(new StringLiteralMapper("groovy.lang.GString$2"));

			registerConverter(new StringLiteralMapper("java.lang.String",
					RDF.TYPE_XMLLITERAL));

			loadDatatypes(getClass().getClassLoader(), DATATYPES_PROPERTIES);
			loadDatatypes(cl, DATATYPES_PROPERTIES);
		} catch (Exception e) {
			throw new ObjectConversionException(e);
		}
	}
}
