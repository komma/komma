/*******************************************************************************
 * Copyright (c) 2023 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/

package net.enilink.commons.iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class WrappedIteratorTest {

    @Test
    public void testIterator() {
        List<Integer> values = List.of(1, 2, 3, 4);
        IExtendedIterator<Integer> it = WrappedIterator.create(values.iterator());
        assertNotNull(it);
        assertTrue(it.hasNext());

        int i = 0;
        while (it.hasNext()) {
            assertEquals(values.get(i++), it.next());
        }
        assertEquals(values.size(), i);

        values = List.of();
        it = WrappedIterator.create(values.iterator());
        assertFalse(it.hasNext());
    }
}
