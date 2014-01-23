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
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * A default implementation of the WorkbenchURIConverter interface.
 * 
 * @since 1.0.0
 */
public class WorkbenchURIConverterImpl extends ExtensibleURIConverter implements
		IWorkbenchURIConverter, IResourceChangeListener {
	protected List<IContainer> inputContainers = new ArrayList<IContainer>();

	protected Map<URI, IURIMapRule> ruleMap = new HashMap<URI, IURIMapRule>();

	protected Set<String> supportedExtensions = new HashSet<String>(
			Arrays.asList("owl", "rdf", "n3", "ttl"));

	/**
	 * Default converter constructor, no containers.
	 */
	public WorkbenchURIConverterImpl() {
		super();
	}

	/**
	 * Construct with an input container.
	 * 
	 * @param inputContainer
	 */
	public WorkbenchURIConverterImpl(IContainer inputContainer) {
		addInputContainer(inputContainer);
	}

	public void addInputContainer(IContainer container) {
		if (container != null && !getInputContainers().contains(container)) {
			inputContainers.add(container);

			try {
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
			} catch (CoreException e) {
				KommaWorkbenchPlugin.INSTANCE.log(e);
			}
		}
	}

	protected void addRule(IFile file) {
		URI fileURI = URIImpl.createPlatformResourceURI(file.getFullPath()
				.toString(), true);
		if (!supportedExtensions.contains(fileURI.fileExtension())) {
			return;
		}
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
		ruleMap.put(fileURI, rule);
		getURIMapRules().addRule(rule);
	}

	public List<IContainer> getInputContainers() {
		return inputContainers;
	}

	public boolean removeInputContainer(IContainer container) {
		if (inputContainers.remove(container)) {
			try {
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
			} catch (CoreException e) {
				KommaWorkbenchPlugin.INSTANCE.log(e);
			}
			return true;
		}
		return false;
	}

	protected void removeRule(IFile file) {
		URIImpl fileURI = URIImpl.createPlatformResourceURI(file.getFullPath()
				.toString(), true);
		IURIMapRule rule = ruleMap.remove(fileURI);
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
					if (delta.getResource().getType() == IResource.FILE) {
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
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
