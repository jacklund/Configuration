package net.geekheads.configuration;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonRootName;

public class JsonConfigFileParser extends ConfigFileParser {
	private JsonNode tree;

	public JsonConfigFileParser(File file) throws FileNotFoundException, ConfigurationException {
		super(file);
	}

	public JsonConfigFileParser(InputStream inputStream) throws ConfigurationException {
		super(inputStream);
	}

	public JsonConfigFileParser(String filename) throws FileNotFoundException, ConfigurationException {
		super(filename);
	}
	
	@Override
	protected void parse(InputStream inputStream) throws ConfigurationException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			tree = mapper.readTree(inputStream);
		} catch (JsonProcessingException e) {
			throw new ConfigurationException("Error reading JSON", e);
		} catch (IOException e) {
			throw new ConfigurationException("Error reading JSON", e);
		}
	}

	@Override
	public void configure(Object... objects) throws ConfigurationException {
		for (Object o : objects) {
			Class<? extends Object> c = o.getClass();
			String nodeName;
			JsonNode node;
			
			// Check for @JsonRootName annotation on the class. Use that as the
			// node name for the class
			JsonRootName annotation = c.getAnnotation(JsonRootName.class);
			if (annotation != null) {
				nodeName = annotation.value();
				node = tree.get(nodeName);
				if (node == null) {
					throw new ConfigurationException("Couldn't find configuration for " + c.getName() +
							", was expecting JSON node named '" + nodeName + "'");
				}
			} else {
				// No annotation. Try using the simple node name, and, failing that,
				// the fully-qualified class name
				nodeName = getSimpleNodeName(c);
				node = tree.get(nodeName);
				if (node == null) {
					node = tree.get(c.getName());
					if (node == null) {
						throw new ConfigurationException("Couldn't find configuration for " + c.getName());
					}
				}
			}
			update(o, node);
		}
	}

	private String getSimpleNodeName(Class<? extends Object> c) {
		String nodeName;
		String simpleName = c.getSimpleName();
		nodeName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
		return nodeName;
	}

	private void update(Object o, JsonNode node) throws ConfigurationException {
		PropertyDescriptor[] properties;
		Method method = null;
		try {
			// Get the class properties
			properties = Introspector.getBeanInfo(o.getClass()).getPropertyDescriptors();
			
			// Iterate over the fields and properties, and try to find a match
			Iterator<Entry<String, JsonNode>> fields = node.getFields();
			while (fields.hasNext()) {
				Entry<String, JsonNode> field = fields.next();
				String fieldName = field.getKey();
				JsonNode fieldNode = field.getValue();
				for (PropertyDescriptor p : properties) {
					// Default is to use the property name for the JSON node
					String jsonName = p.getName();

					// If there's an annotation on the property, use that instead
					JsonProperty annotation = findAnnotation(fieldNode, p);
					if (annotation != null) {
						jsonName = annotation.value();
					}
					
					// If they match, try to either set the value or traverse the subtree
					Class<?> type = p.getPropertyType();
					if (jsonName.equals(fieldName)) {
						method = p.getWriteMethod();
						
						// Go through and get the JSON node as a value, and
						// try to set it using the setter. Don't bother checking
						// if the types match, just let it throw an exception if they don't
						if (fieldNode.isBoolean()) {
							method.invoke(o, new Object[] {fieldNode.asBoolean()});
						} else if (fieldNode.isFloatingPointNumber()) {
							if (type == float.class) {
								method.invoke(o, new Object[] {(float) fieldNode.asDouble()});
							} else {
								method.invoke(o, new Object[] {fieldNode.asDouble()});
							}
						} else if (fieldNode.isInt()) {
							method.invoke(o, new Object[] {fieldNode.asInt()});
						} else if (fieldNode.isLong()) {
							method.invoke(o, new Object[] {fieldNode.asLong()});
						} else if (fieldNode.isNull()) {
							method.invoke(o, new Object[] {null});
						} else if (fieldNode.isTextual()) {
							method.invoke(o, new Object[] {fieldNode.asText()});
						} else if (fieldNode.isObject()) {
							// In the case of an object, we treat it as a subtree
							// Get the object value and recursively update it
							method = updateSubobject(o, fieldNode, p, type);
						} else {
							throw new ConfigurationException("Unknown JSON field type " + fieldNode);
						}
					} else if (type != Class.class && !type.isPrimitive() && !type.isArray() && !type.isEnum()) {
						// Traverse any subobjects
						Method readMethod = p.getReadMethod();
						if (readMethod != null) {
							Object subobject = readMethod.invoke(o, new Object[0]);
							if (subobject != null) {
								update(subobject, node);
							}
						}
					}
				}
			}
		} catch (IntrospectionException e) {
			throw new ConfigurationException("Error introspecting " + o.getClass().getName(), e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException("Error invoking method " + method.getName() + " on " + o.getClass().getName(), e);
		} catch (IllegalArgumentException e) {
			throw new ConfigurationException("Error invoking method " + method.getName() + " on " + o.getClass().getName(), e);
		} catch (InvocationTargetException e) {
			throw new ConfigurationException("Error invoking method " + method.getName() + " on " + o.getClass().getName(), e.getCause());
		}
	}

	private Method updateSubobject(Object parent, JsonNode fieldNode,
			PropertyDescriptor p, Class<?> subobjectType)
			throws IllegalAccessException, InvocationTargetException,
			ConfigurationException {
		Method method;
		method = p.getReadMethod();
		Object subobject = method.invoke(parent, new Object[0]);
		if (subobject != null) update(subobject, fieldNode);
		return method;
	}

	private JsonProperty findAnnotation(JsonNode fieldNode, PropertyDescriptor p) {
		JsonProperty annotation = null;
		if (fieldNode.isObject()) {
			// If this is an object, we treat it as a sub-tree of the object tree
			// Check if there's a getter, and if so, if there's an annotation on it
			Method getter = p.getReadMethod();
			if (getter != null) {
				annotation = getter.getAnnotation(JsonProperty.class);
			}
		}
		if (annotation == null) {
			// Check for an @JsonProperty annotation on the setter, and use
			// the value for that instead, if it's there
			Method setter = p.getWriteMethod();
			if (setter != null) {
				annotation = setter.getAnnotation(JsonProperty.class);
			}
		}
		return annotation;
	}
}
