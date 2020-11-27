/**
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   IBM Corporation - Initial API and implementation
 */
package net.enilink.komma.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import net.enilink.composition.annotations.Iri;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.em.concepts.IOntology;

/**
 * A named graph identified by a {@link URI} whose contents are accessed through
 * an {@link IEntityManager}.
 * 
 * <p>
 * The <code>Model</code> interface allows to work with multiple ontologies and
 * their imports closure. Models can be loaded from and saved to files in
 * multiple formats like Turtle, RDF/XML and others. If a model is loaded or
 * saved without specifying a concrete URL or stream then a set of rules is used
 * by the model set's {@link IURIConverter} to convert the model's URI into a
 * resolvable URL.
 * 
 * <p>
 * If an entity manager ({@link #getManager()}) is constructed for a model then
 * the model's <code>owl:imports</code> closure is traversed and the imported
 * ontologies are also loaded into the {@link IModelSet}. The entity manager has
 * read-write access to this model and read-only access to the statements contained
 * in the imported models.
 */
@Iri(MODELS.NAMESPACE + "Model")
public interface IModel {
	/**
	 * A save option that can be used only with {@link #save(Map)} to specify
	 * that the model is to be saved only if the new contents are different from
	 * actual contents; this compares the bytes in the backing store against the
	 * new bytes that would be saved. The value on this option can be either
	 * <code>null</code>, {@link #OPTION_SAVE_ONLY_IF_CHANGED_FILE_BUFFER}, or
	 * {@link #OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER}.
	 */
	String OPTION_SAVE_ONLY_IF_CHANGED = "SAVE_ONLY_IF_CHANGED";

	/**
	 * A value for {@link #OPTION_SAVE_ONLY_IF_CHANGED} to specify that an
	 * in-memory buffer should be used to compare the new contents with the
	 * actual contents. This will be faster than
	 * {@link #OPTION_SAVE_ONLY_IF_CHANGED_FILE_BUFFER} but will use up more
	 * memory.
	 */
	String OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER = "MEMORY_BUFFER";

	/**
	 * A value for {@link #OPTION_SAVE_ONLY_IF_CHANGED} to specify that a file
	 * buffer should be used to compare the new contents with the actual
	 * contents. This will be slower than
	 * {@link #OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER} but will use up less
	 * memory.
	 */
	String OPTION_SAVE_ONLY_IF_CHANGED_FILE_BUFFER = "FILE_BUFFER";

	/**
	 * A load and save option that can be used to specify a content description
	 * object.
	 */
	String OPTION_CONTENT_DESCRIPTION = "CONTENT_DESCRIPTION";

	/**
	 * A load and save option that can be used to specify the MIME type of the
	 * contents.
	 */
	String OPTION_MIME_TYPE = "MIME_TYPE";

	/**
	 * A load and save option that can be used to specify the character set
	 * (like UTF-8) of the contents.
	 */
	String OPTION_CHARSET = "CHARSET";

	/**
	 * Adds an import to the model
	 * 
	 * @param uri
	 * @param prefix
	 * @throws KommaException
	 */
	void addImport(URI uri, String prefix) throws KommaException;

	/**
	 * {@link IURIConverter#delete(URI, Map) deletes} the model resource using
	 * the specified options, {@link #unload() unloads} it, and then removes it
	 * from the {@link #getModelSet() containing} model set.
	 * <p>
	 * Options are handled generically as feature-to-setting entries; the model
	 * will ignore options it doesn't recognize. The options could even include
	 * things like an Eclipse progress monitor...
	 * </p>
	 * <p>
	 * An implementation typically uses the {@link IModelSet#getURIConverter URI
	 * converter} of the {@link #getModelSet() containing} resource set to
	 * {@link IURIConverter#delete(URI, Map)} the ontology's {@link #getURI()
	 * URI}.
	 * </p>
	 */
	void delete(Map<?, ?> options) throws IOException;

	/**
	 * Returns all directly imported models.
	 */
	Set<URI> getImports();

	/**
	 * Returns all imported models, transitive imports included.
	 */
	Set<URI> getImportsClosure();

	/**
	 * Returns the {@link IEntityManager} manager which is responsible for
	 * loading and modifying the ontology's contents
	 * 
	 * @return manager instance
	 */
	IEntityManager getManager();

	/**
	 * Returns the ontology resource which is managed by this ontology model
	 * 
	 * @return ontology resource
	 */
	IOntology getOntology();

	/**
	 * Returns the containing model set. A model is contained by a model set if
	 * it appears in the {@link IModelSet#getModels models}, i.e., the contents,
	 * of that model set. This reference can only be modified by altering the
	 * contents of the model set directly.
	 * 
	 * @return the containing model set, or <code>null</code> if there isn't
	 *         one.
	 * @see IModelSet#getModels
	 */
	IModelSet getModelSet();

	/**
	 * Returns the URI of this model. The URI is normally expected to be
	 * {@link URI#isRelative absolute} and {@link URI#isHierarchical
	 * hierarchical}; document-relative references will not be serialized and
	 * will not be {@link URI#resolve(URI) resolved}, if this is not the case.
	 * 
	 * @return the URI of this model, or <code>null</code> if there isn't one.
	 * @see #setURI(URI)
	 * @see URI#isRelative
	 * @see URI#isHierarchical
	 */
	URI getURI();

	/**
	 * Returns whether the model is loaded.
	 * <p>
	 * This will be <code>false</code> when the model is first
	 * {@link IModelSet#createModel(URI) created} and will be set to
	 * <code>false</code>, when the model is {@link #unload unloaded}. It will
	 * be set to <code>true</code> when the model is {@link #load(Map) loaded}.
	 * </p>
	 * 
	 * @return whether the model is loaded.
	 */
	boolean isLoaded();

	/**
	 * Sets whether this model has been loaded.
	 * <p>
	 * A model is set to be loaded after it is {@link #load(Map) loaded} and
	 * unset to be loaded after it is {@link #unload unloaded}.
	 * </p>
	 * 
	 * @param isLoaded
	 *            whether this model has been loaded.
	 * @see #isLoaded
	 */
	void setLoaded(boolean isLoaded);

	/**
	 * Returns whether this model has been modified.
	 * <p>
	 * A model is set to be unmodified after it is loaded or saved.
	 * </p>
	 * 
	 * @return whether this resource has been modified.
	 * @see #setModified(boolean)
	 */
	boolean isModified();

	/**
	 * Sets whether this model has been modified.
	 * <p>
	 * A model is set to be unmodified after it is loaded or saved.
	 * </p>
	 * 
	 * @param isModified
	 *            whether this model has been modified.
	 * @see #isModified
	 */
	void setModified(boolean isModified);

	/**
	 * Loads the model using the specified options.
	 * <p>
	 * Options are handled generically as feature-to-setting entries; the
	 * model will ignore options it doesn't recognize. The options could even
	 * include things like an Eclipse progress monitor...
	 * </p>
	 * <p>
	 * An implementation typically uses the {@link IModelSet#getURIConverter URI
	 * converter} of the {@link #getModelSet() containing} model set to
	 * {@link IURIConverter#createInputStream(URI, Map) create} an input stream,
	 * and then delegates to {@link #load(InputStream, Map) load(InputStream,
	 * Map)}.
	 * </p>
	 * <p>
	 * When the load completes, the {@link #getErrors errors} and
	 * {@link #getWarnings warnings} can be consulted. An implementation will
	 * typically deserialize as much of a document as possible while producing
	 * diagnostics for any problems that are encountered.
	 * </p>
	 * 
	 * @param options
	 *            the load options.
	 */
	void load(Map<?, ?> options) throws IOException;

	/**
	 * Resolve the given reference in this model
	 * 
	 * @param reference
	 *            Reference to an {@link IObject}
	 * @return the resolved {@link IObject}
	 */
	IObject resolve(IReference reference);

	/**
	 * Returns the resolved URI for the given local part.
	 * 
	 * @param localPart
	 *            the local part to resolve.
	 * @return the resolved URI for the given local part.
	 */
	URI resolveURI(String localPart);
	
	/**
	 * Loads the model from the URI using the specified options.
	 * 
	 * @see #load(Map)
	 * @see #load(InputStream)
	 * @param options
	 *            the load options.
	 */
	void load(URI uri, Map<?, ?> options) throws IOException;

	/**
	 * Loads the model from the input stream using the specified options.
	 * 
	 * @param inputStream
	 *            the stream
	 * @param options
	 *            the load options.
	 * @see #load(Map)
	 * @see #load(URI)
	 * @see #save(OutputStream, Map)
	 */
	void load(InputStream inputStream, Map<?, ?> options) throws IOException;

	/**
	 * Removes the import from this model
	 * 
	 * @param importedOnt
	 *            the imported model
	 */
	void removeImport(URI importedOnt);

	/**
	 * Saves the resource using the specified options.
	 * <p>
	 * Options are handled generically as feature-to-setting entries; the
	 * resource will ignore options it doesn't recognize. The options could even
	 * include things like an Eclipse progress monitor...
	 * </p>
	 * <p>
	 * An implementation typically uses the {@link IModelSet#getURIConverter URI
	 * converter} of the {@link #getOntologySet containing} model set to
	 * {@link UIRIConverter#createOutputStream(URI, Map) create} an output
	 * stream, and then delegates to {@link #save(OutputStream, Map)
	 * save(OutputStream, Map)}.
	 * </p>
	 * 
	 * @param options
	 *            the save options.
	 * @see #save(OutputStream, Map)
	 */
	void save(Map<?, ?> options) throws IOException;

	/**
	 * Saves the model to the output stream using the specified options.
	 * <p>
	 * Usually, {@link #save(Map) save(Map)} is called directly and it calls
	 * this.
	 * </p>
	 * 
	 * @param outputStream
	 *            the stream
	 * @param options
	 *            the save options.
	 * @see #save(Map)
	 * @see #load(InputStream, Map)
	 */
	void save(OutputStream outputStream, Map<?, ?> options) throws IOException;

	/**
	 * Sets the URI of this resource.
	 * 
	 * @param uri
	 *            the new URI.
	 * @see #getURI
	 */
	void setURI(URI uri);

	/**
	 * Clears the {@link #getErrors errors}, and {@link #getWarnings warnings}
	 * of the model and {@link #isLoaded marks} it as unloaded.
	 */
	void unload();

	/**
	 * Unloads the manager and internal module.
	 */
	void unloadManager();

	/**
	 * Returns a list of the errors in the model; each error will be of type
	 * {@link IDiagnostic}.
	 * <p>
	 * These will typically be produced as the model is {@link #load(Map)
	 * loaded}.
	 * </p>
	 * 
	 * @return a list of the errors in the resource.
	 * @see #load(Map)
	 */
	Set<IDiagnostic> getErrors();

	/**
	 * Returns a list of the warnings and informational messages in the model;
	 * each warning will be of type {@link IDiagnostic}.
	 * <p>
	 * These will typically be produced as the model is {@link #load(Map)
	 * loaded}.
	 * </p>
	 * 
	 * @return a list of the warnings in the resource.
	 * @see #load(Map)
	 */
	Set<IDiagnostic> getWarnings();

	/**
	 * A factory for creating resources.
	 * <p>
	 * A factory is implemented to {@link #createResource create} a specialized
	 * type of resource and is typically registered in
	 * {@link IModel.Factory.Registry registry}.
	 * </p>
	 * 
	 * @see IModelSet#createModel(URI)
	 */
	interface Factory {
		/**
		 * Creates an model with the given URI and returns it.
		 * <p>
		 * Clients will typically not call this directly themselves; it's called
		 * by the model set to {@link IModelSet#createModel(URI) create} a
		 * model.
		 * </p>
		 * 
		 * @param modelSet
		 *            the containing model set.
		 * @param uri
		 *            the URI.
		 * @return a new model.
		 * @see IModelSet#createModel(URI)
		 */
		IModel createModel(IModelSet modelSet, URI uri);

		/**
		 * A descriptor used by a model factory registry to defer factory
		 * creation.
		 * <p>
		 * The creation is deferred until the factory is
		 * {@link IModel.Factory.Registry#getFactory(URI) fetched} for the first
		 * time.
		 * </p>
		 * 
		 * @see IModel.Factory.Registry#getFactory(URI)
		 */
		interface IDescriptor {
			/**
			 * Creates a factory and returns it.
			 * <p>
			 * An implementation may and usually does choose to create only one
			 * instance, which it returns for each call.
			 * </p>
			 * 
			 * @return a factory.
			 */
			Factory createFactory();
		}

		/**
		 * A registry of model factories.
		 * <p>
		 * A {@link IModel.Factory.IDescriptor descriptor} can be used in place
		 * of an actual {@link IModel.Factory factory} as a value in the map.
		 * </p>
		 * 
		 * @see IModelSet#getModelFactoryRegistry()
		 */
		interface Registry {
			/**
			 * Returns the resource factory appropriate for the given URI.
			 * <p>
			 * An implementation will (typically) use the URI's
			 * {@link URI#scheme scheme} to search the
			 * {@link #getProtocolToFactoryMap protocol} map the URI's
			 * {@link URI#fileExtension file extension} to search
			 * {@link #getExtensionToFactoryMap extension} map, and the URI's
			 * {@link URIConverter#contentDescription(URI, Map) content type
			 * identifier} to search the {@link #getContentTypeToFactoryMap()
			 * content type} map. It will
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Descriptor#createFactory
			 * convert} a resulting descriptor into a factory. It may choose to
			 * provide additional mechanisms and algorithms to determine a
			 * factory appropriate for the given URI.
			 * </p>
			 * 
			 * @param uri
			 *            the URI.
			 * @return the resource factory appropriate for the given URI, or
			 *         <code>null</code> if there isn't one.
			 * @see ResourceSet#createModel(URI)
			 */
			Factory getFactory(URI uri);

			/**
			 * Returns the resource factory appropriate for the given URI with
			 * the given {@link URIConverter#contentDescription(URI, Map)
			 * content type} identifier.
			 * <p>
			 * An implementation will (typically) use the URI's
			 * {@link URI#scheme scheme} to search the
			 * {@link #getProtocolToFactoryMap protocol} map the URI's
			 * {@link URI#fileExtension file extension} to search
			 * {@link #getExtensionToFactoryMap extension} map, and the given
			 * content type identifier to search the
			 * {@link #getContentTypeToFactoryMap() content type} map. It will
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Descriptor#createFactory
			 * convert} a resulting descriptor into a factory. It may choose to
			 * provide additional mechanisms and algorithms to determine a
			 * factory appropriate for the given URI.
			 * </p>
			 * 
			 * @param uri
			 *            the URI.
			 * @param contentType
			 *            the content type of the URI or <code>null</code> if a
			 *            content type should not be used during lookup.
			 * @return the resource factory appropriate for the given URI with
			 *         the content content type, or <code>null</code> if there
			 *         isn't one.
			 * @see IModelSet#createModel(URI)
			 */
			Factory getFactory(URI uri, String contentType);

			/**
			 * Returns a map from {@link URI#scheme protocol} to
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory}
			 * or
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Descriptor}
			 * .
			 * 
			 * @return the protocol map.
			 */
			Map<String, Object> getProtocolToFactoryMap();

			/**
			 * The file extension <code>"*"</code> that matches any extension.
			 * 
			 * @see #getExtensionToFactoryMap
			 */
			String DEFAULT_EXTENSION = "*";

			/**
			 * Returns a map from {@link URI#fileExtension file extension} to
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory}
			 * or
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Descriptor}
			 * .
			 * <p>
			 * The {@link #DEFAULT_EXTENSION default} file extension
			 * <code>"*"</code> can be registered as a default that matches any
			 * file extension. This is typically reserved for a default factory
			 * that supports XMI serialization; clients are strongly discouraged
			 * from using this feature in the global registry, particularly
			 * those that must function effectively within an Eclipse
			 * environment.
			 * </p>
			 * 
			 * @return the file extension map.
			 * @see #DEFAULT_EXTENSION
			 */
			Map<String, Object> getExtensionToFactoryMap();

			/**
			 * The content type identifier <code>"*"</code> that matches any
			 * content type identifier.
			 * 
			 * @see #getContentTypeToFactoryMap()
			 */
			String DEFAULT_CONTENT_TYPE_IDENTIFIER = "*";

			/**
			 * Returns a map from content type identifier to
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory}
			 * or
			 * {@link org.eclipse.ModelSetFactory.ecore.resource.Resource.Factory.Descriptor}
			 * .
			 * <p>
			 * The {@link #DEFAULT_CONTENT_TYPE_IDENTIFIER default} content type
			 * identifier <code>"*"</code> can be registered as a default that
			 * matches any content type identifier. This is typically reserved
			 * for a default factory that supports XMI serialization; clients
			 * are strongly discouraged from using this feature in the global
			 * registry, particularly those that must function effectively
			 * within an Eclipse environment.
			 * </p>
			 * 
			 * @return the content type identifier map.
			 * @see #DEFAULT_CONTENT_TYPE_IDENTIFIER
			 */
			Map<String, Object> getContentTypeToFactoryMap();
		}
	}

	/**
	 * A noteworthy issue in a document.
	 */
	@Iri("http://enilink.net/vocab/komma/models#Diagnostic")
	interface IDiagnostic {
		/**
		 * Returns a translated message describing the issue.
		 * 
		 * @return a translated message.
		 */
		String getMessage();

		/**
		 * Returns the source location of the issue. This will typically be just
		 * the {@link IModel#getURI URI} of the ontology containing this
		 * diagnostic.
		 * 
		 * @return the location of the issue, or <code>null</code> if it's
		 *         unknown.
		 */
		String getLocation();

		/**
		 * Returns the line location of the issue within the source. Line
		 * <code>1</code> is the first line.
		 * 
		 * @return the line location of the issue.
		 */
		int getLine();

		/**
		 * Returns the column location of the issue within the source. Column
		 * <code>1</code> is the first column.
		 * 
		 * @return the column location of the issue.
		 */
		int getColumn();
	}

	/**
	 * An internal interface implemented by all models.
	 * 
	 * @see IModel#getModelSet
	 * @see IModelSet#getModels
	 */
	interface Internal extends IModel {
		/**
		 * Allows to avoid demand-loading of an imported model, e.g. for
		 * security reasons.
		 * 
		 * @param imported
		 *            The imported model
		 * @return <code>true</code> if demand-loading should be used, else
		 *         <code>false</code>.
		 */
		boolean demandLoadImport(URI imported);

		/**
		 * Returns a module for this model.
		 */
		KommaModule getModule();

		/**
		 * Returns a module that includes all imported model modules.
		 */
		KommaModule getModuleClosure();

		/**
		 * Indicates whether the model is currently being loaded.
		 * <p>
		 * This will be <code>true</code> during a call to
		 * {@link #load(InputStream, Map) load(InputStream, Map)}, before
		 * notifications are dispatched.
		 * </p>
		 * 
		 * @return whether this model is currently being loaded.
		 */
		boolean isLoading();
	}

	/**
	 * An IO exception that wraps another exception.
	 * <p>
	 * Since save and load throw an IO Exception, it may be convenient for an
	 * implementation to wrap another exception in order to throw it as an IO
	 * exception.
	 * </p>
	 */
	class IOWrappedException extends IOException {
		static final long serialVersionUID = 1L;

		/**
		 * Creates an instance which wraps the given exception.
		 * 
		 * @param exception
		 *            the exception to wrap.
		 */
		public IOWrappedException(Exception exception) {
			super(exception.getLocalizedMessage());
			initCause(exception);
		}

		/**
		 * Creates an instance which wraps the given exception.
		 * 
		 * @param throwable
		 *            the exception to wrap.
		 */
		public IOWrappedException(Throwable throwable) {
			super(throwable.getLocalizedMessage());
			initCause(throwable);
		}
	}
}
