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
package net.enilink.composition.asm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;
import net.enilink.composition.BehaviourFactory;
import net.enilink.composition.ClassDefiner;
import net.enilink.composition.exceptions.CompositionException;
import net.enilink.composition.helpers.IPartialOrder;
import net.enilink.composition.helpers.LinearExtension;

import com.google.inject.Inject;

/**
 * Abstract implementation for {@link BehaviourFactory}s that use
 * {@link BehaviourClassProcessor}s and {@link BehaviourMethodProcessor}s to
 * create concrete behaviour classes.
 */
public abstract class BehaviourFactoryBase implements BehaviourFactory {
	class MethodProcessorRunner {
		Class<?> behaviourClass;
		Collection<Method> methods;

		public MethodProcessorRunner(Class<?> behaviourClass) {
			this.behaviourClass = behaviourClass;
			this.methods = getMethods();
		}

		private Collection<Method> getMethods() {
			List<Method> methods = new ArrayList<Method>();
			methods.addAll(Arrays.asList(behaviourClass.getMethods()));
			HashMap<Object, Method> map = new HashMap<Object, Method>();
			Map<Object, Method> pms = getProtectedMethods(behaviourClass, map);
			methods.addAll(pms.values());
			return methods;
		}

		private Map<Object, Method> getProtectedMethods(Class<?> c,
				Map<Object, Method> methods) {
			if (c == null || c.isInterface()) {
				return methods;
			}
			for (Method m : c.getDeclaredMethods()) {
				if (Modifier.isProtected(m.getModifiers())) {
					Object key = Arrays.asList(m.getName(),
							Arrays.asList(m.getParameterTypes()));
					if (!methods.containsKey(key)) {
						methods.put(key, m);
					}
				}
			}
			return getProtectedMethods(c.getSuperclass(), methods);
		}

		public boolean implementsClass() {
			for (Method method : methods) {
				for (BehaviourMethodProcessor methodProcessor : methodProcessors) {
					if (methodProcessor
							.implementsMethod(behaviourClass, method)) {
						return true;
					}
				}
			}
			return false;
		}

		public void process(BehaviourClassNode classNode) throws Exception {
			Set<BehaviourMethodProcessor> initialized = new HashSet<BehaviourMethodProcessor>();
			for (Method method : methods) {
				ExtendedMethod behaviourMethod = classNode
						.getExtendedMethod(method);
				for (BehaviourMethodProcessor methodProcessor : methodProcessors) {
					boolean implementsMethod = false;
					if (behaviourMethod == null
							&& methodProcessor.implementsMethod(
									classNode.getParentClass(), method)) {
						behaviourMethod = classNode.addExtendedMethod(method,
								definer);
						implementsMethod = true;
					}
					if (behaviourMethod != null
							&& methodProcessor.appliesTo(classNode,
									behaviourMethod)) {
						if (initialized.add(methodProcessor)) {
							methodProcessor.initialize(classNode);
						}
						methodProcessor.process(classNode, behaviourMethod);
					} else if (implementsMethod) {
						throw new CompositionException("Processor "
								+ methodProcessor.getClass()
								+ " pretended to implement method "
								+ behaviourMethod.getOverriddenMethod()
								+ " of class "
								+ classNode.getType().getClassName()
								+ " but was not applied.");
					}
				}
			}
		}
	}

	@Inject
	protected ClassDefiner definer;

	protected List<BehaviourClassProcessor> classProcessors;

	private List<BehaviourMethodProcessor> methodProcessors;

	private <T> boolean dependsOn(T element, T other) {
		DependsOn dependsOn = element.getClass().getAnnotation(DependsOn.class);
		if (dependsOn != null) {
			for (Class<?> type : dependsOn.value()) {
				if (type.isAssignableFrom(other.getClass())) {
					return true;
				}
			}
		}
		return false;
	}

	private <K, T> List<T> ensureList(Map<K, List<T>> map, K key) {
		List<T> list = map.get(key);
		if (list == null) {
			list = new ArrayList<T>();
			map.put(key, list);
		}
		return list;
	}

	Class<?> extendBehaviourClass(String extendedClassName,
			Class<?> behaviourClass) throws Exception {
		boolean createBehaviour = !behaviourClass.isInterface();
		if (!createBehaviour) {
			for (BehaviourClassProcessor classProcessor : classProcessors) {
				if (classProcessor.implementsClass(behaviourClass)) {
					createBehaviour = true;
				}
			}
		}
		MethodProcessorRunner runner = new MethodProcessorRunner(behaviourClass);
		if (createBehaviour | runner.implementsClass()) {
			BehaviourClassNode classNode = new BehaviourClassNode(
					Type.getObjectType(extendedClassName.replace('.', '/')),
					behaviourClass, AsmUtils.getClassInfo(
							behaviourClass.getName(), definer));
			for (BehaviourClassProcessor classProcessor : classProcessors) {
				classProcessor.process(classNode);
			}
			runner.process(classNode);
			return AsmUtils.defineExtendedClass(definer, classNode);
		}
		return null;
	}

	private <T> List<T> filterAndSort(final Set<T> elements) {
		final Set<T> filteredElements = new HashSet<T>(elements);

		for (Iterator<T> it = filteredElements.iterator(); it.hasNext();) {
			UseWith useWith = it.next().getClass().getAnnotation(UseWith.class);
			if (useWith != null) {
				for (Class<?> type : useWith.value()) {
					if (!type.isAssignableFrom(getClass())) {
						it.remove();
					}
				}
			}
		}

		final Map<T, List<T>> successors = new HashMap<T, List<T>>();
		for (T element : filteredElements) {
			DependsOn dependsOn = element.getClass().getAnnotation(
					DependsOn.class);
			if (dependsOn != null) {
				for (Class<?> type : dependsOn.value()) {
					for (T other : filteredElements) {
						if (other != element
								&& type.isAssignableFrom(other.getClass())
								// remove circular dependencies
								&& !dependsOn(other, element)) {
							ensureList(successors, other).add(element);
						}
					}
				}
			}
		}
		return LinearExtension.createLinearExtension(new IPartialOrder<T>() {
			@Override
			public Collection<T> getElements() {
				return filteredElements;
			}

			public Collection<T> getSuccessors(T element) {
				return successors.get(element);
			}
		});
	}

	protected abstract String getExtendedClassName(Class<?> behaviourClass);

	public Collection<Class<?>> implement(Class<?> behaviourClass)
			throws Exception {
		// first check whether we did not already create and load the
		// extension of the given behaviour class
		String extendedClassName = getExtendedClassName(behaviourClass);
		Class<?> extendedClass = AsmUtils.findClass(extendedClassName, definer);
		if (extendedClass == null) {
			extendedClass = extendBehaviourClass(extendedClassName,
					behaviourClass);
		}
		return extendedClass != null ? Collections
				.<Class<?>> singleton(extendedClass) : Collections
				.<Class<?>> emptySet();
	}

	@Inject
	public void setClassProcessors(Set<BehaviourClassProcessor> classProcessors) {
		this.classProcessors = filterAndSort(classProcessors);
	}

	@Inject
	public void setMethodProcessors(
			Set<BehaviourMethodProcessor> methodProcessors) {
		this.methodProcessors = filterAndSort(methodProcessors);
	}
}
