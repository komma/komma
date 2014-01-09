/**
 * <copyright>
 *
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
 * $Id: ValidateAction.java,v 1.27 2008/08/08 17:27:59 marcelop Exp $
 */
package net.enilink.komma.edit.ui.action;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import net.enilink.komma.common.AbstractKommaPlugin;
import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.ui.EclipseUtil;
import net.enilink.komma.common.ui.dialogs.DiagnosticDialog;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.common.util.DiagnosticChain;
import net.enilink.komma.edit.domain.AdapterFactoryEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomain;
import net.enilink.komma.edit.domain.IEditingDomainProvider;
import net.enilink.komma.edit.provider.IItemLabelProvider;
import net.enilink.komma.edit.ui.KommaEditUIPlugin;
import net.enilink.komma.edit.ui.util.EditUIMarkerHelper;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.validation.Diagnostician;
import net.enilink.komma.model.validation.IValidator;
import net.enilink.komma.core.URIImpl;

public class ValidateAction extends AbstractActionHandler {
	public static final String DIAGNOSTIC_SOURCE = "net.enilink.komma";

	public static class EclipseResourcesUtil extends EditUIMarkerHelper {
		public IRunnableWithProgress getWorkspaceModifyOperation(
				IRunnableWithProgress runnableWithProgress) {
			return EclipseUtil
					.createWorkspaceModifyOperation(runnableWithProgress);
		}

		@Override
		protected String getMarkerID() {
			return IValidator.MARKER;
		}

		public void createMarkers(IModel model, Diagnostic diagnostic) {
			try {
				createMarkers(getFile(model), diagnostic, null);
			} catch (CoreException e) {
				KommaEditUIPlugin.INSTANCE.log(e);
			}
		}

		@Override
		protected String composeMessage(Diagnostic diagnostic,
				Diagnostic parentDiagnostic) {
			String message = diagnostic.getMessage();
			if (parentDiagnostic != null) {
				String parentMessage = parentDiagnostic.getMessage();
				if (parentMessage != null) {
					message = message != null ? parentMessage + ". " + message
							: parentMessage;
				}
			}
			return message;
		}

		@Override
		protected void adjustMarker(IMarker marker, Diagnostic diagnostic,
				Diagnostic parentDiagnostic) throws CoreException {
			List<?> data = diagnostic.getData();
			StringBuilder relatedURIs = new StringBuilder();
			boolean first = true;
			for (Object object : data) {
				if (object instanceof IObject) {
					IObject iObject = (IObject) object;
					if (first) {
						first = false;
						marker.setAttribute(IValidator.URI_ATTRIBUTE, iObject
								.getURI() == null ? "" : iObject.getURI()
								.toString());
					} else {
						if (relatedURIs.length() != 0) {
							relatedURIs.append(' ');
						}
						relatedURIs.append(URIImpl.encodeFragment(iObject
								.getURI() == null ? "" : iObject.getURI()
								.toString(), false));
					}
				}
			}

			if (relatedURIs.length() > 0) {
				marker.setAttribute(IValidator.RELATED_URIS_ATTRIBUTE,
						relatedURIs.toString());
			}

			super.adjustMarker(marker, diagnostic, parentDiagnostic);
		}
	}

	protected ISelectionProvider selectionProvider;
	protected List<IObject> selectedObjects;
	protected IEditingDomain domain;
	protected EclipseResourcesUtil eclipseResourcesUtil = AbstractKommaPlugin.IS_RESOURCES_BUNDLE_AVAILABLE ? new EclipseResourcesUtil()
			: null;

	public ValidateAction(IWorkbenchPage page) {
		super(page);
		setText(KommaEditUIPlugin.INSTANCE.getString("_UI_Validate_menu_item"));
		setDescription(KommaEditUIPlugin.INSTANCE
				.getString("_UI_Validate_simple_description"));
	}

	@Override
	public void doRun(IProgressMonitor progressMonitor) {
		final Shell shell = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getShell();
		IRunnableWithProgress runnableWithProgress = new IRunnableWithProgress() {
			public void run(final IProgressMonitor progressMonitor)
					throws InvocationTargetException, InterruptedException {
				try {
					final Diagnostic diagnostic = validate(progressMonitor);
					shell.getDisplay().asyncExec(new Runnable() {
						public void run() {
							if (progressMonitor.isCanceled()) {
								handleDiagnostic(Diagnostic.CANCEL_INSTANCE);
							} else {
								handleDiagnostic(diagnostic);
							}
						}
					});
				} finally {
					progressMonitor.done();
				}
			}
		};

		if (eclipseResourcesUtil != null) {
			runnableWithProgress = eclipseResourcesUtil
					.getWorkspaceModifyOperation(runnableWithProgress);
		}

		try {
			// This runs the operation, and shows progress.
			// (It appears to be a bad thing to fork this onto another thread.)
			//
			new ProgressMonitorDialog(shell).run(true, true,
					runnableWithProgress);
		} catch (Exception exception) {
			KommaEditUIPlugin.INSTANCE.log(exception);
		}
	}

	/**
	 * This simply execute the command.
	 */
	protected Diagnostic validate(IProgressMonitor progressMonitor) {
		int selectionSize = selectedObjects.size();
		int count = selectionSize;
		for (IObject iObject : selectedObjects) {
			for (Iterator<?> i = iObject.getContents().iterator(); i.hasNext(); i
					.next()) {
				++count;
			}
		}

		progressMonitor.beginTask("", count);

		IAdapterFactory adapterFactory = domain instanceof AdapterFactoryEditingDomain ? ((AdapterFactoryEditingDomain) domain)
				.getAdapterFactory() : null;
		Diagnostician diagnostician = createDiagnostician(adapterFactory,
				progressMonitor);

		BasicDiagnostic diagnostic;
		if (selectionSize == 1) {
			diagnostic = diagnostician.createDefaultDiagnostic(selectedObjects
					.get(0));
		} else {
			diagnostic = new BasicDiagnostic(DIAGNOSTIC_SOURCE, 0,
					KommaEditUIPlugin.INSTANCE.getString(
							"_UI_DiagnosisOfNObjects_message",
							new Object[] { Integer.toString(selectionSize) }),
					selectedObjects.toArray());
		}
		Map<Object, Object> context = diagnostician.createDefaultContext();
		for (IObject eObject : selectedObjects) {
			progressMonitor.setTaskName(KommaEditUIPlugin.INSTANCE.getString(
					"_UI_Validating_message",
					new Object[] { diagnostician.getObjectLabel(eObject) }));
			diagnostician.validate(eObject, diagnostic, context);
		}
		return diagnostic;
	}

	protected Diagnostician createDiagnostician(
			final IAdapterFactory adapterFactory,
			final IProgressMonitor progressMonitor) {
		return new Diagnostician(ModelPlugin.getDefault().getValidatorRegistry()) {
			@Override
			public String getObjectLabel(IResource object) {
				if (adapterFactory != null) {
					IItemLabelProvider itemLabelProvider = (IItemLabelProvider) adapterFactory
							.adapt(object, IItemLabelProvider.class);
					if (itemLabelProvider != null) {
						return itemLabelProvider.getText(object);
					}
				}

				return super.getObjectLabel(object);
			}

			@Override
			public boolean validate(Collection<? extends IClass> classes,
					IResource object, DiagnosticChain diagnostics,
					Map<Object, Object> context) {
				progressMonitor.worked(1);
				return super.validate(classes, object, diagnostics, context);
			}
		};
	}

	protected void handleDiagnostic(Diagnostic diagnostic) {
		int severity = diagnostic.getSeverity();
		String title = null;
		String message = null;

		if (severity == Diagnostic.ERROR || severity == Diagnostic.WARNING) {
			title = KommaEditUIPlugin.INSTANCE
					.getString("_UI_ValidationProblems_title");
			message = KommaEditUIPlugin.INSTANCE
					.getString("_UI_ValidationProblems_message");
		} else {
			title = KommaEditUIPlugin.INSTANCE
					.getString("_UI_ValidationResults_title");
			message = KommaEditUIPlugin.INSTANCE
					.getString(severity == Diagnostic.OK ? "_UI_ValidationOK_message"
							: "_UI_ValidationResults_message");
		}

		int result = 0;
		if (diagnostic.getSeverity() == Diagnostic.OK) {
			MessageDialog.openInformation(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(), title, message);
			result = Window.CANCEL;
		} else {
			result = DiagnosticDialog.open(PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getShell(), title, message,
					diagnostic);
		}

		IModel model = eclipseResourcesUtil != null ? domain.getModelSet()
				.getModels().iterator().next() : null;
		if (model != null) {
			eclipseResourcesUtil.deleteMarkers(model);
		}

		if (result == Window.OK) {
			if (!diagnostic.getChildren().isEmpty()) {
				List<?> data = (diagnostic.getChildren().get(0)).getData();
				if (!data.isEmpty() && data.get(0) instanceof IObject) {
					Object part = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.getActivePart();
					if (part instanceof ISetSelectionTarget) {
						((ISetSelectionTarget) part)
								.selectReveal(new StructuredSelection(data
										.get(0)));
					} else if (part instanceof ISelectionProvider) {
						((ISelectionProvider) part)
								.setSelection(new StructuredSelection(data
										.get(0)));
					}
				}
			}

			if (model != null) {
				for (Diagnostic childDiagnostic : diagnostic.getChildren()) {
					eclipseResourcesUtil.createMarkers(model, childDiagnostic);
				}
			}
		}
	}

	@Override
	public void refresh() {
		IStructuredSelection selection = getStructuredSelection();

		selectedObjects = new ArrayList<IObject>();
		for (Iterator<?> objects = selection.iterator(); objects.hasNext();) {
			Object object = AdapterFactoryEditingDomain.unwrap(objects.next());
			if (object instanceof IObject) {
				selectedObjects.add((IObject) object);
			} else {
				setEnabled(false);
				return;
			}
		}
		selectedObjects = ModelUtil.filterDescendants(selectedObjects);
		setEnabled(!selectedObjects.isEmpty());
	}

	public void setWorkbenchPart(IWorkbenchPart workbenchPart) {
		super.setWorkbenchPart(workbenchPart);

		if (workbenchPart instanceof IEditingDomainProvider) {
			domain = ((IEditingDomainProvider) workbenchPart)
					.getEditingDomain();
		}
	}
}
