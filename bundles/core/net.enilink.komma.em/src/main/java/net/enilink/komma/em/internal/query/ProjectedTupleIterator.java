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
 * Converts the repository result into a single Bean.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 */
public class ProjectedTupleIterator extends
		ConvertingIterator<IValue[], Object> implements ITupleResult<Object> {
	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private ITupleResult<IValue[]> result;

	private ResultInfo[] resultInfos;

	public ProjectedTupleIterator(IEntityManagerInternal manager,
			ITupleResult<IValue[]> result, int maxResults,
			ResultInfo[] resultInfos) {
		super(result);
		this.result = result;
		this.manager = manager;
		this.maxResults = maxResults;
		this.resultInfos = resultInfos;
	}

	@Override
	protected Object convert(IValue[] solution) {
		IValue value = solution[0];
		if (value == null) {
			return null;
		}
		if (resultInfos != null) {
			if (value instanceof IReference) {
				if (resultInfos[0].typeRestricted) {
					return manager.findRestricted((IReference) value,
							resultInfos[0].types);
				} else {
					return manager.find((IReference) value,
							resultInfos[0].types);
				}
			} else {
				return manager.toInstance(value, resultInfos[0].types.get(0),
						null);
			}
		}
		return manager.toInstance(value, null, null);
	}

	@Override
	public List<String> getBindingNames() {
		return result.getBindingNames();
	}

	// private Map<Class<?>, List<Method>> useBindingsMap;
	//
	// private void bindValues(Object object, BindingSet solution) {
	// List<Method> bindMethods = useBindingsMap != null ? useBindingsMap
	// .get(object.getClass()) : null;
	// if (bindMethods == null) {
	// for (Method method : object.getClass().getMethods()) {
	// UseBinding useBinding = method.getAnnotation(UseBinding.class);
	// if (useBinding != null
	// && solution.hasBinding(useBinding.value())
	// && method.getParameterTypes().length == 1) {
	// if (bindMethods == null) {
	// bindMethods = new ArrayList<Method>();
	// }
	// bindMethods.add(method);
	// }
	// }
	// if (useBindingsMap == null) {
	// useBindingsMap = new HashMap<Class<?>, List<Method>>();
	// }
	// useBindingsMap.put(object.getClass(),
	// bindMethods != null ? bindMethods : Collections
	// .<Method> emptyList());
	// }
	//
	// for (Method method : bindMethods) {
	// UseBinding useBinding = method.getAnnotation(UseBinding.class);
	// try {
	// Object bindingValue = solution.getValue(useBinding.value());
	// Class<?> type = method.getParameterTypes()[0];
	// if (Boolean.class.equals(type) || Boolean.TYPE.equals(type)) {
	// // convert to boolean value
	// bindingValue = bindingValue != null;
	// } else {
	// bindingValue = manager.getInstance((Value) bindingValue);
	// }
	// method.invoke(object, bindingValue);
	// } catch (Exception e) {
	// throw new KommaException(e);
	// }
	// }
	// }

	@Override
	public boolean hasNext() {
		if (maxResults > 0 && position >= maxResults) {
			close();
			return false;
		}
		return super.hasNext();
	}

	@Override
	public Object next() {
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