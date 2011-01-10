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

import java.util.List;

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.komma.em.internal.IEntityManagerInternal;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.IValue;

/**
 * Converts the repository result into an array of Objects.
 * 
 * @author James Leigh
 * 
 */
public class TupleIterator extends ConvertingIterator<IValue[], Object[]>
		implements ITupleResult<Object[]> {
	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private ITupleResult<IValue[]> result;

	private ResultInfo[] resultInfos;

	public TupleIterator(IEntityManagerInternal manager,
			ITupleResult<IValue[]> result, int maxResults,
			ResultInfo[] resultInfos) {
		super(result);
		this.result = result;
		this.manager = manager;
		this.maxResults = maxResults;
		this.resultInfos = resultInfos;
	}

	@Override
	protected Object[] convert(IValue[] sol) {
		Object[] result = new Object[sol.length];
		for (int i = 0; i < result.length; i++) {
			IValue value = sol[i];
			if (value == null) {
				result[i] = null;
			} else if (resultInfos != null && i < resultInfos.length) {
				if (value instanceof IReference) {
					if (resultInfos[i].typeRestricted) {
						result[i] = manager.findRestricted((IReference) value,
								resultInfos[i].types);
					} else {
						result[i] = manager.find((IReference) value,
								resultInfos[i].types);
					}
				} else {
					result[i] = manager.toInstance(value,
							resultInfos[i].types.get(0), null);
				}
			} else {
				result[i] = manager.toInstance(value, null, null);
			}
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
