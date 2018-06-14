package javax.el;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

public final class BeanElResolverBeanProperties {
	private final Map<String, BeanElResolverBeanProperty> properties;

	private final Class<?> type;

	public BeanElResolverBeanProperties(Class<?> type) throws ELException {
		this.type = type;
		this.properties = new HashMap<String, BeanElResolverBeanProperty>();
		try {
			BeanInfo info = Introspector.getBeanInfo(this.type);
			PropertyDescriptor[] pds = info.getPropertyDescriptors();
			for (int i = 0; i < pds.length; i++) {
				this.properties.put(pds[i].getName(),
						new BeanElResolverBeanProperty(type, pds[i]));
			}
		} catch (IntrospectionException ie) {
			throw new ELException(ie);
		}
	}

	public BeanElResolverBeanProperty get(ELContext ctx, String name) {
		BeanElResolverBeanProperty property = this.properties.get(name);
		if (property == null) {
			throw new PropertyNotFoundException(Util.message(ctx,
					"propertyNotFound", type.getName(), name));
		}
		return property;
	}

	public BeanElResolverBeanProperty getBeanProperty(String name) {
		return get(null, name);
	}

	public Class<?> getType() {
		return type;
	}
}