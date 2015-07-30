package net.enilink.komma.em.tests;

import org.junit.Test;
import net.enilink.composition.annotations.Iri;

import net.enilink.komma.core.EntityVar;
import net.enilink.komma.core.KommaModule;
import net.enilink.komma.core.URI;
import net.enilink.komma.core.URIs;

import static org.junit.Assert.*;

public class EntityVarTest extends EntityManagerTest {
	private static final String NS = "urn:test:";

	@Iri(NS + "Concept")
	public interface Concept {
		EntityVar<?> getStateVar();

		Object getState();

		void setState(Object someState);
	}

	public static class StatefulBehaviour implements Concept {
		EntityVar<Object> stateVar;

		@Override
		public Object getState() {
			return stateVar.get();
		}

		@Override
		public void setState(Object someState) {
			this.stateVar.set(someState);
		}

		@Override
		public EntityVar<?> getStateVar() {
			return stateVar;
		}
	}

	protected KommaModule createModule() throws Exception {
		KommaModule module = super.createModule();
		module.addConcept(Concept.class);
		module.addBehaviour(StatefulBehaviour.class);
		return module;
	}

	@Test
	public void testVar() {
		URI theConcept = URIs.createURI(NS + "theConcept");

		Concept c1 = manager.createNamed(theConcept, Concept.class);
		c1.setState("This is some data.");
		Concept c2 = manager.createNamed(theConcept, Concept.class);

		// state of two beans referring to the same entity must be the same
		assertSame(c1.getState(), c2.getState());

		// changing variables after creation of beans must also work
		c2.setState("Other state");
		assertSame(c1.getState(), c2.getState());

		// reset the state variable
		c1.setState(null);
		assertSame(c1.getState(), c2.getState());

		// just to make sure it is working
		Concept c3 = manager.createNamed(theConcept, Concept.class);
		assertSame(c1.getState(), c2.getState());
		assertSame(c1.getState(), c3.getState());

		// beans with different URIs may not share internal state
		URI otherConcept = URIs.createURI(NS + "otherConcept");
		Concept otherC = manager.createNamed(otherConcept, Concept.class);
		c1.setState("C1 state.");
		otherC.setState("Other state.");
		assertNotSame(c1.getState(), otherC.getState());

		// unless someone forces it
		otherC.setState(c1.getState());
		assertSame(c1.getState(), otherC.getState());
	}
}
