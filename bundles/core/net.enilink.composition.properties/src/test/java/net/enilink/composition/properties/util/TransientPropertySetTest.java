package net.enilink.composition.properties.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransientPropertySetTest {
    private TransientPropertySet<String> propertySet;

    @BeforeEach
    void setUp() {
        propertySet = new TransientPropertySet<>(String.class);
    }

    @Test
    void testAddAndGetAll() {
        assertTrue(propertySet.add("A"));
        assertTrue(propertySet.add("B"));
        assertFalse(propertySet.add("A")); // duplicate
        Set<String> all = propertySet.getAll();
        assertEquals(new HashSet<>(Arrays.asList("A", "B")), all);
    }

    @Test
    void testAddAll() {
        List<String> list = Arrays.asList("A", "B", "C");
        assertTrue(propertySet.addAll(list));
        assertFalse(propertySet.addAll(Collections.singleton("A"))); // already present
        Set<String> all = propertySet.getAll();
        assertEquals(new HashSet<>(list), all);
    }

    @Test
    void testSetAll() {
        propertySet.add("A");
        propertySet.setAll(Arrays.asList("X", "Y"));
        Set<String> all = propertySet.getAll();
        assertEquals(new HashSet<>(Arrays.asList("X", "Y")), all);
    }

    @Test
    void testGetSingle() {
        assertNull(propertySet.getSingle());
        propertySet.add("A");
        assertEquals("A", propertySet.getSingle());
        propertySet.add("B");
        assertTrue(propertySet.getAll().contains(propertySet.getSingle()));
    }

    @Test
    void testSetSingle() {
        propertySet.add("A");
        propertySet.add("B");
        propertySet.setSingle("Z");
        Set<String> all = propertySet.getAll();
        assertEquals(1, all.size());
        assertTrue(all.contains("Z"));
    }

    @Test
    void testRefresh() {
        propertySet.add("A");
        propertySet.refresh(); // should do nothing, but must not throw
        assertEquals(new HashSet<>(Collections.singleton("A")), propertySet.getAll());
    }

    @Test
    void testInit() {
        propertySet.add("A");
        propertySet.init(Arrays.asList("B", "C"));
        Set<String> all = propertySet.getAll();
        assertEquals(new HashSet<>(Arrays.asList("B", "C")), all);
    }

    @Test
    void testGetElementType() {
        assertEquals(String.class, propertySet.getElementType());
    }

    @Test
    void testSetAllEmpty() {
        propertySet.add("A");
        propertySet.setAll(Collections.emptyList());
        assertTrue(propertySet.getAll().isEmpty());
    }
}

