package net.enilink.komma.edit.properties;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.UnexecutableCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;
import net.enilink.komma.edit.KommaEditPlugin;
import net.enilink.komma.em.concepts.IProperty;
import net.enilink.vocab.owl.OWL;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.vocab.xmlschema.XMLSCHEMA;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class LiteralEditingSupport implements IPropertyEditingSupport {
	@Override
	public ProposalSupport getProposalSupport(IEntity subject,
			IReference property, Object value) {
		return null;
	}

	@Override
	public boolean canEdit(IEntity subject, IReference property, Object value) {
		return true;
	}

	@Override
	public Object getValueForEditor(IEntity subject, IReference property,
			Object value) {
		if (value instanceof ILiteral) {
			ILiteral literal = (ILiteral) value;
			return literal.getLabel();
		}
		return value != null ? value.toString() : "";
	}

	protected boolean isAbstractType(URI literalType) {
		return OWL.TYPE_THING.equals(literalType)
				|| RDFS.TYPE_RESOURCE.equals(literalType)
				|| RDFS.TYPE_LITERAL.equals(literalType)
				|| XMLSCHEMA.TYPE_ANYSIMPLETYPE.equals(literalType)
				|| XMLSCHEMA.TYPE_ANYTYPE.equals(literalType);
	}

	@Override
	public ICommand convertValueFromEditor(Object editorValue, IEntity subject,
			IReference property, Object oldValue) {
		ILiteral newLiteral;
		if (editorValue instanceof ILiteral) {
			newLiteral = (ILiteral) editorValue;
		} else {
			URI literalType = null;
			String literalLanguage = null;
			if (oldValue instanceof ILiteral) {
				literalType = ((ILiteral) oldValue).getDatatype();
				literalLanguage = ((ILiteral) oldValue).getLanguage();
			} else {
				IExtendedIterator<? extends IReference> ranges = subject
						.getEntityManager().find(property, IProperty.class)
						.getNamedRanges(subject, false);
				while (ranges.hasNext() && literalType == null) {
					URI type = ranges.next().getURI();
					if (!isAbstractType(type)) {
						literalType = type;
					}
				}
				ranges.close();
			}
			if (isAbstractType(literalType)) {
				literalType = null;
			}
			newLiteral = subject.getEntityManager().createLiteral(
					(String) editorValue, literalType, literalLanguage);
			if (literalType != null) {
				// try to convert literal to given type
				Object result = subject.getEntityManager().toInstance(
						newLiteral);
				if (result instanceof ILiteral) {
					return new UnexecutableCommand(new Status(IStatus.ERROR,
							KommaEditPlugin.PLUGIN_ID,
							"Invalid literal value for type " + literalType));
				}
			}
		}
		if (!newLiteral.equals(oldValue)) {
			return new IdentityCommand(newLiteral);
		}
		return null;
	}
}
