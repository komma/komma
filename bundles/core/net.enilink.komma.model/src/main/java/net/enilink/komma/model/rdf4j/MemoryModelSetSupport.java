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
package net.enilink.komma.model.rdf4j;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import net.enilink.composition.properties.annotations.Transient;
import net.enilink.komma.core.*;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.traits.Behaviour;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.MODELS;
import net.enilink.komma.rdf4j.RDF4JModule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

@Iri(MODELS.NAMESPACE + "MemoryModelSet")
public abstract class MemoryModelSetSupport implements IModelSet,
		IModelSet.Internal, IRepositoryModelSet, Behaviour<IRepositoryModelSet> {
	@Transient
	@Iri(MODELS.NAMESPACE + "dataDir")
	public abstract IValue getDataDir();

	protected static URI resolveWorkspaceURI(URI uri) {
		if ("workspace".equals(uri.scheme())) {
			try {
				String instanceFilter = "(type=osgi.instance.area)";
				BundleContext context = FrameworkUtil.getBundle(MemoryModelSetSupport.class).getBundleContext();
				ServiceReference<?>[] refs = context
						.getServiceReferences("org.eclipse.osgi.service.datalocation.Location", instanceFilter);
				if (refs.length > 0) {
					Object location = context.getService(refs[0]);
					URL loc = (URL) location.getClass().getMethod("getURL").invoke(location);
					URI workspace = URIs.createURI(FileLocator.resolve(loc).toString());
					if ("".equals(workspace.lastSegment())) {
						workspace = workspace.trimSegments(1);
					}
					uri = workspace.appendSegments(uri.segments());
				}
			} catch (Exception e) {
				throw new RepositoryException(e);
			}
		} else {
			throw new RepositoryException("Location service for workspace scheme not found");
		}
		return uri;
	}

	protected String getDataDirPath() {
		IValue dataDir = getDataDir();
		if (dataDir == null) {
			return null;
		}
		if (dataDir instanceof IReference && ((IReference) dataDir).getURI() != null) {
			return resolveWorkspaceURI(((IReference) dataDir).getURI()).toFileString();
		} else {
			return dataDir instanceof ILiteral ? ((ILiteral) dataDir).getLabel() : dataDir.toString();
		}
	}

	public Repository createRepository(IGraph config) throws RepositoryException {
		NotifyingSail store = new MemoryStore();
		if (Boolean.TRUE.equals(getInference())) {
			store = new SchemaCachingRDFSInferencer(store);
		}
		SailRepository repository = new SailRepository(store);
		String dataDirPath = getDataDirPath();
		repository.setDataDir(dataDirPath != null ? new File(dataDirPath) : null);
		repository.init();
		addBasicKnowledge(repository);
		return repository;
	}

	protected void addBasicKnowledge(Repository repository)
			throws RepositoryException {
		RepositoryUtil.addBasicKnowledge(repository, getDefaultGraph(), importRdfAndRdfsVocabulary());
	}

	protected boolean importRdfAndRdfsVocabulary() {
		return true;
	}

	@Transient
	@Iri(MODELS.NAMESPACE + "inference")
	public abstract Boolean getInference();

	@Override
	public void collectInjectionModules(Collection<Module> modules, IGraph config) {
		modules.add(new RDF4JModule());
		modules.add(new AbstractModule() {
			@Singleton
			@Provides
			Repository provideRepository() {
				try {
					return getBehaviourDelegate().createRepository(config);
				} catch (RepositoryException e) {
					throw new KommaException("Unable to create repository.", e);
				}
			}
		});
	}

	@Override
	public boolean isPersistent() {
		return false;
	}
}
