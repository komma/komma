/*
 * Copyright (c) 2008, 2010, Zepheira All rights reserved.
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

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mappers.RoleMapper;
import org.openrdf.model.LiteralFactory;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.TupleQuery;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.result.NamespaceResult;
import org.openrdf.result.Result;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.URI;

public class JavaNameResolverImpl implements JavaNameResolver {
	private final Logger log = LoggerFactory.getLogger(JavaNameResolver.class);

	private static final String FILTER_REGEX_PATTERN = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "SELECT ?thing\n"
			+ "WHERE { ?thing rdf:type ?type .\n"
			+ "	FILTER regex(str(?thing), ?pattern, \"i\")}";

	/** namespace -&gt; package */
	@Inject
	@Named("packages")
	private Map<String, String> packages = new HashMap<String, String>();

	/** namespace -&gt; prefix */
	private Map<String, String> prefixes = new HashMap<String, String>();

	private Map<URI, URI> aliases = new HashMap<URI, URI>();

	private RoleMapper<org.openrdf.model.URI> roles;

	@Inject
	private LiteralConverter literalConverter;

	private ClassLoaderPackages cl;

	private Repository repository;

	private Set<String> nouns = new HashSet<String>();

	private static class ClassLoaderPackages extends ClassLoader {
		public ClassLoaderPackages(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Package[] getPackages() {
			return super.getPackages();
		}
	}

	public JavaNameResolverImpl() {
		this(Thread.currentThread().getContextClassLoader());
	}

	public JavaNameResolverImpl(ClassLoader cl) {
		this.cl = new ClassLoaderPackages(cl);
	}

	@Inject
	public void setRoleMapper(RoleMapper<org.openrdf.model.URI> roles) {
		this.roles = roles;
	}

	@Inject
	public void setRepository(Repository repository) throws StoreException {
		this.repository = repository;
		Set<String> localNames = new HashSet<String>();
		RepositoryConnection con = repository.getConnection();
		try {
			NamespaceResult result = con.getNamespaces();
			try {
				while (result.hasNext()) {
					Namespace ns = result.next();
					bindPrefixToNamespace(ns.getPrefix(), ns.getName());
				}
			} finally {
				result.close();
			}
			Result<Statement> stmts = con.match(null, null, null, false);
			try {
				while (stmts.hasNext()) {
					Resource subj = stmts.next().getSubject();
					if (subj instanceof org.openrdf.model.URI) {
						localNames.add(((org.openrdf.model.URI) subj)
								.getLocalName());
					}
				}
			} finally {
				stmts.close();
			}
		} finally {
			con.close();
		}
		for (String name : localNames) {
			if (!name.matches(".[A-Z_-]")) {
				nouns.add(name.toLowerCase());
			}
		}
	}

	public void assignAlias(URI name, URI alias) {
		aliases.put(name, alias);
	}

	public void assignAnonymous(URI name) {
		aliases.put(name, null);
	}

	public void bindPrefixToNamespace(String prefix, String namespace) {
		if (prefix == null || prefix.length() == 0) {
			prefixes.remove(namespace);
		} else {
			prefixes.put(namespace, enc(prefix));
		}
	}

	public URI getType(URI name) {
		if (aliases.containsKey(name)) {
			return aliases.get(name);
		}
		return name;
	}

	public String getClassName(URI name) {
		if (name == null) {
			return Object.class.getName();
		}
		if (!packages.containsKey(name.namespace())) {
			Class<?> javaClass = findJavaClass(name);
			if (javaClass != null) {
				// TODO support n-dimension arrays
				if (javaClass.isArray()) {
					return javaClass.getComponentType().getName() + "[]";
				}
				if (javaClass.getPackage() != null) {
					return javaClass.getName();
				}
			}
		}
		String pkg = getPackageName(name);
		String simple = initcap(name.localPart());
		if (pkg == null) {
			return simple;
		}
		return pkg + '.' + simple;
	}

	public String getMethodName(URI name) {
		String ns = name.namespace().toString();
		String localPart = name.localPart();
		if (prefixes.containsKey(ns)) {
			return prefixes.get(ns) + initcap(localPart);
		}
		return enc(localPart);
	}

	public String getPackageName(URI uri) {
		if (packages.containsKey(uri.namespace().toString())) {
			return packages.get(uri.namespace().toString());
		}
		Class<?> javaClass = findJavaClass(uri);
		if (javaClass == null || javaClass.getPackage() == null) {
			log.error("Unable to determine Java class for {}", uri);
			return null;
		}
		return javaClass.getPackage().getName();
	}

	public String getPropertyName(URI name) {
		String ns = name.namespace().toString();
		String localPart = name.localPart();
		if (prefixes.containsKey(ns))
			return prefixes.get(ns) + initcap(localPart);
		return enc(localPart);
	}

	public String getPluralPropertyName(URI name) {
		String ns = name.namespace().toString();
		String localPart = name.localPart();
		if (prefixes.containsKey(ns)) {
			return prefixes.get(ns) + plural(initcap(localPart));
		}
		return plural(enc(localPart));
	}

	public String getSimpleName(URI name) {
		return initcap(name.localPart());
	}

	private URIImpl asSesameURI(URI uri) {
		return new URIImpl(uri.toString());
	}

	private String enc(String str) {
		if (str.length() == 0) {
			return "_";
		}
		char[] name = str.toCharArray();
		StringBuffer sb = new StringBuffer(name.length);
		for (int i = 0; i < name.length; i++) {
			if (name[i] == '-' || name[i] == '.') {
				name[i + 1] = Character.toUpperCase(name[i + 1]);
			} else if ('A' <= name[i] && name[i] <= 'Z' || 'a' <= name[i]
					&& name[i] <= 'z') {
				sb.append(name[i]);
			} else if (i > 0 && '0' <= name[i] && name[i] <= '9') {
				sb.append(name[i]);
			} else if ('*' == name[i]) {
				sb.append("Star");
			} else if ('#' == name[i]) {
				sb.append("Hash");
			} else {
				sb.append('_');
			}
		}
		return sb.toString();
	}

	private Class<?> findJavaClass(URI uri) {
		if (uri.equals(RDF.TYPE_XMLLITERAL)) {
			return literalConverter.findClass(uri);
		}
		Class<?> klass = findBeanClassName(uri);
		if (klass != null) {
			return klass;
		}
		klass = findLoadedMethod(uri);
		if (klass != null) {
			return klass;
		}
		return literalConverter.findClass(uri);
	}

	private Class<?> findBeanClassName(URI uri) {
		URIImpl sesameUri = asSesameURI(uri);
		boolean recorded = roles.isRecordedConcept(sesameUri);
		if (recorded) {
			Collection<Class<?>> rs = roles.findRoles(sesameUri);
			for (Class<?> r : rs) {
				if (r.isInterface() && sesameUri.equals(roles.findType(r))
						&& r.getSimpleName().equals(sesameUri.getLocalName())) {
					return r;
				}
			}
			for (Class<?> r : rs) {
				if (r.isInterface() && sesameUri.equals(roles.findType(r))) {
					return r;
				}
			}
		}
		return null;
	}

	private Class<?> findLoadedMethod(URI uri) {
		if (cl == null)
			return null;
		String sn = getSimpleName(uri);
		for (Package pkg : cl.getPackages()) {
			if (pkg.isAnnotationPresent(Iri.class)) {
				String namespace = pkg.getAnnotation(Iri.class).value();
				if (uri.namespace().equals(namespace)) {
					try {
						return Class.forName(pkg.getName() + '.' + sn);
					} catch (ClassNotFoundException e) {
						continue;
					}
				}
			}
		}
		return null;
	}

	private String plural(String singular) {
		if (singular.matches(".*[A-Z_-].*")
				&& !isNoun(singular.replaceAll(".*(?=[A-Z])|.*[_-]", ""))) {
			return singular;
		} else if (singular.endsWith("s") && !singular.endsWith("ss")) {
			return singular;
		} else if (singular.endsWith("ed")) {
			return singular;
		} else if (singular.endsWith("y") && (singular.length() > 1)) {
			char c = singular.charAt(singular.length() - 2);
			if (c == 'a' || c == 'o' || c == 'e' || c == 'u' || c == 'i') {
				return singular + "s";
			} else {
				return singular.substring(0, singular.length() - 1) + "ies";
			}
		} else if (singular.endsWith("s") || singular.endsWith("x")) {
			return singular + "es";
		} else {
			return singular + "s";
		}
	}

	/**
	 * If this is word is a thing in our repository it is a noun. An alternative
	 * is to use a wordnet database.
	 */
	private boolean isNoun(String word) {
		if (nouns != null)
			return nouns.contains(word);
		try {
			if (repository == null)
				return false;
			RepositoryConnection con = repository.getConnection();
			try {
				TupleQuery query = con.prepareTupleQuery(SPARQL,
						FILTER_REGEX_PATTERN);
				LiteralFactory lf = repository.getLiteralFactory();
				query.setBinding("pattern", lf.createLiteral("[#/:]$word\\$"));
				TupleResult result = query.evaluate();
				try {
					return result.hasNext();
				} finally {
					result.close();
				}
			} finally {
				con.close();
			}
		} catch (StoreException exc) {
			return false;
		} catch (MalformedQueryException exc) {
			throw new AssertionError(exc);
		}
	}

	private String initcap(String str) {
		if (str.length() == 0)
			return "";
		char[] name = str.toCharArray();
		StringBuffer sb = new StringBuffer(name.length);
		for (int i = 0; i < name.length; i++) {
			if (i == 0) {
				sb.append(Character.toUpperCase(name[i]));
			} else if (name[i] == '-' || name[i] == '.') {
				name[i + 1] = Character.toUpperCase(name[i + 1]);
			} else {
				sb.append(name[i]);
			}
		}
		String string = sb.toString();
		if (!Character.isLetter(string.charAt(0))) {
			string = "_" + string;
		}
		return string;
	}
}
