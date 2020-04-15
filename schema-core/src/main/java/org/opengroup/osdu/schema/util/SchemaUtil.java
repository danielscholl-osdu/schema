package org.opengroup.osdu.schema.util;


import org.json.JSONException;
import org.opengroup.osdu.schema.exceptions.BadRequestException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.stereotype.Component;

@Component
public class SchemaUtil {

    public void checkBreakingChange(String newSchema, String originalSchema) throws BadRequestException {

        try {
            JSONAssert.assertEquals(originalSchema, newSchema, JSONCompareMode.LENIENT);
        } catch (AssertionError e) {
            throw new BadRequestException("Breaking changes found, please change schema major version");
        } catch (JSONException e) {
            throw new BadRequestException("Bad Input, invalid json");
        }
    }
}
