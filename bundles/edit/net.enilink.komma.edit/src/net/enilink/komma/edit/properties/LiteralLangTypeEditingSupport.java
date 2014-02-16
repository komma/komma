package net.enilink.komma.edit.properties;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.Literal;
import net.enilink.komma.core.URI;

public class LiteralLangTypeEditingSupport extends ResourceEditingSupport {
	public LiteralLangTypeEditingSupport(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public ProposalSupport getProposalSupport(IEntity subject,
			IReference property, Object value) {
		return super.getProposalSupport(subject, null, null);
	}

	@Override
	public Object getValueForEditor(IEntity subject, IReference property,
			Object value) {
		if (value instanceof ILiteral) {
			String lang = ((ILiteral) value).getLanguage();
			if (lang != null) {
				return "@" + lang;
			}
			URI type = ((ILiteral) value).getDatatype();
			if (type != null) {
				return getLabel(subject.getEntityManager().find(type));
			}
		}
		return "";
	}

	@Override
	public ICommand convertValueFromEditor(Object editorValue, IEntity subject,
			IReference property, Object oldValue) {
		final ILiteral oldLiteral = (ILiteral) oldValue;
		String newValue = editorValue.toString().trim();
		if (newValue.toString().isEmpty()) {
			return new IdentityCommand(new Literal(oldLiteral.getLabel()));
		} else if (newValue.startsWith("@")) {
			return new IdentityCommand(new Literal(oldLiteral.getLabel(),
					newValue.substring(1)));
		}
		final ICommand command = super.convertValueFromEditor(editorValue,
				subject, property, oldValue);
		return command == null ? null : command.compose(new SimpleCommand() {
			@Override
			protected CommandResult doExecuteWithResult(
					IProgressMonitor progressMonitor, IAdaptable info)
					throws ExecutionException {
				Object datatype = command.getCommandResult().getReturnValue();
				if (datatype instanceof IReference) {
					return CommandResult.newOKCommandResult(new Literal(
							oldLiteral.getLabel(), ((IReference) datatype)
									.getURI()));
				}
				return CommandResult.newOKCommandResult(new Literal(oldLiteral
						.getLabel()));
			}
		});
	}
}
