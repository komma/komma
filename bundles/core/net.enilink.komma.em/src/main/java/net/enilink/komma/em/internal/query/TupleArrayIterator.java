/*******************************************************************************
 * Copyright (c) 2011 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal.query;

import java.util.List;
import java.util.Map;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.komma.em.internal.IEntityManagerInternal;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;

/**
 * Converts the repository result into a set of object arrays.
 * 
 */
public class TupleArrayIterator extends
		ConvertingIterator<IBindings<IValue>, Object[]> implements
		ITupleResult<Object[]> {
	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private ITupleResult<IBindings<IValue>> result;

	private Map<String, ResultInfo> resultInfos;

	public TupleArrayIterator(IEntityManagerInternal manager,
			ITupleResult<IBindings<IValue>> result, int maxResults,
			Map<String, ResultInfo> resultInfos) {
		super(result);
		this.result = result;
		this.manager = manager;
		this.maxResults = maxResults;
		this.resultInfos = resultInfos;
	}

	@Override
	protected Object[] convert(IBindings<IValue> sol) {
		Object[] result = new Object[getBindingNames().size()];

		ResultInfo resultInfo;
		int i = 0;
		for (String bindingName : getBindingNames()) {
			IValue value = sol.get(bindingName);

			Object converted;
			if (value == null) {
				converted = null;
			} else if (resultInfos != null
					&& (resultInfo = resultInfos.get(bindingName)) != null) {
				if (value instanceof IReference) {
					if (resultInfos.get(bindingName).typeRestricted) {
						converted = manager.findRestricted((IReference) value,
								resultInfo.types);
					} else {
						converted = manager.find((IReference) value,
								resultInfo.types);
					}
				} else {
					converted = manager.toInstance(value,
							resultInfo.types.get(0), null);
				}
			} else {
				converted = manager.toInstance(value, null, null);
			}

			result[i++] = converted;
		}
		return result;
	}

	@Override
	public List<String> getBindingNames() {
		return result.getBindingNames();
	}

	@Override
	public boolean hasNext() {
		if (maxResults > 0 && position >= maxResults) {
			close();
			return false;
		}
		return super.hasNext();
	}

	@Override
	public Object[] next() {
		try {
			position++;
			return super.next();
		} finally {
			if (maxResults > 0 && position >= maxResults) {
				close();
			}
		}
	}
}
