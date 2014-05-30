package net.geekheads.configuration;

public class MissingRequiredValueException extends ConfigurationException {
	private static final long serialVersionUID = -7625293108650124066L;

	public MissingRequiredValueException(String value) {
		super("Configuration value \"" + value + "\" is required");
	}
}
