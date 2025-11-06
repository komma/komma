package net.enilink.komma.edit.properties;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.common.command.CommandResult;
import net.enilink.komma.common.command.ICommand;
import net.enilink.komma.common.command.IdentityCommand;
import net.enilink.komma.common.command.SimpleCommand;
import net.enilink.komma.core.IEntity;
import net.enilink.komma.core.IEntityManager;
import net.enilink.komma.core.ILiteral;
import net.enilink.komma.core.IReference;
import net.enilink.komma.core.IStatement;
import net.enilink.komma.core.Literal;
import net.enilink.komma.core.Statement;
import net.enilink.komma.core.URI;
import net.enilink.vocab.owl.OWL;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

public class LiteralLangTypeEditingSupport extends ResourceEditingSupport {
	public LiteralLangTypeEditingSupport(IAdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	@Override
	public IProposalSupport getProposalSupport(Object element) {
		IStatement stmt = (IStatement) element;
		return super.getProposalSupport(new Statement(stmt.getSubject(), null,
				null));
	}

	@Override
	public Object getEditorValue(Object element) {
		IStatement stmt = (IStatement) element;
		Object value = stmt.getObject();
		if (value instanceof ILiteral) {
			String lang = ((ILiteral) value).getLanguage();
			if (lang != null) {
				return "@" + lang;
			}
			URI type = ((ILiteral) value).getDatatype();
			if (type != null) {
				return getLabel(((IEntity) stmt.getSubject())
						.getEntityManager().find(type));
			}
		}
		return "";
	}

	@Override
	public ICommand convertEditorValue(Object editorValue,
			IEntityManager entityManager, Object element) {
		IStatement stmt = (IStatement) element;
		final ILiteral oldLiteral = (ILiteral) stmt.getObject();
		String newValue = editorValue.toString().trim();
		if (newValue.isEmpty()) {
			return new IdentityCommand(new Literal(oldLiteral.getLabel()));
		} else if (newValue.startsWith("@")) {
			return new IdentityCommand(new Literal(oldLiteral.getLabel(),
					newValue.substring(1)));
		}
		final ICommand command = super.convertEditorValue(editorValue,
				entityManager, new Statement(
						entityManager.find(OWL.TYPE_THING), null, null));
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
