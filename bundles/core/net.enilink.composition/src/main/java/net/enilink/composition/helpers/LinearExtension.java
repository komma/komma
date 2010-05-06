/*******************************************************************************
 * Copyright (c) 2009, 2010 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.composition.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Simple class that creates a topological ordering for a partial ordering given
 * by an {@link IPartialOrder}.
 * 
 * @author Ken Wenzel
 * 
 */
public class LinearExtension<T> {
	private static <T> void addSuccessorsToMinElements(
			IPartialOrder<T> partialOrder, T element,
			Map<T, Integer> predCount, List<T> minElements) {
		Collection<T> succs = partialOrder.getSuccessors(element);
		if (succs != null) {
			for (T succ : succs) {
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
	}

	public static <T> List<T> createLinearExtension(
			IPartialOrder<T> partialOrder) {
		return createLinearExtension(partialOrder, new ArrayList<T>());
	}

	protected static <T> List<T> createLinearExtension(
			IPartialOrder<T> partialOrder, List<T> list) {
		// build mapping of in degrees for each node
		Map<T, Integer> predCount = new HashMap<T, Integer>();

		List<T> minElements = new LinkedList<T>();
		for (T element : partialOrder.getElements()) {
			predCount.put(element, 0);
		}
		for (T element : predCount.keySet()) {
			Collection<T> succs = partialOrder.getSuccessors(element);
			if (succs != null) {
				for (T succ : succs) {
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

			addSuccessorsToMinElements(partialOrder, firstElement, predCount,
					minElements);
			if (secondElement != null) {
				addSuccessorsToMinElements(partialOrder, secondElement,
						predCount, minElements);
			}
		}

		return list;
	}
}
