package net.enilink.komma.edit.ui.properties.internal.wizards;

import org.eclipse.swt.graphics.Image;

import net.enilink.komma.common.adapter.IAdapterFactory;
import net.enilink.komma.edit.ui.provider.AdapterFactoryLabelProvider;

/**
 * 
 * Wrapper for storing a value along with its label.
 * 
 */
public class ItemUtil {
	static class LabeledItem {
		Object value;
		String label;

		public LabeledItem(Object value, String label) {
			this.value = value;
			this.label = label;
		}
	}

	static class LabelProvider extends AdapterFactoryLabelProvider {
		public LabelProvider(IAdapterFactory adapterFactory) {
			super(adapterFactory);
		}

		public String getText(Object object) {
			if (object instanceof LabeledItem) {
				return ((LabeledItem) object).label;
			}
			return super.getText(object);
		}

		public Image getImage(Object object) {
			return super.getImage(unwrap(object));
		}
	}

	static Object unwrap(Object itemOrValue) {
		return itemOrValue instanceof LabeledItem ? ((LabeledItem) itemOrValue).value
				: itemOrValue;
	}
}
