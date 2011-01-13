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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdf.Property;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.generator.source.JavaClassBuilder;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

/**
 * Creates profiles with constants for classes and properties.
 * 
 */
public class ConstantsClassesGenerator implements IGenerator {
	private static final String SELECT_CLASSES = "PREFIX rdfs: <"
			+ RDFS.NAMESPACE
			+ "> PREFIX owl: <"
			+ OWL.NAMESPACE
			+ "> SELECT DISTINCT ?bean WHERE { { ?bean a owl:Class } UNION {?bean a rdfs:Datatype } }";

	final Logger logger = LoggerFactory
			.getLogger(ConstantsClassesGenerator.class);

	@Inject
	private IEntityManager manager;

	/** namespace -&gt; package */
	@Inject
	@Named("packages")
	private Map<String, String> packages = Collections.emptyMap();

	public void exportSourceCode(final SourceCodeHandler handler)
			throws Exception {
		generateConstantsClasses(handler);
	}

	@SuppressWarnings("unchecked")
	private void generateConstantsClasses(final SourceCodeHandler handler)
			throws Exception {
		Map<URI, Set<IEntity>> namespaceToEntity = new HashMap<URI, Set<IEntity>>();

		for (IEntity entity : ((IExtendedIterator<IEntity>) manager
				.createQuery(SELECT_CLASSES).evaluate()).andThen(manager
				.findAll(Property.class))) {
			if (entity.getURI() == null) {
				continue;
			}

			URI namespace = entity.getURI().namespace();
			if (packages.containsKey(namespace.toString())) {
				Set<IEntity> entities = namespaceToEntity.get(namespace);
				if (entities == null) {
					entities = new LinkedHashSet<IEntity>();
					namespaceToEntity.put(namespace, entities);
				}
				entities.add(entity);
			}
		}

		for (Map.Entry<URI, Set<IEntity>> entry : namespaceToEntity.entrySet()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JavaClassBuilder jcb = new JavaClassBuilder(new PrintWriter(baos));

			String pkg = packages.get(entry.getKey().toString());
			jcb.pkg(pkg);

			String[] parts = pkg.split("\\.");
			String className = parts[parts.length - 1].toUpperCase();

			// jcb.annotateURI(Iri.class, entry.getKey());
			jcb.interfaceName(className);

			jcb.staticStringField("NAMESPACE", entry.getKey().toString());
			jcb.code("\tpublic static final ").code(jcb.imports(URI.class))
					.code(" NAMESPACE_URI = ").code(jcb.imports(URIImpl.class))
					.code(".createURI(NAMESPACE);\n\n");

			for (IEntity entity : entry.getValue()) {
				String name = "";
				if (entity instanceof Property) {
					name += "PROPERTY_";
				} else if (entity instanceof net.enilink.vocab.rdfs.Class) {
					name += "TYPE_";
				}
				name += entity.getURI().localPart().toUpperCase();

				jcb.code("\tpublic static final ").code(jcb.imports(URI.class))
						.code(" ").code(name)
						.code(" = NAMESPACE_URI.appendFragment(\"")
						.code(entity.getURI().localPart()).code("\");\n\n");
			}

			jcb.close();

			String code = baos.toString();

			handler.handleSource(code);
		}
	}
}