package org.opengroup.osdu.schema.validation;

import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.exceptions.SchemaVersionException;

public interface SchemaVersionValidator {
	
	public SchemaVersionValidatorType getType();
	
	public void validate(String oldSchema, String newSchema) throws SchemaVersionException, ApplicationException;
	
	
}
