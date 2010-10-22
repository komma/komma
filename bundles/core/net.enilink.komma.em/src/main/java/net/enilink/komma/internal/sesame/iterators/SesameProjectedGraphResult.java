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

import java.util.Iterator;
import java.util.LinkedHashSet;

import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.result.GraphResult;
import org.openrdf.store.StoreException;

import net.enilink.commons.iterator.NiceIterator;
import net.enilink.komma.sesame.ISesameManager;
import net.enilink.komma.util.RESULTS;

/**
 * Converts the repository result into an array of Objects.
 * 
 * @author Ken Wenzel
 * 
 */
public class SesameProjectedGraphResult extends NiceIterator<Object> {
	private Iterator<Resource> resourceIt;

	private Model resultModel;

	private ISesameManager manager;

	private int maxResults;

	private int position;

	public SesameProjectedGraphResult(ISesameManager manager,
			GraphResult result, int maxResults) throws StoreException {
		this(manager, result.asModel(), maxResults);
	}

	protected SesameProjectedGraphResult(ISesameManager manager,
			Model resultModel, int maxResults) {
		this.resourceIt = new LinkedHashSet<Resource>(resultModel.filter(null,
				RDF.TYPE, new URIImpl(RESULTS.TYPE_RESULT.toString()))
				.subjects()).iterator();

		resultModel.remove(null, RDF.TYPE, new URIImpl("urn:komma:Result"));

		this.resultModel = resultModel;
		this.manager = manager;
		this.maxResults = maxResults;
	}

	protected Object convert(Resource resource) {
		return manager.createBean(resource, null, false, resultModel);
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
