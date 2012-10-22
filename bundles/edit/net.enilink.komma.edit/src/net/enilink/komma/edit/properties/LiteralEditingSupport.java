package net.enilink.komma.edit.properties;

import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IValue;
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
		URI literalType = null;
		String literalLanguage = null;
		if (oldValue instanceof ILiteral) {
			literalType = ((ILiteral) oldValue).getDatatype();
			literalLanguage = ((ILiteral) oldValue).getLanguage();
		}
		IValue newLiteral = subject.getEntityManager().createLiteral(
				(String) editorValue, literalType, literalLanguage);
		if (!newLiteral.equals(oldValue)) {
			return new IdentityCommand(newLiteral);
		}
		return null;
	}

}
