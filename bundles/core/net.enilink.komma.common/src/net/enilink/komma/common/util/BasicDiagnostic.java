/**
 * <copyright>
 *
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
 * $Id: BasicDiagnostic.java,v 1.14 2007/10/02 13:20:26 emerks Exp $
 */
package net.enilink.komma.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import net.enilink.komma.common.AbstractKommaPlugin;

/**
 * A basic implementation of a diagnostic that that also acts as a chain.
 */
public class BasicDiagnostic implements Diagnostic, DiagnosticChain {
	/**
	 * The severity.
	 * 
	 * @see #getSeverity
	 */
	protected int severity;

	/**
	 * The message.
	 * 
	 * @see #getMessage
	 */
	protected String message;

	/**
	 * The message.
	 * 
	 * @see #getMessage
	 */
	protected List<Diagnostic> children;

	/**
	 * The data.
	 * 
	 * @see #getData
	 */
	protected List<?> data;

	/**
	 * The source.
	 * 
	 * @see #getSource
	 */
	protected String source;

	/**
	 * The code.
	 * 
	 * @see #getCode
	 */
	protected int code;

	/**
	 * Default Constructor (no initialization for local parameters)
	 */
	public BasicDiagnostic() {
		super();
	}

	public BasicDiagnostic(String source, int code, String message,
			Object[] data) {
		this.source = source;
		this.code = code;
		this.message = message;
		this.data = dataAsList(data);
	}

	public BasicDiagnostic(int severity, String source, int code,
			String message, Object[] data) {
		this(source, code, message, data);
		this.severity = severity;
	}

	public BasicDiagnostic(String source, int code,
			List<? extends Diagnostic> children, String message, Object[] data) {
		this(source, code, message, data);
		if (children != null) {
			for (Diagnostic diagnostic : children) {
				add(diagnostic);
			}
		}
	}

	protected List<?> dataAsList(Object[] data) {
		if (data == null) {
			return Collections.emptyList();
		} else {
			Object[] copy = new Object[data.length];
			System.arraycopy(data, 0, copy, 0, data.length);
			return Arrays.asList(copy);
		}
	}

	protected void setSeverity(int severity) {
		this.severity = severity;
	}

	public int getSeverity() {
		return severity;
	}

	public String getMessage() {
		return message;
	}

	public List<?> getData() {
		return data;
	}

	public List<Diagnostic> getChildren() {
		if (children == null) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(children);
		}
	}

	protected void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	protected void setCode(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public void add(Diagnostic diagnostic) {
		if (children == null) {
			children = new ArrayList<Diagnostic>();
		}

		children.add(diagnostic);
		int childSeverity = diagnostic.getSeverity();
		if (childSeverity > getSeverity()) {
			severity = childSeverity;
		}
	}

	public void addAll(Diagnostic diagnostic) {
		for (Diagnostic child : diagnostic.getChildren()) {
			add(child);
		}
	}

	public void merge(Diagnostic diagnostic) {
		if (diagnostic.getChildren().isEmpty()) {
			add(diagnostic);
		} else {
			addAll(diagnostic);
		}
	}

	public int recomputeSeverity() {
		if (children != null) {
			severity = OK;
			for (Diagnostic child : children) {
				int childSeverity = child instanceof BasicDiagnostic ? ((BasicDiagnostic) child)
						.recomputeSeverity()
						: child.getSeverity();
				if (childSeverity > severity) {
					severity = childSeverity;
				}
			}
		}

		return severity;
	}

	/**
	 * Returns the first throwable object available in the {@link #data} list,
	 * which is set when this diagnostic is instantiated.
	 */
	public Throwable getException() {
		List<?> data = getData();
		if (data != null) {
			for (Object datum : data) {
				if (datum instanceof Throwable) {
					return (Throwable) datum;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Diagnostic ");
		switch (severity) {
		case OK: {
			result.append("OK");
			break;
		}
		case INFO: {
			result.append("INFO");
			break;
		}
		case WARNING: {
			result.append("WARNING");
			break;
		}
		case ERROR: {
			result.append("ERROR");
			break;
		}
		case CANCEL: {
			result.append("CANCEL");
			break;
		}
		default: {
			result.append(Integer.toHexString(severity));
			break;
		}
		}

		result.append(" source=");
		result.append(source);

		result.append(" code=");
		result.append(code);

		result.append(' ');
		result.append(message);

		if (data != null) {
			result.append(" data=");
			result.append(data);
		}
		if (children != null) {
			result.append(' ');
			result.append(children);
		}

		return result.toString();
	}

	private static class StatusWrapper implements IStatus {
		protected static final IStatus[] EMPTY_CHILDREN = new IStatus[0];

		protected Throwable throwable;
		protected Diagnostic diagnostic;
		protected IStatus[] wrappedChildren;

		public StatusWrapper(Diagnostic diagnostic) {
			this.diagnostic = diagnostic;
		}

		public StatusWrapper(DiagnosticException diagnosticException) {
			throwable = diagnosticException;
			diagnostic = diagnosticException.getDiagnostic();
		}

		public IStatus[] getChildren() {
			if (wrappedChildren == null) {
				List<Diagnostic> children = diagnostic.getChildren();
				if (children.isEmpty()) {
					wrappedChildren = EMPTY_CHILDREN;
				} else {
					wrappedChildren = new IStatus[children.size()];
					for (int i = 0; i < wrappedChildren.length; ++i) {
						wrappedChildren[i] = toIStatus(children.get(i));
					}
				}
			}
			return wrappedChildren;
		}

		public int getCode() {
			return diagnostic.getCode();
		}

		public Throwable getException() {
			return throwable != null ? throwable : diagnostic.getException();
		}

		public String getMessage() {
			return diagnostic.getMessage();
		}

		public String getPlugin() {
			return diagnostic.getSource();
		}

		public int getSeverity() {
			return diagnostic.getSeverity();
		}

		public boolean isMultiStatus() {
			return !diagnostic.getChildren().isEmpty();
		}

		public boolean isOK() {
			return diagnostic.getSeverity() == OK;
		}

		public boolean matches(int severityMask) {
			return (diagnostic.getSeverity() & severityMask) != 0;
		}

		@Override
		public String toString() {
			return diagnostic.toString();
		}

		public static IStatus convert(Diagnostic diagnostic) {
			return diagnostic instanceof DiagnosticWrapper ? ((DiagnosticWrapper) diagnostic).status
					: new StatusWrapper(diagnostic);
		}

		public static IStatus create(DiagnosticException diagnosticException) {
			return new StatusWrapper(diagnosticException);
		}
	}

	/**
	 * Returns the diagnostic viewed as an {@link IStatus}.
	 */
	public static IStatus toIStatus(Diagnostic diagnostic) {
		return StatusWrapper.convert(diagnostic);
	}

	/**
	 * Returns the diagnostic exception viewed as an {@link IStatus}.
	 */
	public static IStatus toIStatus(DiagnosticException diagnosticException) {
		return StatusWrapper.create(diagnosticException);
	}

	private static class DiagnosticWrapper implements Diagnostic {
		protected Throwable throwable;
		protected IStatus status;
		protected List<Diagnostic> wrappedChildren;
		protected List<Diagnostic> unmodifiableWrappedChildren;
		protected List<Object> data;

		public DiagnosticWrapper(IStatus status) {
			this.status = status;
		}

		public int getCode() {
			return status.getCode();
		}

		public String getMessage() {
			return status.getMessage();
		}

		public int getSeverity() {
			return status.getSeverity();
		}

		public String getSource() {
			return status.getPlugin();
		}

		public Throwable getException() {
			return status.getException();
		}

		public List<Diagnostic> basicGetChildren() {
			if (wrappedChildren == null) {
				IStatus[] children = status.getChildren();
				if (children.length == 0) {
					wrappedChildren = new ArrayList<Diagnostic>();
				} else {
					wrappedChildren = new ArrayList<Diagnostic>(children.length);
					for (IStatus child : children) {
						wrappedChildren.add(toDiagnostic(child));
					}
				}
			}
			return wrappedChildren;
		}

		public List<Diagnostic> getChildren() {
			if (unmodifiableWrappedChildren == null) {
				unmodifiableWrappedChildren = Collections
						.unmodifiableList(basicGetChildren());
			}
			return unmodifiableWrappedChildren;
		}

		public List<?> getData() {
			if (data == null) {
				List<Object> list = new ArrayList<Object>(2);
				Throwable exception = getException();
				if (exception != null) {
					list.add(exception);
				}
				list.add(status);
				data = Collections.unmodifiableList(list);
			}
			return data;
		}

		public static Diagnostic convert(IStatus status) {
			return status instanceof StatusWrapper ? ((StatusWrapper) status).diagnostic
					: new DiagnosticWrapper(status);
		}
	}

	public static Diagnostic toDiagnostic(IStatus status) {
		return DiagnosticWrapper.convert(status);
	}

	/**
	 * Returns the throwable viewed as a {@link Diagnostic}.
	 * 
	 * @param throwable
	 * @return {@link Diagnostic}
	 */
	public static Diagnostic toDiagnostic(Throwable throwable) {
		if (throwable instanceof DiagnosticException) {
			return ((DiagnosticException) throwable).getDiagnostic();
		} else if (throwable instanceof WrappedException) {
			return toDiagnostic(throwable.getCause());
		}

		if (AbstractKommaPlugin.IS_ECLIPSE_RUNNING) {
			Diagnostic diagnostic = EclipseHelper.toDiagnostic(throwable);
			if (diagnostic != null) {
				return diagnostic;
			}
		}

		String message = throwable.getClass().getName();
		int index = message.lastIndexOf('.');
		if (index >= 0) {
			message = message.substring(index + 1);
		}
		if (throwable.getLocalizedMessage() != null) {
			message = message + ": " + throwable.getLocalizedMessage();
		}

		BasicDiagnostic basicDiagnostic = new BasicDiagnostic(Diagnostic.ERROR,
				"org.eclipse.emf.common", 0, message,
				new Object[] { throwable });

		if (throwable.getCause() != null && throwable.getCause() != throwable) {
			throwable = throwable.getCause();
			basicDiagnostic.add(toDiagnostic(throwable));
		}

		return basicDiagnostic;
	}

	private static class EclipseHelper {
		public static Diagnostic toDiagnostic(Throwable throwable) {
			if (throwable instanceof org.eclipse.core.runtime.CoreException) {
				IStatus status = ((org.eclipse.core.runtime.CoreException) throwable)
						.getStatus();
				DiagnosticWrapper wrapperDiagnostic = new DiagnosticWrapper(
						status);
				Throwable cause = throwable.getCause();
				if (cause != null && cause != throwable) {
					wrapperDiagnostic.basicGetChildren().add(
							BasicDiagnostic.toDiagnostic(cause));
				}
				return wrapperDiagnostic;
			}
			return null;
		}
	}
}
