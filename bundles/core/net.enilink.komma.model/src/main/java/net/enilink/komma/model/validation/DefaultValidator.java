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
import net.enilink.komma.model.ModelPlugin;
import net.enilink.komma.core.IReference;
import net.enilink.komma.em.concepts.IClass;
import net.enilink.komma.em.concepts.IResource;

public class DefaultValidator implements IValidator {
	public static final String DIAGNOSTIC_SOURCE = ModelPlugin.PLUGIN_ID;

	public static final IValidator INSTANCE = new DefaultValidator();

	@Override
	public boolean validate(IResource object, DiagnosticChain diagnostics,
			Map<Object, Object> context) {
		return true;
	}

	@Override
	public boolean validate(Collection<? extends IClass> clazz, IResource object,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}

	@Override
	public Diagnostic validate(IReference datatype, Object value) {
		return Diagnostic.OK_INSTANCE;
	}

	@Override
	public boolean validate(IReference datatype, Object value,
			DiagnosticChain diagnostics, Map<Object, Object> context) {
		return true;
	}
}
