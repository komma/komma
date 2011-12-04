package net.enilink.komma.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdf.RDF;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.concepts.IResource;
import net.enilink.komma.core.IGraph;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.IEntityManagerFactory;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.KommaException;
import net.enilink.komma.core.LinkedHashGraph;
import net.enilink.komma.core.URI;
import net.enilink.komma.util.UnitOfWork;

class ModelSetFactory implements IModelSetFactory {
	@Inject
	private IEntityManagerFactory metaDataManagerFactory;

	@Override
	public IModelSet createModelSet(URI... modelSetTypes) {
		return createModelSet(new LinkedHashGraph(), modelSetTypes);
	}

	@Override
	public IModelSet createModelSet(IGraph configuration, URI... modelSetTypes) {
		IEntityManager metaDataManager = metaDataManagerFactory.get();
		metaDataManager.add(configuration);

		List<IReference> types = new ArrayList<IReference>();
		types.addAll(Arrays.asList(modelSetTypes));
		types.add(MODELS.TYPE_MODELSET);
		types.add(RDFS.TYPE_RESOURCE);

		IModelSet modelSet = null;

		// check if a model set is setup by configuration
		IExtendedIterator<IModelSet> it = null;
		try {
			it = metaDataManager.findAll(IModelSet.class);
			if (it.hasNext()) {
				modelSet = it.next();
			}
		} finally {
			if (it != null) {
				it.close();
			}
		}

		if (modelSet != null) {
			// if a model set already exists, then simply add the additional
			// types
			try {
				metaDataManager.getTransaction().begin();

				@SuppressWarnings("unchecked")
				Set<Object> rdfTypes = (Set<Object>) ((IResource) modelSet)
						.get(RDF.PROPERTY_TYPE);
				rdfTypes.addAll(types);

				metaDataManager.getTransaction().commit();
			} catch (Exception e) {
				metaDataManager.getTransaction().rollback();
				throw new KommaException(e);
			}
		}

		if (modelSet == null) {
			// create a model set if not found
			modelSet = (IModelSet) metaDataManager.create(types
					.toArray(new IReference[types.size()]));
		}

		if (modelSet instanceof IModelSet.Internal) {
			((IModelSet.Internal) modelSet).init();
		}

		return modelSet;
	}
	
	@Inject
	protected void setUnitOfWork(UnitOfWork unitOfWork) {
		// start unit of work if it is not already active
		if (! unitOfWork.isActive()) {
			unitOfWork.begin();
		}
	}
}