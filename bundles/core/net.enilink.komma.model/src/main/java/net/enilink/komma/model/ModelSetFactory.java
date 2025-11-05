package net.enilink.komma.model;

import java.util.Set;

import net.enilink.komma.core.*;
import net.enilink.komma.em.util.UnitOfWork;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;

import com.google.inject.Inject;

class ModelSetFactory implements IModelSetFactory {
	@Inject
	private IEntityManagerFactory metaDataManagerFactory;

	@Override
	public IModelSet createModelSet(URI... modelSetTypes) {
		IGraph config = new LinkedHashGraph();
		BlankNode modelSet = new BlankNode();
		config.add(modelSet, RDF.PROPERTY_TYPE, MODELS.TYPE_MODELSET);
		config.add(modelSet, RDF.PROPERTY_TYPE, RDFS.TYPE_RESOURCE);
		for (URI type : modelSetTypes) {
			config.add(modelSet, RDF.PROPERTY_TYPE, type);
		}
		return createModelSet(null, config);
	}

	@Override
	public IModelSet createModelSet(URI name, IGraph config) {
		IEntityManager metaDataManager = metaDataManagerFactory.get();

		IReference ms = name;
		if (ms == null) {
			Set<IReference> modelSets = config.filter(null, RDF.PROPERTY_TYPE,
					MODELS.TYPE_MODELSET).subjects();
			if (!modelSets.isEmpty()) {
				ms = modelSets.iterator().next();
			}
		}
		if (ms == null) {
			throw new IllegalArgumentException(
					"No model set defined in config.");
		}

		IGraph fullConfig = new LinkedHashGraph(config);
		fullConfig.add(ms, RDF.PROPERTY_TYPE, MODELS.TYPE_MODELSET);
		fullConfig.add(ms, RDF.PROPERTY_TYPE, RDFS.TYPE_RESOURCE);

		if (ms.getURI() == null) {
			// ensure that model set has a unique URI that can be later used to
			// retrieve the correct instance from the store
			URI msUri = URIs.createURI(BlankNode.generateId());
			fullConfig.rename(ms, msUri);
			ms = msUri;
		}

		// add basic types persistently to model set
		// the rest of the config is passed to the toInstance call below and only kept transiently in memory
		metaDataManager.add(new Statement(ms, RDF.PROPERTY_TYPE, MODELS.TYPE_MODELSET));
		metaDataManager.add(new Statement(ms, RDF.PROPERTY_TYPE, RDFS.TYPE_RESOURCE));

		// config is not added to metaDataManager to allow later modifications
		IModelSet modelSet = metaDataManager.toInstance(ms, IModelSet.class, fullConfig);
		if (modelSet instanceof IModelSet.Internal) {
			modelSet = ((IModelSet.Internal) modelSet).create(fullConfig);
		}
		return modelSet;
	}

	@Inject
	protected void setUnitOfWork(UnitOfWork unitOfWork) {
		// start unit of work if it is not already active
		if (!unitOfWork.isActive()) {
			unitOfWork.begin();
		}
	}
}