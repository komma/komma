/*
 * Copyright (c) 2009, 2010, James Leigh All rights reserved.
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
package net.enilink.composition.properties.mapper;

import net.enilink.composition.annotations.Iri;
import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyAttribute;
import net.enilink.composition.mapping.PropertyDescriptor;
import net.enilink.composition.properties.annotations.Localized;
import net.enilink.composition.properties.annotations.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static java.util.Locale.ENGLISH;

/**
 * Determines mapped properties of a given class.
 */
public class IriAnnotationPropertyMapper extends AbstractPropertyMapper {
	protected String getPredicate(Method readMethod) {
		Iri iri = readMethod.getAnnotation(Iri.class);
		return iri == null ? null : iri.value();
	}

	protected boolean isMappedGetter(Method method) {
		if (method.getParameterTypes().length != 0)
			return false;
		if (method.isAnnotationPresent(Iri.class))
			return true;
		return false;
	}

	@Override
	protected List<PropertyAttribute> getAttributes(Method readMethod) {
		List<PropertyAttribute> attributes = new ArrayList<>();
		for (Annotation annotation : readMethod.getAnnotations()) {
			if (annotation instanceof Localized) {
				attributes.add(new PropertyAttribute(PropertyAttribute.LOCALIZED, null));
			} else if (annotation instanceof Type) {
				attributes.add(new PropertyAttribute(PropertyAttribute.TYPE, ((Type) annotation).value()));
			}
		}
		return attributes;
	}
}
