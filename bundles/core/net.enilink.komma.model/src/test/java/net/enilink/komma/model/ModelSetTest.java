package net.enilink.komma.model;

import com.google.inject.Guice;
import net.enilink.komma.core.*;
import net.enilink.vocab.rdf.RDF;
import org.junit.Assert;
import org.junit.Test;

public class ModelSetTest {
	@Test
	public void testCreateMetadataAndDataModelSets() {
		URI metadataModelSet = URIs.createURI("urn:enilink:metadata");
		URI dataModelSet = URIs.createURI("urn:enilink:data");
		URI memoryModelSetType = MODELS.NAMESPACE_URI.appendLocalPart("MemoryModelSet");

		KommaModule module = ModelPlugin.createModelSetModule(getClass().getClassLoader());

		IModelSetFactory factory = Guice.createInjector(new ModelSetModule(module)).getInstance(IModelSetFactory.class);

		IGraph config = new LinkedHashGraph();
		config.add(metadataModelSet, RDF.PROPERTY_TYPE, memoryModelSetType);
		config.add(dataModelSet, RDF.PROPERTY_TYPE, memoryModelSetType);
		config.add(dataModelSet, RDF.PROPERTY_TYPE, MODELS.TYPE_MODELSET);

		IModelSet metaModelSet = factory.createModelSet(metadataModelSet, config);
		metaModelSet.getModule().includeModule(module);
		IModelSet.Internal data = null;
		try {
			IModel metadataModel = metaModelSet.createModel(URIs.createURI("urn:enilink:metadata"));
			Assert.assertNotNull(metadataModel);

			data = (IModelSet.Internal) metadataModel.getManager().toInstance(dataModelSet, IModelSet.class, config);
			data = data.create(config);
			Assert.assertNotNull(data);

			IModel testModel = data.createModel(URIs.createURI("test:model"));
			Assert.assertEquals(data, testModel.getModelSet());

			var stmt = new Statement(URIs.createURI("test:subject"),
					URIs.createURI("test:predicate"), URIs.createURI("test:object"));
			testModel.getManager().add(stmt);
			Assert.assertTrue(testModel.getManager().hasMatch(stmt.getSubject(), stmt.getPredicate(), stmt.getObject()));
		} finally {
			if (data != null) {
				data.dispose();
			}
			metaModelSet.dispose();
		}
	}
}