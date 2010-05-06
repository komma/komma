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
package net.enilink.komma.model.base;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.WrappedIterator;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIImpl;

public class URIMapRuleSet implements IURIMapRuleSet {
	Set<IURIMapRule> rules = new HashSet<IURIMapRule>();
	IURIMapRule[] sortedRules;

	class PriorityComparator implements Comparator<IURIMapRule> {
		@Override
		public int compare(IURIMapRule o1, IURIMapRule o2) {
			return o1.getPriority() - o2.getPriority();
		}
	};

	@Override
	public void addRule(IURIMapRule rule) {
		if (rules.add(rule)) {
			synchronized (rules) {
				sortedRules = null;
			}
		}
	}

	@Override
	public IExtendedIterator<IURIMapRule> iterator() {
		return WrappedIterator.create(rules.iterator());
	}

	@Override
	public URI map(URI uri) {
		synchronized (rules) {
			if (sortedRules == null) {
				sortedRules = rules.toArray(new IURIMapRule[rules.size()]);
				Arrays.sort(sortedRules, new PriorityComparator());
			}
		}

		String uriString = uri.toString();
		for (IURIMapRule rule : sortedRules) {
			String replaced = rule.apply(uriString);
			if (replaced != null) {
				return URIImpl.createURI(replaced);
			}
		}
		return uri;
	}

	@Override
	public void removeRule(IURIMapRule rule) {
		if (rules.remove(rule)) {
			synchronized (rules) {
				sortedRules = null;
			}
		}
	}

}
