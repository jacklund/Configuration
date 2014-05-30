package net.geekheads.configuration;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.MethodSetter;

/**
 * Command line parser for configuration. Uses <a href="http://args4j.kohsuke.org/">args4j</a> for
 * the command-line parsing. Setters on the classes to be configured should be decorated with the
 * args4j {@link Option} annotation.
 *  
 * @author Jack Lund
 *
 */
public class CommandLineParser {
	// Container class used below
	private static class MethodPlusAnnotation {
		private Method method;
		private Option annotation;

		public MethodPlusAnnotation(Method m, Option a) {
			method = m;
			annotation = a;
		}

		public Method getMethod() {
			return method;
		}
		
		public Option getAnnotation() {
			return annotation;
		}
	}
	
	// args4j command-line parser. Note, we pass in a dummy object, because we have to
	private CmdLineParser parser = new CmdLineParser(new Object());

	/**
	 * Constructor. Takes a list of objects to be configured via the command line. Note that
	 * the parser navigates the object hierarchy, using the getters, to find all the configurable items.
	 * @param objects list of configurable objects
	 * @throws IllegalAccessException if one of the getters is not accessible
	 * @throws IllegalArgumentException probably shouldn't be thrown
	 * @throws InvocationTargetException if a getter throws an exception on invocation
	 */
	public CommandLineParser(Object... objects) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		for (Object object : objects) {
			Class<?> c = object.getClass();
			findOptionsInSubtree(object, c);
		}
	}
	
	/**
	 * Find all the options in the object subtree of the passed-in object.
	 * @param object the object to search
	 * @param c its class
	 * @throws IllegalAccessException If one of the getters is not accessible
	 * @throws InvocationTargetException if one of the getters throws an exception
	 */
	private void findOptionsInSubtree(Object object, Class<?> c)
			throws IllegalAccessException, InvocationTargetException {
		setOptions(object, c);
		Method[] getters = findGetters(c);
		for (Method m : getters) {
			Class<?> returnType = m.getReturnType();
			if (!returnType.isArray() && !returnType.isPrimitive() &&
					returnType != String.class && returnType != Class.class &&
					returnType != ClassLoader.class) {
				Object subobject = m.invoke(object, new Object[]{});
				if (subobject != null) findOptionsInSubtree(subobject, returnType);
			}
		}
	}

	/**
	 * Find all the getters for the passed-in class
	 * @param c class to find the getters for
	 * @return a list of the getters
	 */
	private Method[] findGetters(Class<?> c) {
		ArrayList<Method> list = new ArrayList<Method>();
		Method[] methods = c.getMethods();
		for (Method m : methods) {
			String name = m.getName();
			if (name.startsWith("get") && !name.equals("getClass") && m.getParameterTypes().length == 0) {
				list.add(m);
			}
		}
		
		return list.toArray(new Method[0]);
	}

	/**
	 * Add all the options found on the class to the parser
	 * @param object the object to search
	 * @param c the class for the object
	 */
	private void setOptions(Object object, Class<?> c) {
		MethodPlusAnnotation[] annotated = getAnnotatedSetters(c);
		for (MethodPlusAnnotation m : annotated) {
			MethodSetter setter = new MethodSetter(parser, object, m.getMethod());
			parser.addOption(setter, m.getAnnotation());
		}
	}

	/**
	 * Find all the setters on the class which have been annotated with {@link @Option}.
	 * @param c the class
	 * @return a list of the methods plus their annotations
	 */
	private MethodPlusAnnotation[] getAnnotatedSetters(Class<?> c) {
		Method[] methods = c.getMethods();
		ArrayList<MethodPlusAnnotation> list = new ArrayList<MethodPlusAnnotation>();
		for (Method m : methods) {
			Option annotation = m.getAnnotation(Option.class);
			if (m.getParameterTypes().length == 1 &&
					annotation != null) {
				MethodPlusAnnotation mpa = new MethodPlusAnnotation(m, annotation);
				list.add(mpa);
			}
		}
		
		return list.toArray(new MethodPlusAnnotation[0]);
	}

	/**
	 * Get the args4j parser
	 * @return
	 */
	public CmdLineParser getParser() {
		return parser;
	}

	/**
	 * Parse the given command line, populating the passed in objects with the values
	 * @param args arguments from the command line
	 * @throws ConfigurationException if there's a configuration issue
	 */
	public void parse(String... args) throws ConfigurationException {
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			e.printStackTrace();
			throw new ConfigurationException(e.getMessage());
		}
	}

	/**
	 * Print the usage information on the given stream
	 * @param command the name of the top-level command
	 * @param stream the stream
	 */
	public void usage(String command, PrintStream stream) {
		stream.println("Usage: " + command + " arguments...");
		parser.printUsage(stream);
	}
}
