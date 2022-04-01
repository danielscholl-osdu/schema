package org.opengroup.osdu.schema.validation.version.handler.patch;

import java.util.List;
import java.util.Set;

import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.validation.version.SchemaValidationType;
import org.opengroup.osdu.schema.validation.version.handler.SchemaValidationHandler;
import org.opengroup.osdu.schema.validation.version.model.SchemaBreakingChanges;
import org.opengroup.osdu.schema.validation.version.model.SchemaHandlerVO;
import org.opengroup.osdu.schema.validation.version.model.SchemaPatch;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(90)
@Component
public class AddOperationHandler implements SchemaValidationHandler{

	private SchemaValidationHandler nextHandler;

	@Override
	public void setNextHandler(SchemaValidationHandler nextHandler) {
		this.nextHandler = nextHandler;
	}

	@Override
	public void compare(SchemaHandlerVO schemaDiff, SchemaPatch patch, List<SchemaBreakingChanges> schemaBreakingChanges, Set<String> processedArrayPath) throws ApplicationException {

		if(schemaDiff.getValidationType() == getValidationType() 
				&& SchemaConstants.OP_ADD.equals(patch.getOp())) {

			String attribut = patch.getPath().charAt(0) == '/' ? patch.getPath().substring(1) : patch.getPath();

			if(isRefIdPresent(schemaDiff, attribut))
				return;
			schemaBreakingChanges.add(new SchemaBreakingChanges(patch, "Adding attributes at Patch level is not permitted."));
		}else if(null != nextHandler){
			this.nextHandler.compare(schemaDiff, patch, schemaBreakingChanges, processedArrayPath);
		}

	}

	@Override
	public SchemaValidationType getValidationType() {
		return SchemaValidationType.PATCH;
	}

	private boolean isRefIdPresent(SchemaHandlerVO schemaDiff, String attributeName) {
		if(null == schemaDiff.getChangedRefIds())
			return false;
		return schemaDiff.getChangedRefIds().containsValue(attributeName);
	}

}