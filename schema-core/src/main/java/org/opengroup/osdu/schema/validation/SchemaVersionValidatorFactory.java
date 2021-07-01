package org.opengroup.osdu.schema.validation;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchemaVersionValidatorFactory {

	@Autowired
	private List<SchemaVersionValidator> schemaVersionValidator;

	public  SchemaVersionValidator getSchemaVersionValidator(SchemaVersionValidatorType validatorType) {
		Optional<SchemaVersionValidator> schemaValidator = schemaVersionValidator.stream().filter(validator -> validator.getType().equals(validatorType))
				.findAny();

		return schemaValidator.orElseThrow(() ->  new RuntimeException("Unknown validator type: " + validatorType));
	}
}
