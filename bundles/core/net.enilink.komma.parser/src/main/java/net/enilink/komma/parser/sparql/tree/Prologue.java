/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.parser.sparql.tree;

import java.util.List;

import net.enilink.komma.parser.sparql.tree.visitor.Visitable;
import net.enilink.komma.parser.sparql.tree.visitor.Visitor;

public class Prologue implements Visitable {
	protected IriRef base;
	protected List<PrefixDecl> prefixDecls;

	public Prologue(IriRef base, List<PrefixDecl> prefixDecls) {
		this.base = base;
		this.prefixDecls = prefixDecls;
	}
	
	public IriRef getBase() {
		return base;
	}
	
	public List<PrefixDecl> getPrefixDecls() {
		return prefixDecls;
	}

	@Override
	public <R, T> R accept(Visitor<R, T> visitor, T data) {
		return visitor.prologue(this, data);
	}
}
