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
package net.enilink.komma.model.validation;

import java.util.Collection;
import java.util.Map;

import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.common.util.DiagnosticChain;
import net.enilink.komma.core.IReference;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.komma.em.concepts.IResource;

/**
 * A validity checker.
 */
public interface IValidator {
	/**
	 * This is the ID used for Eclipse markers which are based on diagnostics.
	 */
	String MARKER = "org.eclipse.emf.ecore.diagnostic";

	/**
	 * This is the name of the marker attribute to hold the String
	 * representation of the {@link org.eclipse.emf.ecore.util.EcoreUtil#getURI
	 * URI} of the object that is the target of the marker.
	 * 
	 * @see org.eclipse.emf.ecore.util.EcoreUtil#getURI
	 */
	String URI_ATTRIBUTE = "uri";

	/**
	 * This is the name of the marker attribute to hold a space separated
	 * sequence of
	 * {@link net.enilink.komma.core.URIImpl.common.util.URI#encodeFragment(String, boolean)
	 * encoded} Strings where each string is the
	 * {@link org.eclipse.emf.ecore.util.EcoreUtil#getURI URI} of an object
	 * related to the target of the marker. The vale of this attribute should be
	 * processed as follows:
	 * 
	 * <pre>
	 * for (String relatedURI : relatedURIs.split(&quot; &quot;)) {
	 * 	URI uri = URI.createURI(URI.decode(relatedURI));
	 * 	// ...
	 * }
	 *</pre>
	 * 
	 * @see org.eclipse.emf.ecore.util.EcoreUtil#getURI
	 * @see net.enilink.komma.core.URIImpl.common.util.URI#decode(String)
	 */
	String RELATED_URIS_ATTRIBUTE = "relatedURIs";

	/**
	 * An <code>EValidator</code> wrapper that is used by the
	 * {@link IValidator.Registry}.
	 */
	public interface Descriptor {
		/**
		 * Returns the validator.
		 * 
		 * @return the validator.
		 */
		IValidator getValidator();
	}

	/**
	 * A map from {@link String} to
	 * {@link IValidator}.
	 */
	interface Registry extends Map<String, Object> {
		/**
		 * Looks up the package in the map.
		 */
		IValidator getValidator(String namespace);
	}

	/**
	 * An interface for providing labels used within message substitutions.
	 */
	interface SubstitutionLabelProvider {
		/**
		 * Returns the label to identify an object.
		 */
		String getObjectLabel(IResource object);

		/**
		 * Returns the label used to identify a feature.
		 */
		String getPropertyLabel(IProperty property);

		/**
		 * Returns the label to identify a value of some data type.
		 */
		String getValueLabel(Object value);
	}

	/**
	 * An common interface for pattern-based constraints.
	 */
	interface PatternMatcher {
		/**
		 * Returns whether the string value matches the pattern.
		 */
		boolean matches(String value);
	}

	/**
	 * Validates the object in the given context, optionally producing
	 * diagnostics.
	 * 
	 * @param diagnostics
	 *            a place to accumulate diagnostics; if it's <code>null</code>,
	 *            no diagnostics should be produced.
	 * @param context
	 *            a place to cache information, if it's <code>null</code>, no
	 *            cache is supported.
	 * @return whether the object is valid.
	 */
	boolean validate(IResource object, DiagnosticChain diagnostics,
			Map<Object, Object> context);

	boolean validate(Collection<? extends IClass> clazz, IResource object,
			DiagnosticChain diagnostics, Map<Object, Object> context);

	Diagnostic validate(IReference datatype, Object value);

	boolean validate(IReference datatype, Object value,
			DiagnosticChain diagnostics, Map<Object, Object> context);
}
