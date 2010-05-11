/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.KommaCore;
import net.enilink.komma.common.util.BasicDiagnostic;
import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.common.util.DiagnosticChain;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.model.IObject;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.core.IReference;

/**
 * A validity checker for basic {@link IObject} constraints.
 */
public class Diagnostician implements IValidator.SubstitutionLabelProvider,
		IValidator {
	protected IValidator.Registry validatorRegistry;

	public Diagnostician(IValidator.Registry validatorRegistry) {
		this.validatorRegistry = validatorRegistry;
	}

	public String getObjectLabel(IObject object) {
		return ModelUtil.getLabel(object);
	}

	public String getPropertyLabel(IProperty property) {
		return ModelUtil.getLabel(property);
	}

	public String getValueLabel(Object value) {
		return ModelUtil.getLabel(value);
	}

	/**
	 * @since 2.4
	 */
	public Map<Object, Object> createDefaultContext() {
		Map<Object, Object> context = new HashMap<Object, Object>();
		context.put(IValidator.SubstitutionLabelProvider.class, this);
		context.put(IValidator.class, this);
		return context;
	}

	/**
	 * @since 2.4
	 */
	public BasicDiagnostic createDefaultDiagnostic(IObject object) {
		return new BasicDiagnostic(DefaultValidator.DIAGNOSTIC_SOURCE, 0,
				KommaCore.getDefault().getString(
						"_UI_DiagnosticRoot_diagnostic",
						new Object[] { getObjectLabel(object) }),
				new Object[] { object });
	}

	/**
	 * @since 2.4
	 */
	public BasicDiagnostic createDefaultDiagnostic(IReference datatype,
			Object value) {
		return new BasicDiagnostic(DefaultValidator.DIAGNOSTIC_SOURCE, 0,
				KommaCore.getDefault().getString(
						"_UI_DiagnosticRoot_diagnostic",
						new Object[] { getValueLabel(value) }), new Object[] {
						value, datatype });
	}

	public Diagnostic validate(IObject object) {
		BasicDiagnostic diagnostics = createDefaultDiagnostic(object);
		validate(object, diagnostics, createDefaultContext());
		return diagnostics;
	}

	/**
	 * @since 2.4
	 */
	public Diagnostic validate(IObject object, Map<?, ?> contextEntries) {
		BasicDiagnostic diagnostics = createDefaultDiagnostic(object);
		Map<Object, Object> context = createDefaultContext();
		context.putAll(contextEntries);
		validate(object, diagnostics, context);
		return diagnostics;
	}

	public boolean validate(IObject object, DiagnosticChain diagnostics,
			Map<Object, Object> context) {
		return validate(object.getDirectNamedClasses().toList(), object,
				diagnostics, context);
	}

	public boolean validate(Collection<? extends IClass> classes,
			IObject object, DiagnosticChain diagnostics,
			Map<Object, Object> context) {
		Set<IClass> seenClasses = new HashSet<IClass>();
		Queue<IClass> classesQueue = new LinkedList<IClass>(classes);
		while (!classes.isEmpty()) {
			IClass clazz = classesQueue.remove();
			seenClasses.add(clazz);

			Object validator = validatorRegistry.get(clazz.getURI()
					.trimFragment());
			if (validator == null) {
				IExtendedIterator<IClass> superClasses = clazz
						.getDirectNamedSuperClasses();
				if (classes.isEmpty() && !superClasses.hasNext()) {
					validator = validatorRegistry.get(null);
					break;
				} else {
					for (IClass superClass : superClasses) {
						if (!seenClasses.contains(superClass)) {
							classesQueue.add(superClass);
						}
					}
				}
			}

			boolean result = ((IValidator) validator).validate(
					Arrays.asList(clazz), object, diagnostics, context);
			if (result || diagnostics != null) {
				result &= doValidateContents(object, diagnostics, context);
			}
			return result;
		}
		return true;
	}

	protected boolean doValidateContents(IObject object,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		Collection<IObject> contents = object.getContents();
		if (!contents.isEmpty()) {
			Iterator<IObject> i = contents.iterator();
			IObject child = i.next();
			boolean result = validate(child, diagnostics, context);
			while (i.hasNext() && (result || diagnostics != null)) {
				child = i.next();
				result &= validate(child, diagnostics, context);
			}
			return result;
		} else {
			return true;
		}
	}

	public Diagnostic validate(IReference datatype, Object value) {
		BasicDiagnostic diagnostics = createDefaultDiagnostic(datatype, value);
		validate(datatype, value, diagnostics, createDefaultContext());
		return diagnostics;
	}

	public boolean validate(IReference datatype, Object value,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		Object validator = validatorRegistry.get(datatype.getURI()
				.trimFragment());
		if (validator == null) {
			validator = validatorRegistry.get(null);
		}

		return ((IValidator) validator).validate(datatype, value, diagnostics,
				context);
	}
}
