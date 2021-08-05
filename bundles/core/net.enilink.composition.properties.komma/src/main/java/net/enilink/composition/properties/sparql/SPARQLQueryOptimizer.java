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
package net.enilink.composition.properties.sparql;

import net.enilink.composition.mapping.IPropertyMapper;
import net.enilink.composition.mapping.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import net.enilink.composition.asm.util.BehaviourMethodGenerator;
import net.enilink.composition.exceptions.ConfigException;
import net.enilink.composition.properties.annotations.Name;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IQuery;
import net.enilink.komma.core.IStatement;

/**
 * Rewrites the SPARQL query used by sparql behaviour methods by loading
 * additional properties.
 * 
 */
public class SPARQLQueryOptimizer {
	private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	private static final Pattern selectWhere = Pattern.compile(
			"\\sSELECT\\s+([\\?\\$]\\w+)\\s+WHERE\\s*\\{",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final Type IGRAPH_TYPE = Type.getType(IGraph.class);
	private static final Type ISTATEMENT_TYPE = Type.getType(IStatement.class);
	private static final Type OBJECT_ARRAY_TYPE = Type.getType(Object[].class);

	/** @return map of name to uri */
	public Map<String, String> findEagerProperties(IPropertyMapper pm, Class<?> type) {
		Map<String, String> properties = new HashMap<String, String>();
		findEagerProperties(pm, type, properties);
		if (properties.isEmpty())
			return null;
		properties.put("class", RDF_TYPE);
		return properties;
	}

	private Map<String, String> findEagerProperties(IPropertyMapper pm, Class<?> concept,
													Map<String, String> properties) {
		for (PropertyDescriptor pd : pm.getProperties(concept)) {
			Class<?> type = pd.getPropertyType();
			java.lang.reflect.Type generic = pd.getReadMethod().getGenericReturnType();
			if (!isEagerPropertyType(generic, type))
				continue;
			properties.put(pd.getName(), pd.getPredicate());
		}
		for (Class<?> face : concept.getInterfaces()) {
			findEagerProperties(pm, face, properties);
		}
		if (concept.getSuperclass() == null)
			return properties;
		return findEagerProperties(pm, concept.getSuperclass(), properties);
	}

	private boolean isEagerPropertyType(java.lang.reflect.Type t, Class<?> type) {
		return !Set.class.equals(type);
	}

	public void implementQuery(String sparql, String base, IPropertyMapper pm,
			Method method, BehaviourMethodGenerator gen) throws Exception {
		Class<?> range = method.getReturnType();

		boolean primitive = range.isPrimitive();
		boolean functional = !range.equals(Set.class);
		Map<String, String> eager = null;
		if (functional && !primitive) {
			eager = findEagerProperties(pm, range);
		} else if (!primitive) {
			range = Object.class;
			java.lang.reflect.Type t = method.getGenericReturnType();
			if (t instanceof ParameterizedType) {
				java.lang.reflect.Type c = ((ParameterizedType) t)
						.getActualTypeArguments()[0];
				if (c instanceof Class<?>) {
					range = (Class<?>) c;
					eager = findEagerProperties(pm, (Class<?>) c);
				}
			}
		}

		implementQuery(sparql, base, eager, range, functional, method, gen);
	}

	protected void setParameters(Method method, BehaviourMethodGenerator gen)
			throws Exception {
		for (int i = 0, params = method.getParameterTypes().length; i < params; i++) {
			boolean named = false;
			for (Annotation ann : method.getParameterAnnotations()[i]) {
				if (ann.annotationType().equals(Name.class)) {
					for (String name : ((Name) ann).value()) {
						named = true;

						gen.dup();
						gen.push(name);
						gen.loadArg(i);

						gen.invoke(IQuery.class.getMethod("setParameter",
								String.class, Object.class));
					}
				}
			}
			if (!named)
				throw new ConfigException("@name annotation not found: "
						+ method.getName());
		}
	}

	public void implementQuery(String qry, String base,
			Map<String, String> eager, Class<?> range, boolean functional,
			Method method, BehaviourMethodGenerator gen) throws Exception {
		prepareQuery(qry, base, range, eager, gen);
		setParameters(method, gen);
		evaluateQuery(range, functional, gen);
	}

	private void prepareQuery(String qry, String base, Class<?> range,
			Map<String, String> eager, BehaviourMethodGenerator gen)
			throws Exception {
		Method prepareMethod = IEntityManager.class.getMethod("createQuery",
				String.class, String.class);

		Type rangeType = Type.getType(range);
		boolean objectQuery = !(IGRAPH_TYPE.equals(rangeType)
				|| ISTATEMENT_TYPE.equals(rangeType) || OBJECT_ARRAY_TYPE
				.equals(rangeType));

		gen.loadThis();
		gen.invokeVirtual(gen.getMethod().getOwner().getType(),
				SparqlBehaviourMethodProcessor.GET_MANAGER);

		if (objectQuery) {
			gen.push(optimizeQueryString(qry, eager));
		} else {
			gen.push(qry);
		}
		gen.push(base);
		gen.invoke(prepareMethod);

		gen.dup();
		gen.push("this");
		gen.loadBean();
		gen.invoke(IQuery.class.getMethod("setParameter", String.class,
				Object.class));
	}

	/**
	 * @param map
	 *            property name to predicate uri or null for datatype
	 */
	private String optimizeQueryString(String sparql, Map<String, String> map) {
		Matcher matcher = selectWhere.matcher(sparql);
		if (map != null && matcher.find()) {
			String var = matcher.group(1);
			int idx = sparql.lastIndexOf('}');
			StringBuilder sb = new StringBuilder(256 + sparql.length());
			sb.append(sparql, 0, matcher.start(1));
			sb.append(var).append(" ");
			sb.append(var).append("_class").append(" ");
			for (Map.Entry<String, String> e : map.entrySet()) {
				String name = e.getKey();
				if (name.equals("class"))
					continue;
				sb.append(var).append("_").append(name).append(" ");
			}
			sb.append(sparql, matcher.end(1), idx);
			sb.append(" OPTIONAL { ").append(var);
			sb.append(" a ").append(var).append("_class}");
			for (Map.Entry<String, String> e : map.entrySet()) {
				String pred = e.getValue();
				String name = e.getKey();
				if (name.equals("class"))
					continue;
				sb.append(" OPTIONAL { ").append(var).append(" <");
				sb.append(pred).append("> ");
				sb.append(var).append("_").append(name).append("}");
			}
			sb.append(sparql, idx, sparql.length());
			sparql = sb.toString();
		}
		return sparql;
	}

	private void evaluateQuery(Class<?> range, boolean functional,
			BehaviourMethodGenerator gen) throws Exception {
		Label tryLabel = gen.mark();
		// try
		int result = gen.newLocal(Type.getType(IExtendedIterator.class));

		Type rangeType = Type.getType(range);

		if (IGRAPH_TYPE.equals(rangeType) || ISTATEMENT_TYPE.equals(rangeType)) {
			gen.push(ISTATEMENT_TYPE);
			gen.push(0);
			gen.newArray(Type.getType(Class.class));
			gen.invoke(IQuery.class.getMethod("evaluate", Class.class,
					Class[].class));
		} else {
			gen.invoke(IQuery.class.getMethod("evaluate"));
		}
		gen.storeLocal(result);

		gen.loadLocal(result);
		gen.invoke(Iterator.class.getMethod("hasNext"));
		Label noNext = gen.newLabel();
		gen.ifZCmp(GeneratorAdapter.EQ, noNext);
		gen.loadLocal(result);
		gen.invoke(Iterator.class.getMethod("next"));

		gen.mark(noNext);
		gen.push((String) null);

		Label noException = gen.newLabel();
		gen.goTo(noException);

		// catch (Throwable e) {
		// throw e;
		// }
		Label catchLabel = gen.mark();
		Type runtimeException = Type.getType(Throwable.class);
		gen.catchException(tryLabel, catchLabel, runtimeException);
		gen.loadLocal(result);
		gen.invoke(IExtendedIterator.class.getMethod("close"));
		gen.throwException();

		gen.mark(noException);
		gen.loadLocal(result);
		gen.invoke(IExtendedIterator.class.getMethod("close"));

		gen.returnValue();
	}
}
