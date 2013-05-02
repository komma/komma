package net.enilink.komma.edit.properties;

import net.enilink.commons.iterator.IExtendedIterator;
import net.enilink.vocab.rdfs.RDFS;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.concepts.IProperty;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.URI;

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
				if (ranges.hasNext()) {
					literalType = ranges.next().getURI();
				}
				ranges.close();
			}
			if (RDFS.TYPE_LITERAL.equals(literalType)) {
				literalType = null;
			}
			newLiteral = subject.getEntityManager().createLiteral(
					(String) editorValue, literalType, literalLanguage);
		}
		if (!newLiteral.equals(oldValue)) {
			return new IdentityCommand(newLiteral);
		}
		return null;
	}

}
