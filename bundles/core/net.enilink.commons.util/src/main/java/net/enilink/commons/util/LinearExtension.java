/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.commons.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LinearExtension<T> {
	public static void main(String... args) {
		IPartialOrderProvider<String> p = new IPartialOrderProvider<String>() {
			String[] elements = new String[] { "a1", "a2", "a3", "b1", "b2" };

			@Override
			public Collection<String> getElements() {
				return Arrays.asList(elements);
			}

			@Override
			public Collection<String> getSuccessors(String element) {
				if (element == elements[0]) {
					return Arrays.asList(elements[1]);
				} else if (element == elements[1]) {
					return Arrays.asList(elements[2]);
				} else if (element == elements[3]) {
					return Arrays.asList(elements[4]);
				}

				return Collections.emptyList();
			}
		};

		LinearExtension<String> leGen = new LinearExtension<String>(p);
		leGen.createLinearExtension();
	}

	IPartialOrderProvider<T> partialOrderProvider;

	public LinearExtension(IPartialOrderProvider<T> partialOrderProvider) {
		this.partialOrderProvider = partialOrderProvider;
	}

	private void addSuccessorsToMinElements(T element, Map<T, Integer> predCount,
			List<T> minElements) {
		for (T succ : partialOrderProvider.getSuccessors(element)) {
			Integer inDegree = predCount.remove(succ);
			if (inDegree == null) {
				continue;
			}
			
			inDegree--;
			if (inDegree == 0) {
				minElements.add(succ);
			} else {
				predCount.put(succ, inDegree);
			}
		}
	}

	public List<T> createLinearExtension() {
		return createLinearExtension(new ArrayList<T>());
	}

	public List<T> createLinearExtension(List<T> list) {
		// build mapping of in degrees for each node
		Map<T, Integer> predCount = new HashMap<T, Integer>();

		List<T> minElements = new LinkedList<T>();
		for (T element : partialOrderProvider.getElements()) {
			predCount.put(element, 0);
		}
		for (T element : predCount.keySet()) {
			for (T succ : partialOrderProvider.getSuccessors(element)) {
				if (element.equals(succ)) {
					continue;
				}

				Integer p = predCount.get(succ);
				if (p == null) {
					// succ is not contained in
					// partialOrderProvider.getElements()
					continue;
				} else {
					p++;
				}
				predCount.put(succ, p);
			}
		}

		for (Map.Entry<T, Integer> entry : predCount.entrySet()) {
			if (entry.getValue().intValue() == 0) {
				minElements.add(entry.getKey());
			}
		}

		// create intial topological ordering
		while (!minElements.isEmpty()) {
			T firstElement = minElements.remove(0);
			T secondElement = null;

			if (minElements.isEmpty()) {
				// linearExt[j] = firstElement
				list.add(firstElement);
			} else {
				secondElement = minElements.remove(0);

				// linearExt[j - 1] = firstElement
				list.add(firstElement);

				// linearExt[j] = secondElement
				list.add(secondElement);
			}

			addSuccessorsToMinElements(firstElement, predCount, minElements);
			if (secondElement != null) {
				addSuccessorsToMinElements(secondElement, predCount, minElements);
			}
		}

		return list;
	}

	// boolean isDescendant(T ancestor, T descendant) {
	//		
	// }
	//
	// public boolean verify(List<T> linearExt) {
	// for (int i = 0, max = linearExt.size(); i < max - 1; i++) {
	// for (int j = i + 1; j < max; j++) {
	// T element_i = linearExt.get(i);
	// T element_j = linearExt.get(j);
	//
	// if (!descendantsMap.get(element_i).contains(element_j)
	// && descendantsMap.get(element_j).contains(element_i)) {
	// return false;
	// }
	// }
	// }
	//
	// return true;
	// }
}
