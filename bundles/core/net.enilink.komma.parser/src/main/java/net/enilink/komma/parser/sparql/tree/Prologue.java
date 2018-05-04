/*******************************************************************************
 * Copyright (c) 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
