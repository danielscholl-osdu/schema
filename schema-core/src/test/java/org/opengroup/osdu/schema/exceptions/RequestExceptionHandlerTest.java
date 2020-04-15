package org.opengroup.osdu.schema.exceptions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.schema.errors.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.WebRequest;

@RunWith(SpringJUnit4ClassRunner.class)
public class RequestExceptionHandlerTest {

    @InjectMocks
    private RequestExceptionHandler handler;

    @Mock
    private WebRequest mockRequest;

    @Before
    public void setup() {
        handler = new RequestExceptionHandler();
        mockRequest = Mockito.mock(WebRequest.class);
        Mockito.when(mockRequest.getHeader("correlation-id")).thenReturn("sample-id");
    }

    @Test
    public void testNotFoundExHasCorrectMsg() {
        String errMsg = "Resource Not Found";
        NotFoundException ex = new NotFoundException(errMsg);
        ResponseEntity<Object> response = handler.handleNotFoundException(ex, mockRequest);
        Assert.assertNotNull(response);
        Error error = (Error) response.getBody();
        Assert.assertNotNull(error.getMessage());
        Assert.assertTrue(error.getMessage().equals(errMsg));
    }

    @Test
    public void testForbiddenOnAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("access denied");
        ResponseEntity<Object> response = handler.handleAccessDeniedException(ex, mockRequest);
        Assert.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void testNotFoundExHasCorrectStatus() {
        NotFoundException ex = new NotFoundException();
        ResponseEntity<Object> response = handler.handleNotFoundException(ex, mockRequest);
        Assert.assertNotNull(response);
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testCorrectCrlIdWithWebRequest() {
        Assert.assertEquals("sample-id : ", handler.getCorrelationId(mockRequest));
    }

    @Test
    public void testEmptyCrlIdWhenWebRequestIsNull() {
        WebRequest request = null;
        Assert.assertEquals("", handler.getCorrelationId(request));
    }

    @Test
    public void testCorrectHeaderIsExtracted() {
        String headerName = handler.extractMissingHeaderName("The requred header 'Authorization' is missing");
        Assert.assertEquals("Authorization", headerName);
    }

    @Test
    public void testFirstCorrectHeaderIsExtracted() {
        String headerName = handler
                .extractMissingHeaderName("The requred header 'Authorization', 'account' is missing");
        Assert.assertEquals("Authorization", headerName);
    }

    @Test
    public void testEmptyWhenNoMsg() {
        String headerName = handler.extractMissingHeaderName(null);
        Assert.assertEquals("", headerName);

        String headerName2 = handler.extractMissingHeaderName("");
        Assert.assertEquals("", headerName2);

        String headerName3 = handler.extractMissingHeaderName("Requred header is missing");
        Assert.assertEquals("", headerName3);
    }
}
