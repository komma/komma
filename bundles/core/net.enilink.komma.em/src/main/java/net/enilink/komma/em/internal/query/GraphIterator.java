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

import net.enilink.commons.iterator.ConvertingIterator;
import net.enilink.komma.em.concepts.IResource;
import net.enilink.komma.em.internal.IEntityManagerInternal;
import net.enilink.komma.core.IGraphResult;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.IValue;

/**
 * Converts the result into an array of Objects.
 * 
 */
public class GraphIterator extends ConvertingIterator<IStatement, IStatement> {
	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private boolean resolve;

	public GraphIterator(IEntityManagerInternal manager, IGraphResult result,
			int maxResults, boolean resolve) {
		super(result);

		this.manager = manager;
		this.maxResults = maxResults;
		this.resolve = resolve;
	}

	@Override
	protected IStatement convert(IStatement stmt) {
		if (resolve) {
			return new net.enilink.komma.core.Statement(
					(IResource) manager
							.find(stmt.getSubject(), IResource.class),
					manager.find(stmt.getPredicate()), manager.toInstance(
							(IValue) stmt.getObject(), null, null));
		}
		return stmt;
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
	public IStatement next() {
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
