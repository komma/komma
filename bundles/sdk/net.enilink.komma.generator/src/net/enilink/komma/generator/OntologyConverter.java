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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
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

import net.enilink.composition.mappers.ComposedRoleMapper;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import net.enilink.komma.core.IDialect;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IUnitOfWork;
import net.enilink.komma.core.InferencingCapability;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.KommaModule.Association;
import net.enilink.komma.core.SparqlStandardDialect;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.dm.IDataManager;
import net.enilink.komma.dm.IDataManagerFactory;
import net.enilink.komma.dm.change.DataChangeTracker;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.dm.change.IDataChangeTracker;
import net.enilink.komma.em.EagerCachingEntityManagerModule;
import net.enilink.komma.em.ManagerCompositionModule;
import net.enilink.komma.em.util.KommaUtil;
import net.enilink.komma.em.util.RoleClassLoader;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.komma.generator.concepts.CodeClass;
import net.enilink.komma.generator.concepts.CodeOntology;
import net.enilink.komma.generator.concepts.CodeProperty;
import net.enilink.komma.generator.support.ClassPropertySupport;
import net.enilink.komma.generator.support.CodePropertySupport;
import net.enilink.komma.generator.support.ConceptSupport;
import net.enilink.komma.generator.support.OntologySupport;
import net.enilink.komma.literals.LiteralConverter;
import net.enilink.komma.rdf4j.RDF4JModule;

/**
 * A Facade to CodeGenerator and OwlGenerator classes. This class provides a
 * simpler interface to create concept packages and build ontologies. Unlike the
 * composed classes, this class reads and creates jar packages.
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

				List<String> baseClasses = Collections.emptyList();
				if (commandLine.hasOption('e')) {
					baseClasses = Arrays.asList(commandLine
							.getOptionValues('e'));
				}
				bind(new TypeLiteral<Collection<String>>() {
				}).annotatedWith(Names.named("baseClasses")).toInstance(
						baseClasses);

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
		createOntology(beans, rdfOutputFile);
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
		createFiles(jarOutputFile, null, true);
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
		createFiles(directory, metaInfDir, false);
	}

	protected Repository createRepository() throws Exception {
		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		return repository;
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

	private Repository createRepository(ClassLoader cl) throws Exception,
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
			throws RepositoryException, IOException, RDFParseException {
		String filename = url.toString();
		RDFFormat format = formatForFileName(filename);
		RepositoryConnection conn = repository.getConnection();
		ValueFactory uriFactory = repository.getValueFactory();
		try {
			String uri = url.toExternalForm();
			conn.add(url, uri, format, uriFactory.createIRI(uri));
		} catch (RDFParseException e) {
			throw new RDFParseException("Unable to parse file " + url + ": "
					+ e.getMessage(), e.getLineNumber(), e.getColumnNumber());
		} finally {
			conn.close();
		}
	}

	private RDFFormat formatForFileName(String filename) {
		Optional<RDFFormat> format = Rio.getParserFormatForFileName(filename);
		if (format.isPresent())
			return format.get();
		if (filename.endsWith(".owl"))
			return RDFFormat.RDFXML;
		throw new IllegalArgumentException("Unknow RDF format for " + filename);
	}

	private List<Class<?>> findBeans(String pkgName, Collection<URL> urls,
			URLClassLoader cl) throws Exception {
		List<Class<?>> beans = new ArrayList<Class<?>>();
		for (URL jar : urls) {
			try (JarFile file = new JarFile(asLocalFile(jar))) {
				Enumeration<JarEntry> entries = file.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.contains("-") || !name.endsWith(".class"))
						continue;
					name = name.replace('/', '.').replace('\\', '.');
					if (pkgName == null
							|| name.startsWith(pkgName)
							&& name.substring(pkgName.length() + 1).contains(
									".")) {
						name = name.replaceAll(".class$", "");
						beans.add(Class.forName(name, true, cl));
					}
				}
			}
		}
		return beans;
	}

	private void createOntology(List<Class<?>> beans, File output)
			throws Exception {
		Injector genInjector = createGeneratorInjector();

		RDFFormat format = formatForFileName(output.getName());
		Writer out = new FileWriter(output);
		try {
			IDataManagerFactory dmFactory = genInjector
					.getInstance(IDataManagerFactory.class);
			IDataManager dm = dmFactory.get();
			OwlGenerator gen = genInjector.getInstance(OwlGenerator.class);
			for (Map.Entry<String, String> e : packages.entrySet()) {
				String namespace = e.getKey();
				String pkgName = e.getValue();
				String prefix = pkgName.substring(pkgName.lastIndexOf('.') + 1);
				dm.setNamespace(prefix, URIs.createURI(namespace));
				gen.setNamespace(pkgName, prefix, namespace);
			}
			Set<URI> ontologies = gen.exportOntology(beans,
					new IDataVisitor<Void>() {
						List<IStatement> stmts = new ArrayList<IStatement>();

						@Override
						public Void visitBegin() {
							return null;
						}

						@Override
						public Void visitEnd() {
							return null;
						}

						@Override
						public Void visitStatement(IStatement stmt) {
							stmts.add(stmt);
							return null;
						}
					});
			dm.close();

			OntologyWriter writer = new OntologyWriter(format, out);
			genInjector.injectMembers(writer);
			writer.visitBegin();
			for (URI ontology : ontologies) {
				writer.printOntology(ontology);
			}
			writer.visitEnd();
			dmFactory.close();
		} finally {
			out.close();
		}

		genInjector.getInstance(IUnitOfWork.class).end();
		genInjector.getInstance(IEntityManagerFactory.class).close();
	}

	protected KommaModule createKommaModule(URLClassLoader cl) {
		KommaModule module = new KommaModule(cl);
		module.includeModule(KommaUtil.getCoreModule());
		module.addConcept(CodeClass.class);
		module.addConcept(CodeOntology.class);
		module.addConcept(CodeProperty.class);

		module.addBehaviour(ClassPropertySupport.class);
		module.addBehaviour(CodePropertySupport.class);
		module.addBehaviour(ConceptSupport.class);
		module.addBehaviour(OntologySupport.class);

		return module;
	}

	protected Injector createGeneratorInjector() {
		final KommaModule kommaModule = createKommaModule(cl);

		Injector factoryInjector = injector.createChildInjector(
				new AbstractModule() {
					@Override
					protected void configure() {
						UnitOfWork uow = new UnitOfWork();
						uow.begin();

						bind(UnitOfWork.class).toInstance(uow);
						bind(IUnitOfWork.class).toInstance(uow);

						bind(Locale.class).toInstance(Locale.getDefault());
						bind(IDataManager.class).toProvider(
								IDataManagerFactory.class);

						bind(DataChangeTracker.class).in(Singleton.class);
						bind(IDataChangeSupport.class).to(
								DataChangeTracker.class);
						bind(IDataChangeTracker.class).to(
								DataChangeTracker.class);

						bind(IDialect.class).to(SparqlStandardDialect.class);
						bind(InferencingCapability.class).toInstance(
								InferencingCapability.NONE);
					}

					@Singleton
					@Provides
					Repository provideRepository() {
						return repository;
					}
				}, new RDF4JModule());

		return factoryInjector.createChildInjector(
				new ManagerCompositionModule(kommaModule),
				new EagerCachingEntityManagerModule(), new AbstractModule() {
					@Override
					protected void configure() {
						Multibinder.newSetBinder(binder(),
								IEntityDecorator.class);

						Multibinder<IGenerator> generators = Multibinder
								.newSetBinder(binder(), IGenerator.class);
						if (!constantsOnly) {
							generators.addBinding().to(CodeGenerator.class)
									.in(Singleton.class);
						}
						generators.addBinding()
								.to(ConstantsClassesGenerator.class)
								.in(Singleton.class);

						// bind a simple mock factory
						bind(IEntityManagerFactory.class).to(
								SimpleEMFactory.class).in(Singleton.class);
					}

					@Provides
					@Singleton
					IEntityManager provideManager(
							@Named("unmanaged") IEntityManager em) {
						return em;
					}

					@Provides
					@Singleton
					@Named("baseClasses")
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
					@Singleton
					JavaNameResolver provideNameResolver(Injector injector,
							OwlNormalizer normalizer, ClassLoader cl,
							RoleMapper<URI> roleMapper, LiteralConverter lc,
							TypeFactory<URI> typeFactory) throws Exception {
						JavaNameResolverImpl resolver = new JavaNameResolverImpl(
								cl);
						injector.injectMembers(resolver);

						// register extra concepts and datatypes that should be
						// accessible by the name resolver
						ComposedRoleMapper<URI> roleMapperWithExtraConcepts = new ComposedRoleMapper<URI>(
								typeFactory);
						roleMapperWithExtraConcepts.addRoleMapper(roleMapper);
						resolver.setRoleMapper(roleMapperWithExtraConcepts);

						KommaModule extraModule = new KommaModule(cl);
						RoleClassLoader rcl = new RoleClassLoader(extraModule);
						rcl.load();
						for (Association concept : extraModule.getConcepts()) {
							roleMapperWithExtraConcepts.addConcept(
									concept.getJavaClass(),
									concept.getRdfType() != null ? typeFactory
											.createType(concept.getRdfType())
											: null);
						}

						LiteralConverter lcWithExtraTypes = lc.clone();
						for (Association datatype : extraModule.getDatatypes()) {
							lcWithExtraTypes.addDatatype(
									datatype.getJavaClass(),
									datatype.getRdfType() != null ? typeFactory
											.createType(datatype.getRdfType())
											: null);
						}

						// initialize name resolver with known prefixes
						for (Map.Entry<String, String> e : namespaces
								.entrySet()) {
							resolver.bindPrefixToNamespace(e.getKey(),
									e.getValue());
						}
						if (propertyNamesPrefix != null) {
							for (Map.Entry<String, String> e : packages
									.entrySet()) {
								resolver.bindPrefixToNamespace(
										propertyNamesPrefix, e.getKey());
							}
						}
						for (IReference clazz : normalizer
								.getAnonymousClasses()) {
							resolver.assignAnonymous(clazz.getURI());
						}

						for (Map.Entry<URI, URI> e : normalizer.getAliases()
								.entrySet()) {
							resolver.assignAlias(e.getKey(), e.getValue());
						}

						return resolver;
					}

					@Provides
					@Singleton
					OwlNormalizer provideNormalizer(Injector injector)
							throws Exception {
						OwlNormalizer normalizer = new OwlNormalizer();
						injector.injectMembers(normalizer);
						normalizer.normalize();
						return normalizer;
					}
				});
	}

	private void createFiles(File output, File metaInfDir, boolean createJar)
			throws Exception {
		Injector genInjector = createGeneratorInjector();

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

		genInjector.getInstance(IUnitOfWork.class).end();
		genInjector.getInstance(IEntityManagerFactory.class).close();
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
			throws RepositoryException, RDFParseException, IOException {
		Map<String, URI> ontologies = new HashMap<String, URI>();
		for (URL rdf : rdfSources) {
			String path = "META-INF/ontologies/";
			path += asLocalFile(rdf).getName();
			URI ontology = findOntology(rdf);
			// package only ontologies with generated code
			if (ontology != null && packages.containsKey(ontology.namespace())) {
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

	private URI findOntology(URL rdf) throws RepositoryException,
			RDFParseException, IOException {
		Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();
		loadOntology(repository, rdf);
		RepositoryConnection conn = repository.getConnection();
		try {
			Statement st = first(conn, RDF.TYPE, OWL.ONTOLOGY);
			if (st != null)
				return URIs.createURI(st.getSubject().stringValue());
			st = first(conn, RDFS.ISDEFINEDBY, null);
			if (st != null)
				return URIs.createURI(st.getObject().stringValue());
			st = first(conn, RDF.TYPE, OWL.CLASS);
			if (st != null)
				return URIs.createURI(((IRI) st.getSubject())
						.getNamespace());
			st = first(conn, RDF.TYPE, RDFS.CLASS);
			if (st != null)
				return URIs.createURI(((IRI) st.getSubject())
						.getNamespace());
			return null;
		} finally {
			conn.clear();
			conn.close();
		}
	}

	private Statement first(RepositoryConnection conn,
			IRI pred, Value obj) throws RepositoryException {
		RepositoryResult<Statement> stmts = conn.getStatements(null, pred, obj,
				true);
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
