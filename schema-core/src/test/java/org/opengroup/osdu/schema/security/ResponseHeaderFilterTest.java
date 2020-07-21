package org.opengroup.osdu.schema.security;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
public class ResponseHeaderFilterTest {

    @InjectMocks
    ResponseHeaderFIlter responseHeaderFIlter;

    @Mock
    DpsHeaders dpsHeaders;

    @Mock
    HttpServletResponse httpServletResponse;

    @Mock
    HttpServletRequest httpServletRequest;

    @Mock
    FilterChain chain;

    @Test
    public void test_ResponseHeaderFiler_Options() throws IOException, ServletException {

        Mockito.when(httpServletRequest.getMethod()).thenReturn("OPTIONS");
        responseHeaderFIlter.doFilter(httpServletRequest, httpServletResponse, chain);
        assertNotNull(httpServletResponse);
        responseHeaderFIlter.destroy();

    }

    @Test
    public void test_ResponseHeaderFiler_Get() throws IOException, ServletException {

        Mockito.when(httpServletRequest.getMethod()).thenReturn("GET");
        responseHeaderFIlter.doFilter(httpServletRequest, httpServletResponse, chain);
        assertNotNull(httpServletResponse);
        responseHeaderFIlter.destroy();

    }

}
