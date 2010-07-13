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
package net.enilink.composition.helpers;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isTransient;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import net.enilink.composition.ClassDefiner;
import net.enilink.composition.annotations.Iri;
import net.enilink.composition.annotations.parameterTypes;
import net.enilink.composition.asm.AsmUtils;
import net.enilink.composition.asm.CompositeClassNode;
import net.enilink.composition.asm.ExtendedMethod;
import net.enilink.composition.asm.Types;
import net.enilink.composition.asm.processors.CompositeConstructorGenerator;
import net.enilink.composition.asm.util.ExtendedMethodGenerator;
import net.enilink.composition.asm.util.MethodNodeGenerator;
import net.enilink.composition.exceptions.CompositionException;
import net.enilink.composition.mappers.RoleMapper;
import net.enilink.composition.mappers.TypeFactory;
import net.enilink.composition.traits.Behaviour;
import net.enilink.composition.vocabulary.OBJ;

import com.google.inject.Inject;

/**
 * This class takes a collection of roles (interfaces or classes) and uses
 * composition to combine this into a single class.
 * 
 * @author James Leigh
 * 
 */
public class ClassCompositor<T> implements Types, Opcodes {
	private String RDFS_SUBCLASSOF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";

	private ClassDefiner definer;
	private RoleMapper<T> mapper;
	private String className;
	private Class<?> baseClass = Object.class;
	private Set<Class<?>> interfaces;
	private Set<Class<?>> javaClasses;
	private Collection<Method> methods;
	private Map<String, Method> namedMethods;
	private List<Class<?>> behaviours;

	private Map<Method, String> superMethods = new HashMap<Method, String>();

	private TypeFactory<T> typeFactory;

	private CompositeClassNode compositeClass;

	public ClassCompositor(String className, int size) {
		this.className = className;

		interfaces = new LinkedHashSet<Class<?>>(size);
		javaClasses = new LinkedHashSet<Class<?>>(size);
	}

	public void setTypeFactory(TypeFactory<T> typeFactory) {
		this.typeFactory = typeFactory;
	}

	@Inject
	public void setClassDefiner(ClassDefiner definer) {
		this.definer = definer;
	}

	public void setRoleMapper(RoleMapper<T> mapper) {
		this.mapper = mapper;
	}

	public void setBaseClass(Class<?> baseClass) {
		this.baseClass = baseClass;
	}

	public Set<Class<?>> getInterfaces() {
		return interfaces;
	}

	public void addInterface(Class<?> iface) {
		this.interfaces.add(iface);
	}

	public void addAllBehaviours(Collection<Class<?>> javaClasses) {
		this.javaClasses.addAll(javaClasses);
	}

	public Class<?> compose() throws Exception {
		compositeClass = new CompositeClassNode(Type.getObjectType(className
				.replace('.', '/')), baseClass);

		for (Class<?> clazz : javaClasses) {
			addInterfaces(clazz);
		}
		for (Class<?> face : interfaces) {
			compositeClass.addInterfaceClass(face);
		}

		new CompositeConstructorGenerator().process(compositeClass);
		compositeClass.addInjectorField();

		behaviours = new ArrayList<Class<?>>();
		for (Class<?> clazz : javaClasses) {
			if (addBehaviour(clazz)) {
				behaviours.add(clazz);
			}
		}
		if (baseClass != null && !Object.class.equals(baseClass)) {
			javaClasses.add(baseClass);
		}
		methods = getMethods();

		namedMethods = new HashMap<String, Method>(methods.size());
		for (Method method : methods) {
			if (method.isAnnotationPresent(Iri.class)) {
				String uri = method.getAnnotation(Iri.class).value();
				if (!namedMethods.containsKey(uri)
						|| !isBridge(method, methods)) {
					namedMethods.put(uri, method);
				}
			}
		}
		for (Method method : methods) {
			if (!method.getName().startsWith("_$")) {
				boolean bridge = isBridge(method, methods);
				implementMethod(method, method.getName(), bridge);
			}
		}

		return AsmUtils.defineExtendedClass(definer, compositeClass);
	}

	private void addInterfaces(Class<?> clazz) {
		if (interfaces.contains(clazz)) {
			return;
		}
		if (clazz.isInterface() && !isSpecial(clazz)) {
			interfaces.add(clazz);
		}
		Class<?> superclass = clazz.getSuperclass();
		if (superclass != null) {
			addInterfaces(superclass);
		}
		for (Class<?> face : clazz.getInterfaces()) {
			addInterfaces(face);
		}
	}

	private Collection<Method> getMethods() {
		Map<List<?>, Method> map = new HashMap<List<?>, Method>();
		for (Class<?> face : interfaces) {
			for (Method m : face.getMethods()) {
				if (isSpecial(m)) {
					continue;
				}
				Class<?>[] ptypes = getParameterTypes(m);
				List<Object> list = new ArrayList<Object>(ptypes.length + 2);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(ptypes));
				if (map.containsKey(list)) {
					if (getRank(m) > getRank(map.get(list))) {
						map.put(list, m);
					}
				} else {
					map.put(list, m);
				}
			}
		}
		for (Class<?> face : javaClasses) {
			for (Method m : face.getMethods()) {
				if (isSpecial(m)) {
					continue;
				}
				Class<?>[] ptypes = getParameterTypes(m);
				List<Object> list = new ArrayList<Object>(ptypes.length + 2);
				list.add(m.getName());
				list.add(m.getReturnType());
				list.addAll(Arrays.asList(ptypes));
				if (map.containsKey(list)) {
					if (getRank(m) > getRank(map.get(list))) {
						map.put(list, m);
					}
				} else {
					map.put(list, m);
				}
			}
		}
		return map.values();
	}

	private int getRank(Method m) {
		int rank = m.getAnnotations().length;
		if (m.isAnnotationPresent(parameterTypes.class))
			return rank - 1;
		return rank;
	}

	private boolean isSpecial(Class<?> iface) {
		return Behaviour.class.isAssignableFrom(iface);
	}

	private boolean isSpecial(Method m) {
		if (Modifier.isStatic(m.getModifiers())
				|| Modifier.isTransient(m.getModifiers())) {
			return true;
		}
		return Object.class.equals(m.getDeclaringClass());
	}

	private Class<?>[] getParameterTypes(Method m) {
		if (m.isAnnotationPresent(parameterTypes.class))
			return m.getAnnotation(parameterTypes.class).value();
		return m.getParameterTypes();
	}

	private boolean isBridge(Method method, Collection<Method> methods) {
		for (Method m : methods) {
			if (!m.getName().equals(method.getName()))
				continue;
			if (!Arrays.equals(getParameterTypes(m), getParameterTypes(method)))
				continue;
			if (m.getReturnType().equals(method.getReturnType()))
				continue;
			if (m.getReturnType().isAssignableFrom(method.getReturnType()))
				return true;
		}
		return false;
	}

	private Type[] toTypes(Class<?>[] classes) {
		Type[] types = new Type[classes.length];
		for (int i = 0; i < classes.length; i++) {
			types[i] = Type.getType(classes[i]);
		}
		return types;
	}

	private boolean implementMethod(Method method, String name, boolean bridge)
			throws Exception {
		List<Class<?>> chain = chain(method);
		List<Object[]> implementations = getImplementations(chain, method);
		if (implementations.isEmpty()) {
			return false;
		}
		// TODO use direct chaining if not isMessage(chain, method)
		Class<?> returnType = method.getReturnType();
		boolean chained = implementations.size() > 1
				|| !returnType.equals(((Method) implementations.get(0)[1])
						.getReturnType()) || isMessage(chain, method);
		Method face = AsmUtils.findInterfaceOrSuperMethod(method,
				method.getDeclaringClass(),
				compositeClass.getInterfacesClasses());

		ExtendedMethod newMethod = compositeClass.addExtendedMethod(face,
				definer);

		// clear instruction in the case when faceNode is not an abstract method
		newMethod.instructions.clear();

		if (bridge) {
			newMethod.access |= ACC_BRIDGE;
		}

		MethodNodeGenerator gen = new MethodNodeGenerator(newMethod);

		boolean chainStarted = false;
		for (Object[] ar : implementations) {
			assert ar.length == 2;
			Object target = ar[0];
			Method m = (Method) ar[1];
			if (chained) {
				if (!chainStarted) {
					chainStarted = true;

					gen.newInstance(INVOCATIONMESSAGECONTEXT_TYPE);
					gen.dup();

					gen.loadThis();
					Class<?> mtype = getMessageType(method);
					if (mtype != null) {
						gen.push(Type.getType(mtype));
					} else {
						gen.push((String) null);
					}

					// push method
					loadMethodObject(Type.getType(m.getDeclaringClass()),
							m.getName(), Type.getType(m.getReturnType()),
							toTypes(m.getParameterTypes()), gen);
					gen.loadArgArray();

					gen.invokeConstructor(
							INVOCATIONMESSAGECONTEXT_TYPE,
							new org.objectweb.asm.commons.Method("<init>",
									Type.VOID_TYPE, new Type[] { OBJECT_TYPE,
											Type.getType(Class.class),
											Type.getType(Method.class),
											Type.getType(Object[].class) }));
				}
				if ("super".equals(target)) {
					String dname = createSuperCall(m);
					appendInvocation("this", compositeClass.getType(), dname,
							Type.getType(m.getReturnType()),
							toTypes(m.getParameterTypes()), gen);
				} else {
					appendInvocation(target,
							Type.getType(m.getDeclaringClass()), m.getName(),
							Type.getType(m.getReturnType()),
							toTypes(m.getParameterTypes()), gen);
				}
			} else {
				callMethod(target, m, gen);
			}
		}
		boolean voidReturnType = returnType.equals(Void.TYPE);
		if (chainStarted) {
			String proceed;
			if (!voidReturnType && returnType.isPrimitive()) {
				proceed = "Object getFunctionalLiteralResponse()";
			} else if (returnType.equals(Set.class)) {
				proceed = "java.util.Set getObjectResponse()";
			} else if (!voidReturnType) {
				proceed = "Object getFunctionalObjectResponse()";
			} else {
				proceed = "void proceed()";
			}
			gen.invokeVirtual(INVOCATIONMESSAGECONTEXT_TYPE,
					org.objectweb.asm.commons.Method.getMethod(proceed));
			if (!voidReturnType) {
				gen.unbox(Type.getType(returnType));
			}
			chainStarted = false;
		}
		gen.returnValue();
		gen.endMethod();
		return true;
	}

	@SuppressWarnings("unchecked")
	private String createSuperCall(Method m) {
		if (superMethods.containsKey(m)) {
			return superMethods.get(m);
		}
		String name = "_$super" + superMethods.size() + "_" + m.getName();
		MethodNode mn = new MethodNode(ACC_PRIVATE, name,
				Type.getMethodDescriptor(m), null, null);
		compositeClass.methods.add(mn);
		MethodNodeGenerator gen = new MethodNodeGenerator(mn);
		// call super method
		gen.loadThis();
		gen.loadArgs();
		gen.invokeSpecial(compositeClass.getParentType(),
				org.objectweb.asm.commons.Method.getMethod(m));
		gen.returnValue();
		gen.endMethod();
		superMethods.put(m, name);
		return name;
	}

	private Class<?> getMessageType(Method method) {
		if (method.isAnnotationPresent(Iri.class)) {
			String id = method.getAnnotation(Iri.class).value();
			return mapper.findInterfaceConcept(typeFactory.createType(id));
		}
		return null;
	}

	private List<Class<?>> chain(Method method) throws Exception {
		if (behaviours == null) {
			return null;
		}
		int size = behaviours.size();
		List<Class<?>> all = new ArrayList<Class<?>>(size);
		for (Class<?> behaviour : behaviours) {
			if (isMethodPresent(behaviour, method)) {
				all.add(behaviour);
			}
		}
		Iterator<Class<?>> iter;
		List<Class<?>> rest = new ArrayList<Class<?>>(all.size());
		// sort plain methods before @precedes methods
		iter = all.iterator();
		while (iter.hasNext()) {
			Class<?> behaviour = iter.next();
			if (!isOverridesPresent(behaviour)) {
				rest.add(behaviour);
				iter.remove();
			}
		}
		rest.addAll(all);
		all = rest;
		rest = new ArrayList<Class<?>>(all.size());
		// sort intercepting methods before plain methods
		iter = all.iterator();
		while (iter.hasNext()) {
			Class<?> behaviour = iter.next();
			if (isMessage(behaviour, method)) {
				rest.add(behaviour);
				iter.remove();
			}
		}
		rest.addAll(all);
		// sort by @precedes annotations
		List<Class<?>> list = new ArrayList<Class<?>>(rest.size());
		while (!rest.isEmpty()) {
			int before = rest.size();
			iter = rest.iterator();
			loop: while (iter.hasNext()) {
				Class<?> b1 = iter.next();
				for (Class<?> b2 : rest) {
					if (b2 != b1 && overrides(b2, b1)) {
						continue loop;
					}
				}
				list.add(b1);
				iter.remove();
			}
			if (before <= rest.size())
				throw new CompositionException("Invalid method chain: "
						+ rest.toString());
		}
		return list;
	}

	private boolean isMessage(List<Class<?>> behaviours, Method method)
			throws Exception {
		if (behaviours != null) {
			for (Class<?> behaviour : behaviours) {
				if (isMessage(behaviour, method)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return list of <String, Method>
	 */
	private List<Object[]> getImplementations(List<Class<?>> behaviours,
			Method method) throws Exception {
		List<Object[]> list = new ArrayList<Object[]>();
		Class<?> type = method.getReturnType();
		Class<?> superclass = compositeClass.getParentClass();
		Class<?>[] types = getParameterTypes(method);
		if (behaviours != null) {
			for (Class<?> behaviour : behaviours) {
				list.add(new Object[] { behaviour, getMethod(behaviour, method) });
			}
		}
		if (!superclass.equals(Object.class)) {
			try {
				Method m = superclass.getMethod(method.getName(), types);
				Class<?> returnType = m.getReturnType();
				if (!isAbstract(m.getModifiers()) && returnType.equals(type)) {
					list.add(new Object[] { "super", m });
				}
			} catch (NoSuchMethodException e) {
				// no super method
			}
		}
		for (Method m : getSubMethods(method)) {
			if (m.equals(method)) {
				continue;
			}
			list.addAll(getImplementations(chain(m), m));
		}
		return list;
	}

	private List<Method> getSubMethods(Method method) {
		for (Annotation ann : method.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (RDFS_SUBCLASSOF.equals(mapper.findAnnotationString(type))) {
				try {
					Object value = type.getMethod("value").invoke(ann);
					String[] uris = (String[]) value;
					List<Method> list = new ArrayList<Method>(uris.length);
					for (String uri : uris) {
						Method m = namedMethods.get(uri);
						if (m != null) {
							list.add(m);
						}
					}
					return list;
				} catch (Exception e) {
					continue;
				}
			}
		}
		return Collections.emptyList();
	}

	private void appendInvocation(Object target, Type declaringClass,
			String name, Type returnType, Type[] paramTypes,
			MethodNodeGenerator gen) {
		gen.dup();
		gen.loadThis();
		if (!target.equals("this")) {
			loadBehaviour((Class<?>) target, gen);
		}
		loadMethodObject(declaringClass, name, returnType, paramTypes, gen);
		gen.invokeVirtual(INVOCATIONMESSAGECONTEXT_TYPE,
				new org.objectweb.asm.commons.Method("appendInvocation",
						INVOCATIONMESSAGECONTEXT_TYPE, new Type[] {
								OBJECT_TYPE, Type.getType(Method.class) }));
	}

	private void loadBehaviour(Class<?> behaviourClass, MethodNodeGenerator gen) {
		gen.invokeVirtual(
				compositeClass.getType(),
				new org.objectweb.asm.commons.Method(
						getGetterName(behaviourClass), Type
								.getType(behaviourClass), new Type[0]));
	}

	private void loadMethodObject(Type declaringClass, String name,
			Type returnType, Type[] paramTypes, MethodNodeGenerator gen) {
		FieldNode methodField = compositeClass.addStaticMethodField(
				declaringClass, name, returnType, paramTypes);
		gen.getStatic(compositeClass.getType(), methodField.name,
				Type.getType(methodField.desc));
	}

	private void callMethod(Object target, Method method,
			MethodNodeGenerator gen) {
		gen.loadThis();
		if (!target.equals("this")) {
			loadBehaviour((Class<?>) target, gen);
		}
		gen.loadArgs();
		gen.invokeVirtual(Type.getType(method.getDeclaringClass()),
				org.objectweb.asm.commons.Method.getMethod(method));
	}

	private boolean isMethodPresent(Class<?> javaClass, Method method)
			throws Exception {
		return getMethod(javaClass, method) != null;
	}

	private boolean isMessage(Class<?> javaClass, Method method)
			throws Exception {
		return getMethod(javaClass, method).isAnnotationPresent(
				parameterTypes.class);
	}

	private Method getMethod(Class<?> javaClass, Method method)
			throws Exception {
		try {
			Class<?>[] types = method.getParameterTypes();
			Method m = javaClass.getMethod(method.getName(), types);
			if (!isAbstract(m.getModifiers()) && !isTransient(m.getModifiers())
					&& !isObjectMethod(m)) {
				return m;
			}
		} catch (NoSuchMethodException e) {
			// look at @parameterTypes
		}
		Class<?>[] type = method.getParameterTypes();
		for (Method m : javaClass.getMethods()) {
			if (m.getName().equals(method.getName())) {
				parameterTypes ann = m.getAnnotation(parameterTypes.class);
				if (ann != null && Arrays.equals(ann.value(), type)) {
					return m;
				}
			}
		}
		return null;
	}

	private boolean isOverridesPresent(Class<?> javaClass) {
		for (Annotation ann : javaClass.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (OBJ.PRECEDES.equals(mapper.findAnnotationString(type))) {
				return true;
			}
		}
		return false;
	}

	private boolean overrides(Class<?> javaClass, Class<?> b1) throws Exception {
		for (Annotation ann : javaClass.getAnnotations()) {
			Class<? extends Annotation> type = ann.annotationType();
			if (OBJ.PRECEDES.equals(mapper.findAnnotationString(type))) {
				Method m = type.getMethod("value");
				for (Object c : ((Object[]) m.invoke(ann))) {
					if (c instanceof Class<?>
							&& ((Class<?>) c).isAssignableFrom(b1)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private String getGetterName(Class<?> javaClass) {
		return "_$get" + javaClass.getSimpleName()
				+ Integer.toHexString(javaClass.getName().hashCode());
	}

	@SuppressWarnings("unchecked")
	private boolean addBehaviour(Class<?> javaClass) throws Exception {
		Type behaviourType = Type.getType(javaClass);
		try {
			String getterName = getGetterName(javaClass);
			String fieldName = "_$" + getterName.substring(5);

			ExtendedMethod mn = new ExtendedMethod(compositeClass, ACC_PRIVATE,
					getterName, Type.getMethodDescriptor(behaviourType,
							new Type[0]), null, null);
			ExtendedMethodGenerator gen = new ExtendedMethodGenerator(mn);

			Label exists = gen.newLabel();

			gen.loadThis();
			gen.getField(compositeClass.getType(), fieldName, behaviourType);
			gen.dup();

			gen.ifNonNull(exists);
			gen.pop();

			gen.newInstance(behaviourType);
			gen.dup();

			Constructor<?> constructor;
			try {
				constructor = javaClass.getConstructor(Object.class);
				gen.loadThis();
			} catch (NoSuchMethodException e) {
				constructor = javaClass.getConstructor();
			}
			gen.invokeConstructor(behaviourType,
					org.objectweb.asm.commons.Method.getMethod(constructor));

			gen.injectMembers();

			gen.dup();
			gen.loadThis();
			gen.swap();
			gen.putField(compositeClass.getType(), fieldName, behaviourType);

			gen.mark(exists);

			gen.returnValue();
			gen.endMethod();

			compositeClass.methods.add(mn);
			compositeClass.fields.add(new FieldNode(ACC_PRIVATE, fieldName,
					behaviourType.getDescriptor(), null, null));

			return true;
		} catch (NoSuchMethodException e) {
			// no default constructor
			return false;
		}
	}

	private boolean isObjectMethod(Method m) {
		return m.getDeclaringClass().getName().equals(Object.class.getName());
	}
}
