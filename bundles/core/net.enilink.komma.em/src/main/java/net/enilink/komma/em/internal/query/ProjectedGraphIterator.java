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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import net.enilink.commons.iterator.NiceIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.komma.em.internal.IEntityManagerInternal;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IGraphResult;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.ITupleResult;
import net.enilink.komma.core.LinkedHashGraph;
import net.enilink.komma.util.RESULTS;

/**
 * Converts the repository result into an array of Objects.
 * 
 * @author Ken Wenzel
 * 
 */
public class ProjectedGraphIterator extends NiceIterator<Object> implements
		ITupleResult<Object> {
	protected static IGraph asGraph(IGraphResult result) {
		IGraph graph = new LinkedHashGraph();
		while (result.hasNext()) {
			graph.add(result.next());
		}
		result.close();
		return graph;
	}

	private IEntityManagerInternal manager;

	private int maxResults;

	private int position;

	private Iterator<IReference> resourceIt;

	private IGraph resultGraph;

	private ResultInfo resultInfo;

	protected ProjectedGraphIterator(IEntityManagerInternal manager,
			IGraph resultGraph, int maxResults, ResultInfo resultInfo) {
		this.resourceIt = new LinkedHashSet<IReference>(resultGraph.filter(
				null, RDF.PROPERTY_TYPE, RESULTS.TYPE_RESULT).subjects())
				.iterator();

		resultGraph.remove(null, RDF.PROPERTY_TYPE, RESULTS.TYPE_RESULT);

		this.resultGraph = resultGraph;
		this.manager = manager;
		this.maxResults = maxResults;
		this.resultInfo = resultInfo;
	}

	public ProjectedGraphIterator(IEntityManagerInternal manager,
			IGraphResult result, int maxResults, ResultInfo resultInfo) {
		this(manager, asGraph(result), maxResults, resultInfo);
	}

	protected Object convert(IReference resource) {
		return manager.toInstance(resource,
				resultInfo != null ? resultInfo.types.get(0) : null,
				resultGraph);
	}

	@Override
	public List<String> getBindingNames() {
		return Arrays.asList("entity");
	}

	@Override
	public boolean hasNext() {
		if (maxResults > 0 && position >= maxResults) {
			close();
			return false;
		}
		return resourceIt.hasNext();
	}

	@Override
	public Object next() {
		try {
			position++;
			return convert(resourceIt.next());
		} finally {
			if (maxResults > 0 && position >= maxResults) {
				close();
			}
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException(
				"Removal of results is not permitted.");
	}

}
