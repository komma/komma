/*
 * Copyright (c) 2008, 2010, James Leigh All rights reserved.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.URIFactory;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.result.Result;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import net.enilink.komma.common.util.URIUtil;
import net.enilink.komma.generator.concepts.CodeClass;
import net.enilink.komma.generator.concepts.CodeOntology;
import net.enilink.komma.generator.concepts.CodeProperty;
import net.enilink.komma.generator.support.ClassPropertySupport;
import net.enilink.komma.generator.support.CodePropertySupport;
import net.enilink.komma.generator.support.ConceptSupport;
import net.enilink.komma.generator.support.OntologySupport;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.sesame.DecoratingSesameManagerFactory;

/**
 * A Facade to CodeGenerator and OwlGenerator classes. This class provides a
 * simpler interface to create concept packages and build ontologies. Unlike the
 * composed classes, this class reads and creates jar packages.
 * 
 * @author James Leigh
 * 
 */
public class OntologyConverter implements IApplication {
	static class CommandLineModule extends AbstractModule {
		CommandLine commandLine;

		CommandLineModule(CommandLine commandLine) {
			this.commandLine = commandLine;
		}

		@Override
		protected void configure() throws RuntimeException {
			try {
				bindConstant().annotatedWith(Names.named("constantsOnly")).to(
						commandLine.hasOption('c'));

				if (commandLine.hasOption('p')) {
					String prefix = commandLine.getOptionValue('p');
					if (prefix == null) {
						prefix = "";
					}
					bind(String.class).annotatedWith(
							Names.named("propertyNamesPrefix")).toInstance(
							prefix);
				}
				if (commandLine.hasOption('e')) {
					bind(new TypeLiteral<Collection<String>>() {
					}).annotatedWith(Names.named("baseClasses")).toInstance(
							Arrays.asList(commandLine.getOptionValues('e')));
				}

				bind(new TypeLiteral<Collection<URL>>() {
				}).annotatedWith(Names.named("jars")).toInstance(
						findJars(commandLine.getArgs(), 0));
				bind(new TypeLiteral<Collection<URL>>() {
				}).annotatedWith(Names.named("rdfSources")).toInstance(
						findRdfSources(commandLine.getArgs(), 0));

				Map<String, String> packages = new HashMap<String, String>();
				for (String value : commandLine.getOptionValues('b')) {
					String[] split = value.split("=", 2);
					if (split.length != 2) {
						throw new ParseException("Invalid bind option: "
								+ value);
					}

					packages.put(split[1], split[0]);
				}

				bind(new TypeLiteral<Map<String, String>>() {
				}).annotatedWith(Names.named("packages")).toInstance(packages);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}

		private Collection<URL> findRdfSources(String[] args, int offset)
				throws MalformedURLException {
			List<URL> sources = new ArrayList<URL>();

			for (int i = offset; i < args.length; i++) {
				URL url;
				File file = new File(args[i]);
				if (file.isDirectory() || args[i].endsWith(".jar"))
					continue;
				if (file.exists()) {
					url = file.toURI().toURL();
				} else {
					url = new URL(args[i]);
				}
				sources.add(url);
			}

			return sources;
		}

		private Collection<URL> findJars(String[] args, int offset)
				throws MalformedURLException {
			List<URL> jars = new ArrayList<URL>();

			for (int i = offset; i < args.length; i++) {
				URL url;
				File file = new File(args[i]);
				System.err.println("FILE: " + args[i]);
				if (file.exists()) {
					url = file.toURI().toURL();
				} else {
					try {
						url = new URL(args[i]);
					} catch (MalformedURLException mue) {
						continue;
					}
				}
				if (file.isDirectory() || args[i].endsWith(".jar")) {
					jars.add(url);
				}
			}

			return jars;
		}
	}

	private static final String META_INF_CONCEPTS = "META-INF/org.openrdf.concepts";

	private static final String META_INF_BEHAVIOURS = "META-INF/org.openrdf.behaviours";

	private static final String META_INF_DATATYPES = "META-INF/org.openrdf.datatypes";

	private static final String META_INF_ONTOLOGIES = "META-INF/org.openrdf.ontologies";

	private static final Options options = new Options();
	static {
		Option pkg = new Option("b", "bind", true,
				"Binds the package name and namespace together");
		pkg.setArgName("package=uri");
		pkg.setArgs(Option.UNLIMITED_VALUES);

		Option jar = new Option("j", "jar", true,
				"filename where the jar will be saved");
		jar.setArgName("jar file");

		Option dir = new Option("d", "directory", true,
				"directory where the files will be saved");
		Option metaInfDir = new Option(null, "meta-inf", true,
				"directory for meta information (usually META-INF)");

		Option file = new Option("r", "rdf", true,
				"filename where the rdf ontology will be saved");
		file.setArgName("RDF file");
		Option prefix = new Option("p", "prefix", true,
				"prefix the property names with namespace prefix");
		prefix.setArgName("prefix");
		prefix.setOptionalArg(true);
		Option constantsOnly = new Option("c", "constants-only", true,
				"generate only constants classes");
		constantsOnly.setArgName("prefix");
		constantsOnly.setOptionalArg(true);
		Option baseClass = new Option("e", "extends", true,
				"super class that all concepts should extend");
		baseClass.setArgName("full class name");
		options.addOption(baseClass);
		options.addOption(prefix);
		options.addOption(constantsOnly);
		options.addOption("h", "help", false, "print this message");
		options.addOption(pkg);
		options.addOption(jar);
		options.addOption(dir);
		options.addOption(metaInfDir);
		options.addOption(file);
	}

	public static void main(String[] args) throws Exception {
		try {
			CommandLine line = new PosixParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				String cmdLineSyntax = "codegen [options] [ontology | jar]...";
				String header = "[ontology | jar]... are a list of RDF and jar files that should be imported before converting.";
				formatter.printHelp(cmdLineSyntax, header, options, "");
				return;
			}
			if (!line.hasOption('b'))
				throw new ParseException("Required bind option missing");
			if (!(line.hasOption('j') || line.hasOption('d'))
					&& !line.hasOption('r'))
				throw new ParseException("Required jar or rdf option missing");
			if (line.hasOption('j') && line.hasOption('r'))
				throw new ParseException(
						"Only one jar or rdf option can be present");

			Injector injector = Guice.createInjector(Stage.PRODUCTION,
					new CommandLineModule(line));
			OntologyConverter converter = injector
					.getInstance(OntologyConverter.class);

			converter.init();
			if (line.hasOption('j')) {
				converter.createJar(new File(line.getOptionValue('j')));
			} else if (line.hasOption('d')) {
				String metaInfDir = line.getOptionValue("meta-inf");
				converter.createFiles(new File(line.getOptionValue('d')),
						metaInfDir != null ? new File(metaInfDir) : null);
			} else {
				converter.createOntology(new File(line.getOptionValue('r')));
			}
			return;
		} catch (ParseException exp) {
			System.err.println(exp.getMessage());
			System.exit(1);
		}
	}

	final Logger logger = LoggerFactory.getLogger(OntologyConverter.class);

	private boolean importJarOntologies = true;

	@Inject
	@Named("jars")
	private Collection<URL> jars = Collections.emptyList();

	@Inject
	@Named("rdfSources")
	private Collection<URL> rdfSources = Collections.emptyList();

	private Map<String, String> namespaces = new HashMap<String, String>();

	/** namespace -&gt; package */
	@Inject
	@Named("packages")
	private Map<String, String> packages;

	private Repository repository;

	private URLClassLoader cl;

	private String propertyNamesPrefix;

	private Collection<String> baseClasses;

	@Inject
	private Injector injector;

	@Inject
	@Named("constantsOnly")
	private boolean constantsOnly;

	/**
	 * If the ontologies bundled with the included jars should be imported.
	 * 
	 * @return <code>true</code> if the ontology will be imported.
	 */
	public boolean isImportJarOntologies() {
		return importJarOntologies;
	}

	/**
	 * If the ontologies bundled with the included jars should be imported.
	 * 
	 * @param importJarOntologies
	 *            <code>true</code> if the ontology will be imported.
	 */
	public void setImportJarOntologies(boolean importJarOntologies) {
		this.importJarOntologies = importJarOntologies;
	}

	/**
	 * The property names prefix or null for default prefix.
	 */
	public String getPropertyNamesPrefix() {
		return propertyNamesPrefix;
	}

	/**
	 * The property names prefix or null for default prefix.
	 */
	public void setPropertyNamesPrefix(
			@Named("propertyNamesPrefix") String propertyNamesPrefix) {
		this.propertyNamesPrefix = propertyNamesPrefix;
	}

	/**
	 * Array of Java Class names that all concepts will extend.
	 * 
	 * @return Array of Java Class names that all concepts will extend.
	 */
	public Collection<String> getBaseClasses() {
		return baseClasses;
	}

	/**
	 * Array of Java Class names that all concepts will extend.
	 * 
	 * @param strings
	 */
	public void setBaseClasses(
			@Named("baseClasses") Collection<String> baseClasses) {
		this.baseClasses = baseClasses;
	}

	/**
	 * Set the prefix that should be used for this ontology namespace.
	 * 
	 * @param prefix
	 * @param namespace
	 */
	public void setNamespace(String prefix, String namespace) {
		namespaces.put(prefix, namespace);
	}

	/**
	 * Create the local repository and load the RDF files.
	 * 
	 * @throws Exception
	 */
	public void init() throws Exception {
		cl = createClassLoader(jars);
		Thread.currentThread().setContextClassLoader(cl);
		repository = createRepository(cl);
		for (URL url : rdfSources) {
			loadOntology(repository, url);
		}
	}

	/**
	 * Generate an OWL ontology from the JavaBeans in the included jars.
	 * 
	 * @param rdfOutputFile
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addJar(URL)}
	 */
	public void createOntology(File rdfOutputFile) throws Exception {
		List<Class<?>> beans = new ArrayList<Class<?>>();
		if (packages.isEmpty()) {
			beans.addAll(findBeans(null, jars, cl));
		} else {
			for (String packageName : packages.values()) {
				beans.addAll(findBeans(packageName, jars, cl));
			}
		}
		ValueFactory valueFactory = new ValueFactoryImpl(
				repository.getURIFactory(), repository.getLiteralFactory());
		LiteralConverter literalConverter = new LiteralConverter();
		literalConverter.setClassLoader(cl);
		createOntology(beans, literalConverter, rdfOutputFile);
	}

	/**
	 * Generate concept Java classes from the ontology in the local repository.
	 * 
	 * @param jarOutputFile
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addRdfSource(URL)}
	 */
	public void createJar(File jarOutputFile) throws Exception {
		createFiles(repository, cl, jarOutputFile, null, true);
	}

	/**
	 * Generate concept Java classes from the ontology in the local repository.
	 * 
	 * @param directory
	 * @param metaInfDir
	 * @throws Exception
	 * @see {@link #addOntology(URI, String)}
	 * @see {@link #addRdfSource(URL)}
	 */
	public void createFiles(File directory, File metaInfDir) throws Exception {
		createFiles(repository, cl, directory, metaInfDir, false);
	}

	protected Repository createRepository() throws StoreException {
		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		return repository;
	}

	protected DecoratingSesameManagerFactory createSesameManagerFactory(
			Repository repository, URLClassLoader cl) {
		KommaModule module = new KommaModule(cl);
		module.addConcept(CodeClass.class);
		module.addConcept(CodeOntology.class);
		module.addConcept(CodeProperty.class);

		module.addBehaviour(ClassPropertySupport.class);
		module.addBehaviour(CodePropertySupport.class);
		module.addBehaviour(ConceptSupport.class);
		module.addBehaviour(OntologySupport.class);

		DecoratingSesameManagerFactory factory = new DecoratingSesameManagerFactory(
				module, repository);
		return factory;
	}

	private URLClassLoader createClassLoader(Collection<URL> importJars)
			throws MalformedURLException {
		Thread thread = Thread.currentThread();
		ClassLoader cl = thread.getContextClassLoader();
		String name = OntologyConverter.class.getName().replace('.', '/');
		if (cl == null || cl.getResource(name + ".class") == null) {
			cl = OntologyConverter.class.getClassLoader();
		}
		URL[] classpath = importJars.toArray(new URL[0]);
		if (cl instanceof URLClassLoader) {
			URL[] urls = ((URLClassLoader) cl).getURLs();
			URL[] jars = classpath;
			classpath = new URL[jars.length + urls.length];
			System.arraycopy(jars, 0, classpath, 0, jars.length);
			System.arraycopy(urls, 0, classpath, jars.length, urls.length);
		}
		return URLClassLoader.newInstance(classpath, cl);
	}

	private Repository createRepository(ClassLoader cl) throws StoreException,
			IOException, RDFParseException {
		Repository repository = createRepository();
		RepositoryConnection conn = repository.getConnection();
		try {
			for (Map.Entry<String, String> e : namespaces.entrySet()) {
				conn.setNamespace(e.getKey(), e.getValue());
			}
		} finally {
			conn.close();
		}
		if (importJarOntologies) {
			for (String owl : loadOntologyList(cl)) {
				URL url = cl.getResource(owl);
				loadOntology(repository, url);
			}
		}
		return repository;
	}

	@SuppressWarnings("unchecked")
	private Collection<String> loadOntologyList(ClassLoader cl)
			throws IOException {
		Properties ontologies = new Properties();
		String name = "META-INF/org.openrdf.ontologies";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			URL url = resources.nextElement();
			ontologies.load(url.openStream());
		}
		Collection<?> list = ontologies.keySet();
		return (Collection<String>) list;
	}

	private void loadOntology(Repository repository, URL url)
			throws StoreException, IOException, RDFParseException {
		String filename = url.toString();
		RDFFormat format = formatForFileName(filename);
		RepositoryConnection conn = repository.getConnection();
		URIFactory uriFactory = repository.getURIFactory();
		try {
			String uri = url.toExternalForm();
			conn.add(url, uri, format, uriFactory.createURI(uri));
		} catch (RDFParseException e) {
			throw new RDFParseException("Unable to parse file " + url + ": "
					+ e.getMessage(), e.getLineNumber(), e.getColumnNumber());
		} finally {
			conn.close();
		}
	}

	private RDFFormat formatForFileName(String filename) {
		RDFFormat format = RDFFormat.forFileName(filename);
		if (format != null)
			return format;
		if (filename.endsWith(".owl"))
			return RDFFormat.RDFXML;
		throw new IllegalArgumentException("Unknow RDF format for " + filename);
	}

	private List<Class<?>> findBeans(String pkgName, Collection<URL> urls,
			URLClassLoader cl) throws Exception {
		List<Class<?>> beans = new ArrayList<Class<?>>();
		for (URL jar : urls) {
			JarFile file = new JarFile(asLocalFile(jar));
			Enumeration<JarEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (name.contains("-") || !name.endsWith(".class"))
					continue;
				name = name.replace('/', '.').replace('\\', '.');
				if (pkgName == null || name.startsWith(pkgName)
						&& name.substring(pkgName.length() + 1).contains(".")) {
					name = name.replaceAll(".class$", "");
					beans.add(Class.forName(name, true, cl));
				}
			}
		}
		return beans;
	}

	private void createOntology(List<Class<?>> beans,
			LiteralConverter literalConverter, File output) throws Exception {
		RDFFormat format = formatForFileName(output.getName());
		Writer out = new FileWriter(output);
		try {
			RepositoryConnection conn = repository.getConnection();
			OwlGenerator gen = new OwlGenerator();
			gen.setLiteralConverter(literalConverter);
			for (Map.Entry<String, String> e : packages.entrySet()) {
				String namespace = e.getKey();
				String pkgName = e.getValue();
				String prefix = pkgName.substring(pkgName.lastIndexOf('.') + 1);
				conn.setNamespace(prefix, namespace);
				gen.setNamespace(pkgName, prefix, namespace);
			}
			RDFHandler inserter = new RDFInserter(conn);
			Set<URI> ontologies = gen.exportOntology(beans, inserter);
			OntologyWriter writer = new OntologyWriter(format, out);
			writer.setConnection(conn);
			writer.startRDF();
			for (URI ontology : ontologies) {
				writer.printOntology(ontology);
			}
			writer.endRDF();
			conn.close();
		} finally {
			out.close();
		}
	}

	private void createFiles(Repository repository, URLClassLoader cl,
			File output, File metaInfDir, boolean createJar) throws Exception {
		DecoratingSesameManagerFactory factory = createSesameManagerFactory(
				repository, cl);
		Collection<AbstractModule> modules = factory.createGuiceModules(null);
		modules.add(new AbstractModule() {
			@Override
			protected void configure() {
				Multibinder<IGenerator> generators = Multibinder.newSetBinder(
						binder(), IGenerator.class);
				if (!constantsOnly) {
					generators.addBinding().to(CodeGenerator.class)
							.in(Singleton.class);
				}
				generators.addBinding().to(ConstantsClassesGenerator.class)
						.in(Singleton.class);
			}

			@Provides
			@Named("baseClasses")
			@SuppressWarnings("unused")
			@Singleton
			Collection<Class<?>> provideBaseClasses(
					@Named("baseClasses") Collection<String> baseClasses,
					ClassLoader cl) throws Exception {
				List<Class<?>> base = new ArrayList<Class<?>>();
				if (baseClasses != null) {
					for (String bc : baseClasses) {
						base.add(Class.forName(bc, true, cl));
					}
				}
				return base;
			}

			@Provides
			@SuppressWarnings("unused")
			@Singleton
			JavaNameResolver provideNameResolver(Injector injector,
					OwlNormalizer normalizer, ClassLoader cl)
					throws StoreException {
				JavaNameResolverImpl resolver = new JavaNameResolverImpl(cl);
				injector.injectMembers(resolver);

				for (Map.Entry<String, String> e : namespaces.entrySet()) {
					resolver.bindPrefixToNamespace(e.getKey(), e.getValue());
				}
				if (propertyNamesPrefix != null) {
					for (Map.Entry<String, String> e : packages.entrySet()) {
						resolver.bindPrefixToNamespace(propertyNamesPrefix,
								e.getKey());
					}
				}
				for (URI uri : normalizer.getAnonymousClasses()) {
					String ns = uri.getNamespace();
					net.enilink.komma.core.URI name = URIImpl
							.createURI(ns).appendFragment(uri.getLocalName());
					resolver.assignAnonymous(name);
				}

				for (Map.Entry<URI, URI> e : normalizer.getAliases().entrySet()) {
					String ns1 = e.getKey().getNamespace();
					net.enilink.komma.core.URI name = URIImpl
							.createURI(ns1).appendFragment(
									e.getKey().getLocalName());
					String ns2 = e.getValue().getNamespace();
					net.enilink.komma.core.URI alias = URIImpl
							.createURI(ns2).appendFragment(
									e.getValue().getLocalName());
					resolver.assignAlias(name, alias);
				}

				return resolver;
			}

			@Provides
			@SuppressWarnings("unused")
			@Singleton
			OwlNormalizer provideNormalizer(Injector injector) throws Exception {
				OwlNormalizer normalizer = new OwlNormalizer();
				injector.injectMembers(normalizer);

				try {
					normalizer.normalize();
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}

				return normalizer;
			}
		});

		Injector genInjector = injector.createChildInjector(modules);

		FileSourceCodeHandler handler = createJar ? new FileSourceCodeHandler()
				: new FileSourceCodeHandler(output);

		Set<IGenerator> generators = genInjector
				.getInstance(new Key<Set<IGenerator>>() {
				});
		generateSourceCode(generators, handler);

		Set<String> concepts = new TreeSet<String>();
		Set<String> behaviours = new TreeSet<String>();
		concepts.addAll(handler.getAnnotatedClasses());
		behaviours.addAll(handler.getAbstractClasses());
		List<String> literals = handler.getConcreteClasses();
		concepts.removeAll(literals);

		if (!createJar) {
			if (metaInfDir != null) {
				// omit META-INF subdirectory since it will be added by
				// packageManifests
				if ("meta-inf".equals(metaInfDir.getName().toLowerCase())) {
					metaInfDir = metaInfDir.getParentFile();
				}
				packageManifests(metaInfDir, concepts, behaviours, literals);
			}
		} else {
			if (handler.getClasses().isEmpty())
				throw new IllegalArgumentException(
						"No classes found - Try a different namespace.");

			JavaCompiler javac = new JavaCompiler();
			List<File> classpath = getClassPath(cl);
			File dir = handler.getTarget();
			javac.compile(handler.getClasses(), dir, classpath);

			packageJar(output, dir, concepts, behaviours, literals);
		}
	}

	private List<File> getClassPath(URLClassLoader cl)
			throws UnsupportedEncodingException {
		List<File> classpath = new ArrayList<File>();
		for (URL jar : cl.getURLs()) {
			classpath.add(asLocalFile(jar));
		}
		String classPath = System.getProperty("java.class.path");
		for (String path : classPath.split(File.pathSeparator)) {
			classpath.add(new File(path));
		}
		return classpath;
	}

	private void generateSourceCode(Set<IGenerator> generators,
			FileSourceCodeHandler handler) throws Exception {
		for (IGenerator generator : generators) {
			generator.exportSourceCode(handler);
		}
	}

	private void packageManifests(File targetDir, Collection<String> concepts,
			Collection<String> behaviours, List<String> literals)
			throws Exception {
		printManifest(concepts, targetDir, META_INF_CONCEPTS);
		printManifest(behaviours, targetDir, META_INF_BEHAVIOURS);
		printManifest(literals, targetDir, META_INF_DATATYPES);
	}

	private void packageJar(File output, File dir, Collection<String> concepts,
			Collection<String> behaviours, List<String> literals)
			throws Exception {
		FileOutputStream stream = new FileOutputStream(output);
		JarOutputStream jar = new JarOutputStream(stream);
		try {
			packageFiles(dir, dir, jar);
			printManifest(concepts, jar, META_INF_CONCEPTS);
			printManifest(behaviours, jar, META_INF_BEHAVIOURS);
			printManifest(literals, jar, META_INF_DATATYPES);
			packOntologies(rdfSources, jar);
		} finally {
			jar.close();
			stream.close();
		}
	}

	private void packageFiles(File base, File dir, JarOutputStream jar)
			throws IOException, FileNotFoundException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				packageFiles(base, file, jar);
			} else if (file.exists()) {
				String path = file.getAbsolutePath();
				path = path.substring(base.getAbsolutePath().length() + 1);
				// replace separatorChar by '/' on all platforms
				if (File.separatorChar != '/') {
					path = path.replace(File.separatorChar, '/');
				}
				jar.putNextEntry(new JarEntry(path));
				copyInto(file.toURI().toURL(), jar);
				file.delete();
			}
		}
	}

	private void copyInto(URL source, OutputStream out)
			throws FileNotFoundException, IOException {
		logger.debug("Packaging {}", source);
		InputStream in = source.openStream();
		try {
			int read;
			byte[] buf = new byte[512];
			while ((read = in.read(buf)) > 0) {
				out.write(buf, 0, read);
			}
		} finally {
			in.close();
		}
	}

	private void printManifest(Collection<String> roles, File targetDir,
			String entry) throws IOException {
		File targetFile = new File(targetDir, entry);
		PrintStream out = null;
		for (String name : roles) {
			if (out == null) {
				out = new PrintStream(targetFile);
			}
			out.println(name);
		}
		if (out != null) {
			out.close();
		}
	}

	private void printManifest(Collection<String> roles, JarOutputStream jar,
			String entry) throws IOException {
		PrintStream out = null;
		for (String name : roles) {
			if (out == null) {
				jar.putNextEntry(new JarEntry(entry));
				out = new PrintStream(jar);
			}
			out.println(name);
		}
		if (out != null) {
			out.flush();
		}
	}

	private void packOntologies(Collection<URL> rdfSources, JarOutputStream jar)
			throws StoreException, RDFParseException, IOException {
		Map<String, URI> ontologies = new HashMap<String, URI>();
		for (URL rdf : rdfSources) {
			String path = "META-INF/ontologies/";
			path += asLocalFile(rdf).getName();
			URI ontology = findOntology(rdf);
			// package only ontologies with generated code
			if (ontology != null
					&& packages.containsKey(URIUtil
							.modelUriToNamespace(ontology.stringValue()))) {
				ontologies.put(path, ontology);
				jar.putNextEntry(new JarEntry(path));
				copyInto(rdf, jar);
			}
		}
		if (ontologies.isEmpty()) {
			return;
		}
		jar.putNextEntry(new JarEntry(META_INF_ONTOLOGIES));
		PrintStream out = new PrintStream(jar);
		for (Map.Entry<String, URI> e : ontologies.entrySet()) {
			out.print(e.getKey());
			out.print("\t=\t");
			out.println(e.getValue().toString());
		}
		out.flush();
	}

	private File asLocalFile(URL rdf) throws UnsupportedEncodingException {
		return new File(URLDecoder.decode(rdf.getFile(), "UTF-8"));
	}

	private URI findOntology(URL rdf) throws StoreException, RDFParseException,
			IOException {
		Repository repository = createRepository();
		URIFactory uriFactory = repository.getURIFactory();
		loadOntology(repository, rdf);
		RepositoryConnection conn = repository.getConnection();
		try {
			Statement st = first(conn, RDF.TYPE, OWL.ONTOLOGY);
			if (st != null)
				return (URI) st.getSubject();
			st = first(conn, RDFS.ISDEFINEDBY, null);
			if (st != null)
				return (URI) st.getObject();
			st = first(conn, RDF.TYPE, OWL.CLASS);
			if (st != null)
				return uriFactory.createURI(((URI) st.getSubject())
						.getNamespace());
			st = first(conn, RDF.TYPE, RDFS.CLASS);
			if (st != null)
				return uriFactory.createURI(((URI) st.getSubject())
						.getNamespace());
			return null;
		} finally {
			conn.clear();
			conn.close();
		}
	}

	private Statement first(RepositoryConnection conn, URI pred, Value obj)
			throws StoreException {
		Result<Statement> stmts = conn.match(null, pred, obj, true);
		try {
			if (stmts.hasNext())
				return stmts.next();
			return null;
		} finally {
			stmts.close();
		}
	}

	@Override
	public Object start(IApplicationContext context) throws Exception {
		return true;
	}

	@Override
	public void stop() {

	}

}
