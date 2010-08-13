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
package net.enilink.komma.internal.sesame.iterators;

import java.util.List;

import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.result.TupleResult;
import org.openrdf.store.StoreException;

import net.enilink.komma.internal.query.ResultInfo;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.sesame.iterators.SesameIterator;

/**
 * Converts the repository result into a single Bean.
 * 
 * @author James Leigh
 * @author Ken Wenzel
 */
public class SesameProjectedTupleResult extends
		SesameIterator<BindingSet, Object> {
	private List<String> bindings;

	private ISesameManager manager;

	private int maxResults;

	private int position;

	private ResultInfo[] resultInfos;

	public SesameProjectedTupleResult(ISesameManager manager,
			TupleResult result, int maxResults, ResultInfo[] resultInfos) {
		super(result);
		try {
			bindings = result.getBindingNames();
		} catch (StoreException e) {
			throw new KommaException(e);
		}
		this.manager = manager;
		this.maxResults = maxResults;
		this.resultInfos = resultInfos;
	}

	@Override
	protected Object convert(BindingSet solution) {
		Value value = solution.getValue(bindings.get(0));
		if (value == null) {
			return null;
		}
		if (resultInfos != null) {
			if (value instanceof Resource) {
				if (resultInfos[0].typeRestricted) {
					return manager.findRestricted((Resource) value,
							resultInfos[0].types);
				} else {
					return manager.find((Resource) value, resultInfos[0].types);
				}
			} else {
				return manager.getInstance(value, resultInfos[0].types[0]);
			}
		}
		return manager.getInstance(value, null);
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