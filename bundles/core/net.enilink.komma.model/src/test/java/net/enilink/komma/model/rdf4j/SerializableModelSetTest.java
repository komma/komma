/*******************************************************************************
 * Copyright (c) 2022 Fraunhofer IWU and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     Fraunhofer IWU - initial API and implementation
 *******************************************************************************/
package net.enilink.komma.model.rdf4j;

import junit.framework.AssertionFailedError;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.URIs;
import net.enilink.komma.core.visitor.IDataVisitor;
import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.ModelUtil;
import net.enilink.komma.model.rdf4j.SerializableModelSupport.ReconstructNodeIds;
import net.enilink.komma.model.rdf4j.SerializableModelSupport.ShortenNodeIds;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SerializableModelSetTest {

    Repository repository;
    IModelSet modelSet;

    static void assertEquals(Collection<?> expected, Set<?> actual) {
        Set<?> actualCopy = new HashSet<>(actual);
        actualCopy.removeAll(expected);
        if (!actualCopy.isEmpty()) {
            throw new AssertionFailedError("Set contains unexpected elements: " + actualCopy);
        }
        Set<?> expectedCopy = new HashSet<>(expected);
        expectedCopy.removeAll(actual);
        if (!expectedCopy.isEmpty()) {
            throw new AssertionFailedError("Set is missing expected elements: " + expectedCopy);
        }
    }

    @Before
    public void beforeTest() throws Exception {
        repository = new SailRepository(new MemoryStore());
        repository.init();
        modelSet = RDF4JModelSetFactory.createModelSet(repository);
    }

    @After
    public void afterTest() throws Exception {
        modelSet.dispose();
        // repository is not closed if model set just wraps RDF4J repository
        repository.shutDown();
    }

    @Test
    public void testShortenIds() throws Exception {
        String file = "/model-with-bnodes.ttl";

        IModel model = modelSet.createModel(URIs.createURI("test:model"));
        Map<Object, Object> options = new HashMap<>();
        options.put(IModel.OPTION_MIME_TYPE, "text/turtle");
        model.load(getClass().getResourceAsStream(file), options);

        // determine all shortened node IDs
        Set<String> expectedNodeIds = new HashSet<>();
        ModelUtil.readData(getClass().getResourceAsStream(file), model.getURI().toString(),
            "text/turtle", true, new ShortIdCollector(expectedNodeIds));

        final Set<String> foundNodeIds = new HashSet<>();
        model.getManager().match(null, null, null).forEach(stmt -> {
            List.of(stmt.getSubject(), stmt.getObject()).forEach(value -> {
                if (value instanceof IReference && ((IReference) value).getURI() == null) {
                    String id = value.toString();
                    Matcher m = ShortenNodeIds.idPattern.matcher(id);
                    if (m.matches()) {
                        foundNodeIds.add(m.group(2));
                    }
                }
            });
        });
        assertEquals(expectedNodeIds, foundNodeIds);

        // load file again leading to duplicate short IDs
        model.load(getClass().getResourceAsStream(file), options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // save model should not lead to duplicate IDs
        model.save(baos, options);

        Set<String> nodeIdsWithSuffix = new HashSet<>();
        ModelUtil.readData(new ByteArrayInputStream(baos.toByteArray()), model.getURI().toString(),
            "text/turtle", true, new ShortIdCollector(nodeIdsWithSuffix) {
                Pattern extendedIdPattern = Pattern.compile("^_:n([0-9a-z]{1,13})-[0-9]+$");
                @Override
                void addNodeId(Object value) {
                    if (value instanceof IReference && ((IReference) value).getURI() == null) {
                        String id = value.toString();
                        // recognizes short IDs with an additional suffix like '-2'
                        Matcher m = extendedIdPattern.matcher(id);
                        if (m.matches()) {
                            nodeIds.add(m.group(1));
                        }
                    }
                }
            });
        // each short node ID should be duplicated with a suffix if same file is loaded twice
        assertEquals(expectedNodeIds, nodeIdsWithSuffix);
    }

    class ShortIdCollector implements IDataVisitor<IStatement> {

        final Set<String> nodeIds;

        ShortIdCollector(Set<String> nodeIds) {
            this.nodeIds = nodeIds;
        }

        @Override
        public IStatement visitStatement(IStatement stmt) {
            addNodeId(stmt.getSubject());
            addNodeId(stmt.getObject());
            return null;
        }

        void addNodeId(Object value) {
            if (value instanceof IReference && ((IReference) value).getURI() == null) {
                String id = value.toString();
                Matcher m = ReconstructNodeIds.idPattern.matcher(id);
                if (m.matches()) {
                    nodeIds.add(m.group(1));
                }
            }
        }
    }
}