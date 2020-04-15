// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.schema.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.http.ResponseHeaders;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResponseHeaderFIlter implements Filter {

    private static final String OPTIONS_STRING = "OPTIONS";

    @Autowired
    private DpsHeaders dpsHeaders;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        this.dpsHeaders.addCorrelationIdIfMissing();

        Map<String, List<Object>> standardHeaders = ResponseHeaders.STANDARD_RESPONSE_HEADERS;
        for (Map.Entry<String, List<Object>> header : standardHeaders.entrySet()) {
            httpResponse.addHeader(header.getKey(), header.getValue().toString());
        }
        httpResponse.addHeader(DpsHeaders.CORRELATION_ID, this.dpsHeaders.getCorrelationId());

        if (httpRequest.getMethod().equalsIgnoreCase(OPTIONS_STRING)) {
            httpResponse.setStatus(HttpStatus.SC_OK);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}