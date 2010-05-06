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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.core.URI;

public class CompoundURIMapRuleSet implements IURIMapRuleSet {
	private List<IURIMapRuleSet> members;

	public CompoundURIMapRuleSet(List<IURIMapRuleSet> members) {
		this.members = new CopyOnWriteArrayList<IURIMapRuleSet>(members);
	}

	public CompoundURIMapRuleSet(IURIMapRuleSet... members) {
		this(Arrays.asList(members));
	}

	public void addMember(IURIMapRuleSet member) {
		if (!this.members.contains(member)) {
			this.members.add(member);
		}
	}

	@Override
	public void addRule(IURIMapRule rule) {
		members.get(0).addRule(rule);
	}

	@Override
	public IExtendedIterator<IURIMapRule> iterator() {
		Iterator<IURIMapRuleSet> memberIt = members.iterator();
		if (!memberIt.hasNext()) {
			return NiceIterator.emptyIterator();
		}

		IExtendedIterator<IURIMapRule> ruleIt = memberIt.next().iterator();
		while (memberIt.hasNext()) {
			ruleIt.andThen(memberIt.next().iterator());
		}
		return ruleIt;
	}

	@Override
	public URI map(URI uri) {
		for (Iterator<IURIMapRuleSet> memberIt = members.iterator(); memberIt
				.hasNext();) {
			URI replaced = memberIt.next().map(uri);
			if (!uri.equals(replaced)) {
				return replaced;
			}
		}

		return uri;
	}

	public boolean removeMember(IURIMapRuleSet member) {
		return this.members.remove(member);
	}

	@Override
	public void removeRule(IURIMapRule rule) {
		members.get(0).removeRule(rule);
	}
}