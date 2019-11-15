/*******************************************************************************
 * Copyright (c) 2011 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal.query;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.komma.em.internal.IEntityManagerInternal;
import net.enilink.komma.core.IBindings;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;
import net.enilink.komma.core.LinkedHashBindings;
import net.enilink.komma.core.URI;

/**
 * Converts the repository result into a set of {@link IBindings}.
 * 
 */
public class TupleBindingsIterator extends
		ConvertingIterator<IBindings<IValue>, IBindings<Object>> implements
		ITupleResult<IBindings<Object>> {
	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private ITupleResult<IBindings<IValue>> result;

	private Map<String, ResultInfo> resultInfos;

	public TupleBindingsIterator(IEntityManagerInternal manager,
			ITupleResult<IBindings<IValue>> result, int maxResults,
			Map<String, ResultInfo> resultInfos) {
		super(result);
		this.result = result;
		this.manager = manager;
		this.maxResults = maxResults;
		this.resultInfos = resultInfos;
	}

	@Override
	protected IBindings<Object> convert(IBindings<IValue> sol) {
		LinkedHashBindings<Object> result = new LinkedHashBindings<Object>();

		Iterator<String> keys = sol.getKeys().iterator();
		ResultInfo resultInfo;
		ResultInfo resultInfoForAll = resultInfos != null ? resultInfos
				.get(null) : null;
		for (IValue value : sol) {
			String varName = keys.next();

			Object converted;
			if (value == null) {
				converted = null;
			} else if (resultInfos != null
					&& (((resultInfo = resultInfos.get(varName))) != null || (resultInfo = resultInfoForAll) != null)) {
				if (value instanceof IReference) {
					List<Class<?>> types = resultInfo.types;
					if (!types.isEmpty() && types.get(0) == URI.class) {
						converted = ((IReference) value).getURI();
					} else if (resultInfo.typeRestricted) {
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

			result.put(varName, converted);
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
	public IBindings<Object> next() {
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
