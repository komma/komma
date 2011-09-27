/*
 * Copyright (c) 2007, 2010, James Leigh All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
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

/**
 * Converts the repository result into an array of Objects.
 * 
 * @author James Leigh
 * 
 */
public class TupleIterator extends
		ConvertingIterator<IBindings<IValue>, IBindings<Object>> implements
		ITupleResult<IBindings<Object>> {
	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private ITupleResult<IBindings<IValue>> result;

	private Map<String, ResultInfo> resultInfos;

	public TupleIterator(IEntityManagerInternal manager,
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
		for (IValue value : sol) {
			String varName = keys.next();

			Object converted;
			if (value == null) {
				converted = null;
			} else if (resultInfos != null
					&& (resultInfo = resultInfos.get(varName)) != null) {
				if (value instanceof IReference) {
					if (resultInfos.get(varName).typeRestricted) {
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
