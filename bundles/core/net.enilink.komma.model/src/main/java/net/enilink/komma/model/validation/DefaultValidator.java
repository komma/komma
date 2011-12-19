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

import java.util.Collection;
import java.util.Map;

import net.enilink.komma.common.util.Diagnostic;
import net.enilink.komma.common.util.DiagnosticChain;
import net.enilink.komma.concepts.IClass;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.model.ModelCore;
import net.enilink.komma.core.IReference;

public class DefaultValidator implements IValidator {
	public static final String DIAGNOSTIC_SOURCE = ModelCore.PLUGIN_ID;

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
