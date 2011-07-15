package net.enilink.komma.edit.ui.wizards;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import net.enilink.vocab.owl.AnnotationProperty;
import net.enilink.vocab.owl.DatatypeProperty;
import net.enilink.vocab.owl.DeprecatedProperty;
import net.enilink.vocab.owl.FunctionalProperty;
import net.enilink.vocab.owl.ObjectProperty;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.core.URI;

public class PropertyConnectionSorter extends ViewerSorter {
	@Override
	public int category(Object element) {
		if (element instanceof AnnotationProperty) {
			return 1;
		}
		if (element instanceof DatatypeProperty) {
			return 2;
		}
		if (element instanceof DeprecatedProperty) {
			return 3;
		}
		if (element instanceof FunctionalProperty) {
			return 4;
		}
		if (element instanceof ObjectProperty) {
			return 5;
		}

		return 0;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2) {
			return cat1 - cat2;
		}

		if (!(e1 instanceof IProperty)) {
			return 1;
		}
		if (!(e2 instanceof IProperty)) {
			return -1;
		}

		URI u1 = ((IProperty) e1).getURI();
		URI u2 = ((IProperty) e2).getURI();

		if (u1.equals(u2)) {
			return 0;
		}

		int cmpVal = getComparator().compare(u1.namespace().toString(),
				u2.namespace().toString());

		if (0 == cmpVal) {
			cmpVal = getComparator().compare(u1.localPart(), u2.localPart());
		}

		return cmpVal;
	}
}
