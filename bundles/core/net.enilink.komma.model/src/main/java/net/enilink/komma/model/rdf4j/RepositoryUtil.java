package net.enilink.komma.model.rdf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.em.KommaEM;

/**
 * Contains common methods for initializing RDF repositories. 
 */
class RepositoryUtil {
	/**
	 * Adds "basic knowledge" like the RDF, RDFS and OWL vocabulary to an RDF4J repository.
	 * 
	 * @param repository The repository
	 * @param targetGraph URI of the target graph
	 * @param addRdfAndRdfsVocabulary <code>true</code> if RDF and RDFS vocabulary should be added, else <code>false</code>
	 */
	static void addBasicKnowledge(Repository repository, URI targetGraph, boolean addRdfAndRdfsVocabulary) {
		if (AbstractKommaPlugin.IS_OSGI_RUNNING) {
			Set<String> bundleNames = new HashSet<>(Arrays.asList("net.enilink.vocab.owl", "net.enilink.vocab.rdfs"));
			List<Bundle> bundles = Stream
					.of(FrameworkUtil.getBundle(RepositoryUtil.class).getBundleContext().getBundles())
					.filter(b -> bundleNames.contains(b.getSymbolicName())).collect(Collectors.toList());

			RepositoryConnection conn = null;
			try {
				conn = repository.getConnection();
				for (Bundle bundle : bundles) {
					URL url = bundle.getResource("META-INF/org.openrdf.ontologies");
					if (url != null) {
						URL resolvedUrl = FileLocator.resolve(url);

						Properties properties = new Properties();
						InputStream in = resolvedUrl.openStream();
						properties.load(in);
						in.close();

						URI baseUri = URIs.createURI(url.toString()).trimSegments(1);
						for (Map.Entry<Object, Object> entry : properties.entrySet()) {
							String file = entry.getKey().toString();
							if (!addRdfAndRdfsVocabulary && file.contains("rdfs")) {
								// skip RDF and RDFS schema
								continue;
							}

							URI fileUri = URIs.createFileURI(file);
							fileUri = fileUri.resolve(baseUri);

							resolvedUrl = FileLocator.resolve(new URL(fileUri.toString()));
							if (resolvedUrl != null) {
								in = resolvedUrl.openStream();
								if (in != null && in.available() > 0) {
									URI defaultGraph = targetGraph;
									Resource[] contexts = defaultGraph == null ? new Resource[0]
											: new Resource[] {
													repository.getValueFactory().createIRI(defaultGraph.toString()) };
									conn.add(in, "", RDFFormat.RDFXML, contexts);
								}
								if (in != null) {
									in.close();
								}
							}
						}
					}
				}
			} catch (IOException e) {
				throw new KommaException("Cannot access RDF data", e);
			} catch (RepositoryException e) {
				throw new KommaException("Loading RDF failed", e);
			} catch (RDFParseException e) {
				throw new KommaException("Invalid RDF data", e);
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (RepositoryException e) {
						KommaEM.INSTANCE.log(e);
					}
				}
			}
		}
	}
}
