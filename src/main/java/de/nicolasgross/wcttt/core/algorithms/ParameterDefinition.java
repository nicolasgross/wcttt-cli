package de.nicolasgross.wcttt.core.algorithms;

public class ParameterDefinition {

	private String name;
	private ParameterType type;

	public ParameterDefinition(String name, ParameterType type) {
		if (name == null) {
			throw new IllegalArgumentException("Parameter 'name' must not be " +
					"null");
		}
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public ParameterType getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ParameterDefinition)) {
			return false;
		} else if (obj == this) {
			return true;
		}

		ParameterDefinition other = (ParameterDefinition) obj;
		return this.name.equals(other.name) && this.type == other.type;
	}

	@Override
	public String toString() {
		return name + " [" + type.name() + "]";
	}
}
