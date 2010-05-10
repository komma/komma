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
package net.enilink.komma.sesame;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.openrdf.repository.Repository;

import net.enilink.komma.internal.sesame.AbstractSesameManager;
import net.enilink.komma.internal.sesame.DecoratingSesameManager;
import net.enilink.komma.core.IEntityDecorator;
import net.enilink.komma.core.KommaModule;

/**
 * Extended SesameManagerFactory
 * 
 * @author Ken Wenzel
 * 
 */
public class DecoratingSesameManagerFactory extends SesameManagerFactory {
	private IEntityDecorator[] decorators;

	public DecoratingSesameManagerFactory(KommaModule module,
			IEntityDecorator... decorators) {
		super(module);
		this.decorators = decorators;
	}

	public DecoratingSesameManagerFactory(KommaModule module,
			Repository repository, IEntityDecorator... decorators) {
		super(module, repository);
		this.decorators = decorators;
	}

	protected IEntityDecorator[] getDecorators() {
		return decorators;
	}

	protected AbstractSesameManager createSesameManager() {
		return new DecoratingSesameManager(isInjectManager(), getDecorators());
	}

	protected boolean isInjectManager() {
		return true;
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.emptyMap();
	}

	@Override
	public Set<String> getSupportedProperties() {
		return Collections.emptySet();
	}
}
