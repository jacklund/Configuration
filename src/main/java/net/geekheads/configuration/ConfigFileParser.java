package net.geekheads.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public abstract class ConfigFileParser {
	public ConfigFileParser(String filename) throws FileNotFoundException, ConfigurationException {
		this(new File(filename));
	}

	public ConfigFileParser(File file) throws FileNotFoundException, ConfigurationException {
		this(new FileInputStream(file));
	}

	public ConfigFileParser(InputStream inputStream) throws ConfigurationException {
		parse(inputStream);
	}
	
	protected abstract void parse(InputStream inputStream) throws ConfigurationException;

	public abstract void configure(Object... objects) throws ConfigurationException;
}
