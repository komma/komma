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
package net.enilink.komma.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.generator.concepts.CodeClass;
import net.enilink.komma.generator.concepts.CodeOntology;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.owl.Ontology;
import net.enilink.vocab.rdfs.Datatype;
import net.enilink.vocab.rdfs.RDFS;

/**
 * Converts OWL ontologies into JavaBeans. This class can be used to create Elmo
 * concepts or other JavaBean interfaces or classes.
 * 
 * @author James Leigh
 * 
 */
public class CodeGenerator implements IGenerator {
	private static final String JAVA_NS = "java:";

	private static final String SELECT_CLASSES = "PREFIX rdfs: <"
			+ RDFS.NAMESPACE
			+ "> PREFIX owl: <"
			+ OWL.NAMESPACE
			+ "> SELECT DISTINCT ?bean WHERE { { ?bean a owl:Class } UNION {?bean a rdfs:Datatype } }";

	private Collection<Class<?>> baseClasses = Collections.emptyList();

	private Exception exception;

	final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);

	@Inject
	private IEntityManager manager;

	@Inject
	IUnitOfWork unitOfWork;

	/** namespace -&gt; package */
	@Inject
	@Named("packages")
	private Map<String, String> packages = new HashMap<String, String>();

	private String propertyNamesPrefix;

	private JavaNameResolver resolver;

	private void addBaseClass(IEntity bean) {
		net.enilink.vocab.rdfs.Class klass;
		klass = (net.enilink.vocab.rdfs.Class) bean;
		Class<net.enilink.vocab.rdfs.Class> rdfsClass = net.enilink.vocab.rdfs.Class.class;
		if (!containKnownNamespace(klass.getRdfsSubClassOf())) {
			for (Class<?> b : baseClasses) {
				URI name = URIs.createURI(JAVA_NS + b.getName());
				klass.getRdfsSubClassOf().add(
						bean.getEntityManager().createNamed(name, rdfsClass));
			}
		}
	}

	private void buildClass(IEntity bean, String packageName,
			SourceCodeHandler handler) throws Exception {
		String code = ((CodeClass) bean).generateSourceCode(resolver);
		handler.handleSource(code);
	}

	private void buildClassOrDatatype(IEntity bean, String packageName,
			SourceCodeHandler handler) {
		try {
			if (bean instanceof Datatype) {
				buildDatatype(bean, packageName, handler);
			} else {
				buildClass(bean, packageName, handler);
			}
		} catch (Exception exc) {
			logger.error("Error processing {}", bean);
			if (exception == null) {
				exception = exc;
			}
		}
	}

	private void buildDatatype(IEntity bean, String packageName,
			SourceCodeHandler handler) throws Exception {
		String code = ((CodeClass) bean).generateSourceCode(resolver);
		handler.handleSource(code);
	}

	private void buildPackage(IEntityManager manager, String namespace,
			SourceCodeHandler handler) throws Exception {
		Ontology ont = findOntology(manager, namespace);
		CodeOntology codeOnt = (CodeOntology) ont;
		String code = codeOnt.generatePackageInfo(namespace, resolver);
		handler.handleSource(code);
	}

	private boolean containKnownNamespace(Set<? extends IEntity> set) {
		boolean contain = false;
		for (IEntity e : set) {
			URI name = e.getURI();
			if (name == null)
				continue;
			if (packages.containsKey(name.namespace().toString())) {
				contain = true;
			}
		}
		return contain;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * net.enilink.komma.generator.IGenerator#exportSourceCode(de.fraunhofer
	 * .iwu.komma.generator.SourceCodeHandler)
	 */
	public void exportSourceCode(final SourceCodeHandler handler)
			throws Exception {
		ExecutorService execSvc = Executors.newFixedThreadPool(3);
		List<Callable<Void>> tasks = new ArrayList<>();

		IQuery<?> query = manager.createQuery(SELECT_CLASSES);
		for (Object o : query.getResultList()) {
			final IEntity bean = (IEntity) o;
			if (bean.getURI() == null)
				continue;
			String namespace = bean.getURI().namespace().toString();
			if (packages.containsKey(namespace)) {
				addBaseClass(bean);
				final String pkg = packages.get(namespace);
				tasks.add(new Callable<Void>() {
					public Void call() {
						unitOfWork.begin();
						buildClassOrDatatype(bean, pkg, handler);
						unitOfWork.end();
						return null;
					}
				});
			}
		}
		query.close();

		execSvc.invokeAll(tasks);
		execSvc.shutdown();

		for (String namespace : packages.keySet()) {
			buildPackage(manager, namespace, handler);
		}

		if (exception != null) {
			throw exception;
		}
	}

	private Ontology findOntology(IEntityManager manager, String namespace) {
		if (namespace.endsWith("#"))
			return manager.createNamed(URIs.createURI(namespace.substring(0,
					namespace.length() - 1)), Ontology.class);
		return manager.createNamed(URIs.createURI(namespace), Ontology.class);
	}

	public Collection<Class<?>> getBaseClasses() {
		return baseClasses;
	}

	public String getPropertyNamesPrefix() {
		return propertyNamesPrefix;
	}

	@Inject(optional = true)
	public void setBaseClasses(
			@Named("baseClasses") Collection<Class<?>> baseClasses) {
		this.baseClasses = baseClasses;
	}

	@Inject
	public void setJavaNameResolver(JavaNameResolver resolver) {
		this.resolver = resolver;
	}

	@Inject(optional = true)
	public void setPropertyNamesPrefix(
			@Named("propertyNamesPrefix") String prefixPropertyNames) {
		this.propertyNamesPrefix = prefixPropertyNames;
	}
}