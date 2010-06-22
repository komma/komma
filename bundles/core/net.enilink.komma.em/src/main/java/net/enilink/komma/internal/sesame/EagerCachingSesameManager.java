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
package net.enilink.komma.internal.sesame;

import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.openrdf.model.Resource;

import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.sesame.ISesameEntity;
import net.enilink.komma.sesame.ISesameResourceAware;
import net.enilink.komma.sesame.SesameReference;

public class EagerCachingSesameManager extends DecoratingSesameManager {
	@SuppressWarnings("unchecked")
	private Map<Resource, Object> resource2Bean = new ReferenceMap(
			ReferenceMap.HARD, ReferenceMap.HARD, true);

	public EagerCachingSesameManager(boolean injectManager,
			IEntityDecorator... decorators) {
		super(injectManager, decorators);
	}

	public EagerCachingSesameManager(IEntityDecorator... decorators) {
		this(true, decorators);
	}

	@Override
	protected ISesameEntity createBeanForClass(Resource resource, Class<?> type) {
		Object bean = resource2Bean.get(resource);
		if (bean == null) { // TODO check this behaviour
			// || !bean.getClass().equals(type)) {
			// Object oldBean = bean;
			// Class<?> oldType = bean == null ? null : bean.getClass();
			// if (oldType != null) {
			// Set<Class> newItfs = new HashSet<Class>(Arrays.asList(type
			// .getInterfaces()));
			// newItfs.removeAll(Arrays.asList(oldType.getInterfaces()));
			// System.out.println(Arrays.asList(newItfs));
			// }
			bean = super.createBeanForClass(resource, type);
			resource2Bean.put(resource, bean);
		}

		return (ISesameEntity) bean;
	}

	@Override
	public <T> T rename(T bean, Resource dest) {
		resource2Bean.remove(((ISesameResourceAware) bean).getSesameResource());

		resource2Bean.put(dest, bean);
		SesameReference reference = getFactory().getReference(dest);
		((ISesameManagerAware) bean).initSesameReference(reference);
		return super.rename(bean, dest);
	}
}
