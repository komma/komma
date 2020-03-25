package net.enilink.komma.common.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;

public class EclipseUtil {
	static final Class<?> FILE_CLASS;
	static {
		Class<?> fileClass = null;
		try {
			fileClass = IFile.class;
		} catch (Throwable exception) {
			// Ignore any exceptions and assume the class isn't available.
		}
		FILE_CLASS = fileClass;
	}

	static final Class<?> FILE_REVISION_CLASS;
	static final Method FILE_REVISION_GET_URI_METHOD;
	static {
		Class<?> fileRevisionClass = null;
		Method fileRevisionGetURIMethod = null;
		try {
			fileRevisionClass = EclipseUtil.class.getClassLoader().loadClass("org.eclipse.team.core.history.IFileRevision");
			fileRevisionGetURIMethod = fileRevisionClass.getMethod("getURI");
		} catch (Throwable exeption) {
			// Ignore any exceptions and assume the class isn't
			// available.
		}
		FILE_REVISION_CLASS = fileRevisionClass;
		FILE_REVISION_GET_URI_METHOD = fileRevisionGetURIMethod;
	}

	static final String URI_EDITOR_INPUT_CLASSNAME = "org.eclipse.ui.IURIEditorInput";

	static final Class<?> URI_EDITOR_INPUT_CLASS;
	static {
		Class<?> editorInputClass = null;
		try {
			editorInputClass = EclipseUtil.class.getClassLoader().loadClass(
					URI_EDITOR_INPUT_CLASSNAME);
		} catch (Throwable exception) {
			// The class is not available.
		}
		URI_EDITOR_INPUT_CLASS = editorInputClass;
	}

	public static URI getURI(IEditorInput editorInput) {
		if (FILE_CLASS != null) {
			IFile file = (IFile) editorInput.getAdapter(FILE_CLASS);
			if (file != null) {
				return URIs.createPlatformResourceURI(file.getFullPath()
						.toString(), true);
			}
		}
		if (FILE_REVISION_CLASS != null) {
			Object fileRevision = editorInput.getAdapter(FILE_REVISION_CLASS);
			if (fileRevision != null) {
				try {
					return URIs
							.createURI(((java.net.URI) FILE_REVISION_GET_URI_METHOD
									.invoke(fileRevision)).toString());
				} catch (Throwable exception) {
					CommonUIPlugin.INSTANCE.log(exception);
				}
			}
		}
		if (URI_EDITOR_INPUT_CLASS != null
				&& editorInput != null
				&& URI_EDITOR_INPUT_CLASS.isAssignableFrom(editorInput
						.getClass())) {
			try {
				Method getURI = editorInput.getClass().getMethod("getURI");
				java.net.URI uri = (java.net.URI) getURI.invoke(editorInput);
				return URIs.createURI(uri.toString()).trimFragment();
			} catch (Exception e) {
				// Ignore
			}
		}

		return null;
	}

	static final String FILE_EDITOR_INPUT_CLASSNAME = "org.eclipse.ui.part.FileEditorInput";
	static final Class<?> FILE_EDITOR_INPUT_CLASS;
	static {
		Class<?> editorInputClass = null;
		try {
			editorInputClass = EclipseUtil.class.getClassLoader().loadClass(
					FILE_EDITOR_INPUT_CLASSNAME);
		} catch (Throwable exception) {
			// The class is not available.
		}
		FILE_EDITOR_INPUT_CLASS = editorInputClass;
	}

	public static IEditorInput createEditorInput(IFile file) {
		if (FILE_EDITOR_INPUT_CLASS != null) {
			try {
				Constructor<?> init = FILE_EDITOR_INPUT_CLASS
						.getConstructor(IFile.class);
				return (IEditorInput) init.newInstance(file);
			} catch (Exception e) {
				// Ignore
			}
		}
		return null;
	}

	public static IFile getFile(Object datum) {
		if (datum == null) {
			return null;
		}
		if (datum instanceof IFile) {
			return (IFile) datum;
		} else if (FILE_EDITOR_INPUT_CLASS != null
				&& FILE_EDITOR_INPUT_CLASS.isAssignableFrom(datum.getClass())) {
			try {
				Method getFile = datum.getClass().getMethod("getFile");
				return (IFile) getFile.invoke(datum);
			} catch (Exception e) {
				// Ignore
			}
		}
		return null;
	}

	public static IRunnableWithProgress createWorkspaceModifyOperation(
			IRunnableWithProgress runnable) {
		try {
			Constructor<?> constructor = EclipseUtil.class
					.getClassLoader()
					.loadClass(
							"org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation")
					.getConstructor(IRunnableWithProgress.class);
			return (IRunnableWithProgress) constructor.newInstance(runnable);
		} catch (Exception e) {
			// class not found
		}
		return runnable;
	}
}