/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $$RCSfile: WorkbenchURIConverterImpl.java,v $$
 *  $$Revision: 1.6 $$  $$Date: 2006/05/17 20:13:45 $$ 
 */
package net.enilink.komma.workbench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.base.ExtensibleURIConverter;
import net.enilink.komma.model.base.IURIMapRule;
import net.enilink.komma.model.base.SimpleURIMapRule;
import net.enilink.komma.workbench.internal.KommaWorkbenchPlugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * A default implementation of the WorkbenchURIConverter interface.
 * 
 * @since 1.0.0
 */
public class WorkbenchURIConverterImpl extends ExtensibleURIConverter implements
		IWorkbenchURIConverter, IResourceChangeListener, AutoCloseable {
	protected List<IContainer> inputContainers = new ArrayList<>();

	protected Map<IPath, IURIMapRule> ruleMap = new HashMap<>();

	protected Set<String> supportedExtensions = new HashSet<>(Arrays.asList(
			"owl", "rdf", "n3", "ttl", "nt"));

	protected Map<IProject, List<IProject>> projectDependencies = new HashMap<>();
	protected Map<IProject, Integer> trackedProjects = new HashMap<>();

	/**
	 * Default converter constructor, no containers.
	 */
	public WorkbenchURIConverterImpl() {
	}

	/**
	 * Construct with an input container.
	 * 
	 * @param inputContainer
	 */
	public WorkbenchURIConverterImpl(IContainer inputContainer) {
		addInputContainer(inputContainer);
	}

	protected void addRules(Collection<? extends IContainer> containers) {
		try {
			for (IContainer container : containers) {
				container.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource)
							throws CoreException {
						if (resource.getType() == IResource.FILE) {
							addRule((IFile) resource);
						}
						return true;
					}
				});
			}
		} catch (CoreException e) {
			KommaWorkbenchPlugin.INSTANCE.log(e);
		}
	}

	protected void updateDependencies(IProject project,
			List<IProject> dependencies) throws CoreException {
		List<IProject> last = projectDependencies.get(project);
		if (last == null) {
			last = Collections.emptyList();
		}
		Set<IProject> toRemove = new HashSet<>(last);
		toRemove.removeAll(dependencies);
		Set<IProject> toAdd = new HashSet<>(dependencies);
		toAdd.removeAll(last);
		for (Iterator<IProject> it = toAdd.iterator(); it.hasNext();) {
			IProject p = it.next();
			Integer refCount = trackedProjects.get(p);
			if (refCount != null) {
				it.remove();
			}
			trackedProjects.put(p, refCount != null ? refCount + 1 : 1);
		}
		for (Iterator<IProject> it = toRemove.iterator(); it.hasNext();) {
			IProject p = it.next();
			Integer refCount = trackedProjects.get(p);
			if (refCount == null || refCount <= 1) {
				trackedProjects.remove(p);
			} else {
				trackedProjects.put(p, refCount - 1);
				it.remove();
			}
		}
		addRules(toAdd);
		removeRules(toRemove);
		if (dependencies.isEmpty()) {
			projectDependencies.remove(project);
		} else {
			projectDependencies.put(project, dependencies);
		}
	}

	public void addInputContainer(IContainer container) {
		if (container != null && !getInputContainers().contains(container)) {
			inputContainers.add(container);
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
					IResourceChangeEvent.POST_CHANGE);
			addRules(Collections.singleton(container));
			if (container.getType() == IResource.PROJECT
					&& container.isAccessible()) {
				try {
					updateDependencies((IProject) container,
							Arrays.asList(((IProject) container)
									.getReferencedProjects()));
				} catch (CoreException e) {
					KommaWorkbenchPlugin.INSTANCE.log(e);
				}
			}
		}
	}

	protected void addRule(IFile file) {
		if (!supportedExtensions.contains(file.getFileExtension())
				|| ruleMap.containsKey(file.getFullPath())) {
			return;
		}
		URI fileURI = URIImpl.createPlatformResourceURI(file.getFullPath()
				.toString(), true);
		String ontology = null;
		try {
			ontology = ModelUtil.findOntology(file.getContents(), fileURI
					.toString(), ModelUtil.mimeType(ModelUtil
					.contentDescription(this, fileURI)));
		} catch (Exception e) {
			// ignore parse exception
		}
		if (ontology == null) {
			ontology = fileURI.toString();
		}
		SimpleURIMapRule rule = new SimpleURIMapRule(file.isDerived() ? 10 : 0,
				ontology, fileURI.toString());
		ruleMap.put(file.getFullPath(), rule);
		getURIMapRules().addRule(rule);
	}

	public List<IContainer> getInputContainers() {
		return inputContainers;
	}

	protected void removeRules(Collection<? extends IContainer> containers) {
		try {
			for (IContainer container : containers) {
				container.accept(new IResourceVisitor() {
					@Override
					public boolean visit(IResource resource)
							throws CoreException {
						if (resource.getType() == IResource.FILE) {
							removeRule((IFile) resource);
						}
						return true;
					}
				});
			}
		} catch (CoreException e) {
			KommaWorkbenchPlugin.INSTANCE.log(e);
		}
	}

	public boolean removeInputContainer(IContainer container) {
		if (inputContainers.remove(container)) {
			removeRules(Collections.singleton(container));
			if (container.getType() == IResource.PROJECT
					&& container.isAccessible()) {
				try {
					updateDependencies((IProject) container,
							Collections.<IProject> emptyList());
				} catch (CoreException e) {
					KommaWorkbenchPlugin.INSTANCE.log(e);
				}
			}
			if (inputContainers.isEmpty()) {
				ResourcesPlugin.getWorkspace().removeResourceChangeListener(
						this);
			}
			return true;
		}
		return false;
	}

	protected void removeRule(IFile file) {
		IURIMapRule rule = ruleMap.remove(file.getFullPath());
		if (rule != null) {
			getURIMapRules().removeRule(rule);
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		try {
			delta.accept(new IResourceDeltaVisitor() {
				@Override
				public boolean visit(IResourceDelta delta) throws CoreException {
					if (delta.getResource() instanceof IProject
							&& inputContainers.contains(delta.getResource())
							&& delta.getResource().isAccessible()) {
						IProject p = (IProject) delta.getResource();
						updateDependencies(p,
								Arrays.asList(p.getReferencedProjects()));
					} else if (delta.getResource().getType() == IResource.FILE
							&& supportedExtensions.contains(delta.getResource()
									.getFileExtension())) {
						boolean isRelevant = false;
						for (IContainer container : inputContainers) {
							isRelevant = container.getFullPath().isPrefixOf(
									delta.getFullPath());
							if (isRelevant) {
								break;
							}
						}
						isRelevant |= trackedProjects.containsKey(delta
								.getResource().getProject());
						if (isRelevant) {
							if (delta.getKind() == IResourceDelta.ADDED) {
								addRule((IFile) delta.getResource());
							} else if (delta.getKind() == IResourceDelta.REMOVED) {
								removeRule((IFile) delta.getResource());
							} else if (delta.getKind() == IResourceDelta.CHANGED
									&& delta.getFlags() != IResourceDelta.MARKERS) {
								removeRule((IFile) delta.getResource());
								addRule((IFile) delta.getResource());
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			KommaWorkbenchPlugin.INSTANCE.log(e);
		}
	}

	@Override
	public void close() throws Exception {
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		if (ws != null) {
			ws.removeResourceChangeListener(this);
		}
		trackedProjects.clear();
	}
}
