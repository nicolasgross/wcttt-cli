package de.nicolasgross.wcttt.core.algorithms;

public class ParameterValue<T> {

	private ParameterDefinition definition;
	private T value;

	public ParameterValue(ParameterDefinition definition, T value) {
		if (definition == null || value == null) {
			throw new IllegalArgumentException("Parameters 'definition' and " +
					"'value' must not be null");
		}
		this.definition = definition;
		this.value = value;
	}

	public ParameterDefinition getDefinition() {
		return definition;
	}

	public T getValue() {
		return value;
	}
}
