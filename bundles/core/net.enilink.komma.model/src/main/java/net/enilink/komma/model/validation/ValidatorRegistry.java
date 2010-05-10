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

import java.util.HashMap;

/**
 * An implementation of a validator registry.
 */
public class ValidatorRegistry extends HashMap<String, Object> implements
		IValidator.Registry {
	private static final long serialVersionUID = 1L;

	protected IValidator.Registry delegateRegistry;

	public ValidatorRegistry() {
		super();
	}

	public ValidatorRegistry(IValidator.Registry delegateRegistry) {
		this.delegateRegistry = delegateRegistry;
	}

	@Override
	public Object get(Object key) {
		Object validator = super.get(key);
		if (validator instanceof IValidator.Descriptor) {
			IValidator.Descriptor validatorDescriptor = (IValidator.Descriptor) validator;
			validator = validatorDescriptor.getValidator();
			put((String) key, validator);
			return validator;
		} else if (validator != null) {
			return validator;
		} else {
			return delegatedGet(key);
		}
	}

	public IValidator getValidator(String namespace) {
		return (IValidator) get(namespace);
	}

	protected Object delegatedGet(Object key) {
		if (delegateRegistry != null) {
			return delegateRegistry.get(key);
		}

		return key == null ? DefaultValidator.INSTANCE : null;
	}

	@Override
	public boolean containsKey(Object key) {
		return super.containsKey(key) || delegateRegistry != null
				&& delegateRegistry.containsKey(key);
	}
}
