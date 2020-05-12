package org.opengroup.osdu.schema.validation;

import static org.junit.Assert.assertEquals;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
public class SchemaValidatorTest {

    @InjectMocks
    SchemaValidator schemaValidator;

    @Mock
    ConstraintValidatorContext constraintValidatorContext;

    private Object schemaInputObject;
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void test_JsonArrayAsInput_Schema() throws JsonMappingException, JsonProcessingException {
        schemaInputObject = mapper.readValue("[{}]", Object.class);
        assertEquals(false, schemaValidator.isValid(schemaInputObject, constraintValidatorContext));
    }

    @Test
    public void test_JsonObjectAsInput_Schema() throws JsonMappingException, JsonProcessingException {
        schemaInputObject = mapper.readValue("{}", Object.class);
        assertEquals(true, schemaValidator.isValid(schemaInputObject, constraintValidatorContext));
    }

}