package net.enilink.komma.edit.ui.assist;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.parboiled.support.ParsingResult;

import net.enilink.komma.KommaCore;

public abstract class ReflectiveSemanticProposals implements
		ISemanticProposalProvider {
	class Proposal implements ISemanticProposal {
		Method m;

		public Proposal(Method m) {
			this.m = m;
			m.setAccessible(true);
		}

		@Override
		public IContentProposal[] compute(ParsingResult<?> result, int index,
				String prefix) {
			try {
				return (IContentProposal[]) m
						.invoke(ReflectiveSemanticProposals.this, result,
								index, prefix);
			} catch (Exception e) {
				KommaCore.log(e);
			}
			return null;
		}
	}

	protected Map<String, Proposal> proposals = new HashMap<String, Proposal>();

	public ReflectiveSemanticProposals() {
		for (Method m : getClass().getMethods()) {
			if (IContentProposal[].class.equals(m.getReturnType())) {
				Class<?>[] paramTypes = m.getParameterTypes();
				if (paramTypes.length == 3
						&& ParsingResult.class.equals(paramTypes[0])
						&& int.class.equals(paramTypes[1])
						&& String.class.equals(paramTypes[2])) {
					proposals.put(m.getName(), new Proposal(m));
				}
			}
		}
	}

	@Override
	public ISemanticProposal getProposal(String rule) {
		return proposals.get(rule);
	}

	protected IContentProposal[] compute(Method m, ParsingResult<?> result,
			int index, String prefix) {
		try {
			return (IContentProposal[]) m.invoke(
					ReflectiveSemanticProposals.this, result, index, prefix);
		} catch (Exception e) {
			KommaCore.log(e);
		}
		return null;
	}
}
