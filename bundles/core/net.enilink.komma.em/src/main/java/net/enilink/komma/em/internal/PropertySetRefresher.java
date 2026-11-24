/*******************************************************************************
 * Copyright (c) 2026 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.em.internal;

import java.lang.ref.WeakReference;
import java.util.List;

import net.enilink.composition.properties.komma.KommaPropertySetFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.dm.change.DataChangeSupport;
import net.enilink.komma.dm.change.IDataChange;
import net.enilink.komma.dm.change.IDataChangeListener;
import net.enilink.komma.dm.change.IDataChangeSupport;
import net.enilink.komma.dm.change.IStatementChange;

/**
 * Refreshes the (shared) property sets of a {@link KommaPropertySetFactory}
 * if statements are changed by any means, e.g. directly via
 * {@link net.enilink.komma.core.IEntityManager#add(IStatement)} instead of via
 * the mapped properties of an entity.
 * <p>
 * The listener only holds a weak reference to the factory and unregisters
 * itself as soon as the factory has been garbage collected.
 */
public class PropertySetRefresher implements IDataChangeListener {
	private final WeakReference<KommaPropertySetFactory> factoryRef;
	private final IDataChangeSupport changeSupport;

	public static void register(KommaPropertySetFactory factory, IDataChangeSupport changeSupport) {
		PropertySetRefresher refresher = new PropertySetRefresher(factory, changeSupport);
		if (changeSupport instanceof DataChangeSupport) {
			// ensure that property sets are refreshed before external listeners are notified
			((DataChangeSupport) changeSupport).addInternalChangeListener(refresher);
		} else {
			changeSupport.addChangeListener(refresher);
		}
	}

	protected PropertySetRefresher(KommaPropertySetFactory factory, IDataChangeSupport changeSupport) {
		this.factoryRef = new WeakReference<>(factory);
		this.changeSupport = changeSupport;
	}

	@Override
	public void dataChanged(List<IDataChange> changes) {
		KommaPropertySetFactory factory = factoryRef.get();
		if (factory == null) {
			if (changeSupport instanceof DataChangeSupport) {
				((DataChangeSupport) changeSupport).removeInternalChangeListener(this);
			} else {
				changeSupport.removeChangeListener(this);
			}
			return;
		}
		for (IDataChange change : changes) {
			if (change instanceof IStatementChange) {
				IStatement stmt = ((IStatementChange) change).getStatement();
				IReference subject = stmt.getSubject();
				if (subject != null) {
					factory.refresh(subject, stmt.getPredicate());
				}
			}
		}
	}
}
