/**
 * <copyright>
 *
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: EditUIMarkerHelper.java,v 1.12 2008/05/07 19:08:40 emerks Exp $
 */
package net.enilink.komma.edit.ui.util;

import java.util.ArrayList;
import java.util.List;

import net.enilink.komma.common.ui.MarkerHelper;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.validation.IValidator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

/**
 * Extension of {@link MarkerHelper} that provides extra functionalities useful
 * when using KOMMA classes such as {@link IModel}.
 */
public class EditUIMarkerHelper extends MarkerHelper {
	@Override
	protected IFile getFile(Object datum) {
		if (datum instanceof IModel) {
			IModel model = (IModel) datum;
			URI uri = model.getURI();
			uri = model.getModelSet().getURIConverter().normalize(uri);
			return getFile(uri);
		} else if (datum instanceof IObject) {
			return getFile(((IObject) datum).getModel());
		} else if (datum instanceof IModel.IDiagnostic) {
			String location = ((IModel.IDiagnostic) datum).getLocation();
			if (location != null) {
				return getFile(URIs.createURI(location));
			}
		}
		return super.getFile(datum);
	}

	@Override
	protected void adjustMarker(IMarker marker, Diagnostic diagnostic,
			Diagnostic parentDiagnostic) throws CoreException {
		if (!adjustMarker(marker, diagnostic) && parentDiagnostic != null) {
			adjustMarker(marker, parentDiagnostic);
		}
	}

	protected boolean adjustMarker(IMarker marker, Diagnostic diagnostic)
			throws CoreException {
		if (diagnostic.getData() != null) {
			for (Object element : diagnostic.getData()) {
				if (element instanceof IModel.IDiagnostic) {
					IModel.IDiagnostic modelDiagnostic = (IModel.IDiagnostic) element;
					if (modelDiagnostic.getLocation() != null) {
						marker.setAttribute(
								IMarker.LOCATION,
								KommaEditUIPlugin.getPlugin().getString(
										"_UI_MarkerLocation",
										Integer.toString(modelDiagnostic
												.getLine()),
										Integer.toString(modelDiagnostic
												.getColumn())));

						marker.setAttribute(IMarker.LINE_NUMBER,
								modelDiagnostic.getLine());
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean hasMarkers(Object object, boolean includeSubtypes, int depth) {
		if (object instanceof IModelSet) {
			IModelSet modelSet = (IModelSet) object;
			for (IModel model : modelSet.getModels()) {
				if (hasMarkers(model, includeSubtypes, depth)) {
					return true;
				}
			}
			return false;
		} else if (object instanceof Diagnostic) {
			List<?> data = ((Diagnostic) object).getData();
			if (data != null) {
				for (Object datum : data) {
					if (datum instanceof IModelSet) {
						return hasMarkers(datum, includeSubtypes, depth);
					}
				}
			}
		}

		return super.hasMarkers(object, includeSubtypes, depth);
	}

	@Override
	public void deleteMarkers(Object object, boolean includeSubtypes, int depth) {
		if (object instanceof IModelSet) {
			IModelSet modelSet = (IModelSet) object;
			for (IModel model : modelSet.getModels()) {
				deleteMarkers(model, includeSubtypes, depth);
			}
		} else if (object instanceof Diagnostic) {
			List<?> data = ((Diagnostic) object).getData();
			if (data != null) {
				for (Object datum : data) {
					if (datum instanceof IModelSet) {
						deleteMarkers(datum, includeSubtypes, depth);
						return;
					}
				}
			}
		}

		super.deleteMarkers(object, includeSubtypes, depth);
	}

	@Override
	public List<?> getTargetObjects(Object object, IMarker marker) {
		if (object instanceof AdapterFactoryEditingDomain) {
			ArrayList<Object> result = new ArrayList<Object>();
			AdapterFactoryEditingDomain editingDomain = (AdapterFactoryEditingDomain) object;
			String uriAttribute = marker.getAttribute(IValidator.URI_ATTRIBUTE,
					null);
			if (uriAttribute != null) {
				URI uri = URIs.createURI(uriAttribute);
				IObject iObject = editingDomain.getModelSet().getObject(uri,
						true);
				if (iObject != null) {
					result.add(editingDomain.getWrapper(iObject));
				}
			}
			String relatedURIsAttribute = marker.getAttribute(
					IValidator.RELATED_URIS_ATTRIBUTE, null);
			if (relatedURIsAttribute != null) {
				for (String relatedURI : relatedURIsAttribute.split(" ")) {
					URI uri = URIs.createURI(URIs.decode(relatedURI));
					IObject iObject = editingDomain.getModelSet().getObject(
							uri, true);
					if (iObject != null) {
						result.add(editingDomain.getWrapper(iObject));
					}
				}
			}
			return result;
		} else {
			return super.getTargetObjects(object, marker);
		}
	}
}
