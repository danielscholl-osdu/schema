package org.opengroup.osdu.schema.validation.version.handler.patch;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.opengroup.osdu.schema.constants.SchemaConstants;
import org.opengroup.osdu.schema.exceptions.ApplicationException;
import org.opengroup.osdu.schema.util.SchemaUtil;
import org.opengroup.osdu.schema.validation.version.SchemaValidationType;
import org.opengroup.osdu.schema.validation.version.handler.SchemaValidationHandler;
import org.opengroup.osdu.schema.validation.version.model.SchemaBreakingChanges;
import org.opengroup.osdu.schema.validation.version.model.SchemaHandlerVO;
import org.opengroup.osdu.schema.validation.version.model.SchemaPatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(90)
@Component
public class AddOperationHandler implements SchemaValidationHandler{

	private SchemaValidationHandler nextHandler;

	@Autowired
	private SchemaUtil schemaUtil;

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
			else if (isPresentInTarget(schemaDiff, patch))
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

	private boolean isPresentInTarget(SchemaHandlerVO schemaDiff, SchemaPatch patch) {
		Pattern pattern = Pattern.compile(SchemaConstants.SCHEMA_KIND_REGEX);
		String path =  patch.getPath();
		String sourceField = StringUtils.substringAfterLast(path, "/");
		if(!pattern.matcher(path).matches() || !isAtRoot(path))
			return false;

		Iterator<String> fieldNameItr = schemaDiff.getTargetSchema().fieldNames();
		while(fieldNameItr.hasNext()) {
			String fieldName = fieldNameItr.next();
			if(pattern.matcher(fieldName).matches()) {
				if(schemaUtil.isValidSchemaVersionChange(fieldName, sourceField,schemaDiff.getValidationType())){
					return true;
				}
			}
		}

		return false;
	}

	private boolean isAtRoot(String path) {
		String subString [] = path.split("\\/");
		return subString.length == 2;
	}

}