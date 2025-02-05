package org.opengroup.osdu.schema.util;

import static io.restassured.RestAssured.given;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opengroup.osdu.schema.constants.HttpConnection;
import org.opengroup.osdu.schema.stepdefs.model.HttpRequest;
import org.opengroup.osdu.schema.stepdefs.model.HttpResponse;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class RestAssuredClient implements HttpClient {
    RestAssuredClient() {
        // Due to a known issue in RestAssured the following deprecated methods has to
        // be used
        // https://github.com/rest-assured/rest-assured/issues/497#issuecomment-143404851
        RestAssured.config = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam(HttpConnection.HTTP_CONNECTION_TIMEOUT, HttpConnection.CONNECTION_TIMEOUT_IN_MILLISECONDS)
                .setParam(HttpConnection.HTTP_SOCKET_TIMEOUT, HttpConnection.CONNECTION_TIMEOUT_IN_MILLISECONDS))
                .redirect(RedirectConfig.redirectConfig().followRedirects(HttpConnection.FOLLOW_REDIRECTS));
        RestAssured.urlEncodingEnabled = false;
    }

    private RequestSpecification getRequestSpecification(HttpRequest httpRequest) {
        return new RequestSpecBuilder().setBaseUri(httpRequest.getUrl()).addHeaders(httpRequest.getRequestHeaders())
                .addQueryParams(httpRequest.getQueryParams()).addPathParams(httpRequest.getPathParams())
                .addFilter(new RequestLoggingFilter(LogDetail.URI)).addFilter(new ResponseLoggingFilter(LogDetail.BODY))
                .build();
    }

    private HttpResponse getHttpResponse(Response response) {
        final Map<String, List<String>> responseHeaders = response.getHeaders().asList().stream().collect(
                Collectors.groupingBy(Header::getName, Collectors.mapping(Header::getValue, Collectors.toList())));

        return HttpResponse.builder().code(response.getStatusCode()).responseHeaders(responseHeaders)
                .body(response.body().asString()).build();
    }

    @Override
    public HttpResponse send(HttpRequest httpRequest) {
        RequestSpecification requestSpecification = getRequestSpecification(httpRequest);

        if (httpRequest.getBody() != null) {
            requestSpecification.body(httpRequest.getBody());
        }

        try {
            Response response = RestAssured.given(requestSpecification).request(httpRequest.getHttpMethod()).then().extract()
                    .response();
            return getHttpResponse(response);
        } catch (Exception e) {
            return HttpResponse.builder().exception(e).build();
        }
    }

    @Override
    public <T> T send(HttpRequest httpRequest, Class<T> classOfT) {
        HttpResponse httpResponse = send(httpRequest);
        return JsonUtils.fromJson(httpResponse.getBody(), classOfT);
    }
}
