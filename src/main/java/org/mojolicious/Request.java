package org.mojolicious;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.Attribute;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Request {
    private Parameters queryParams;
    private Parameters bodyParams;
    private HttpRequest nettyRequest;

    public Request() {
        this.queryParams = new Parameters();
        this.bodyParams = new Parameters();
    }

    public Request(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        this.queryParams = new Parameters();
        this.bodyParams = new Parameters();
        parseQueryParameters();
    }

    public Parameters params() {
        Parameters merged = bodyParams.clone();
        merged.append(queryParams);
        return merged;
    }

    public String param(String name) {
        return params().param(name);
    }

    public List<String> everyParam(String name) {
        return params().everyParam(name);
    }

    public Parameters queryParams() {
        return queryParams;
    }

    public Parameters bodyParams() {
        return bodyParams;
    }

    public void setQueryParams(Parameters queryParams) {
        this.queryParams = queryParams;
    }

    public void setBodyParams(Parameters bodyParams) {
        this.bodyParams = bodyParams;
    }

    public void parseQueryString(String queryString) {
        if (queryString != null && !queryString.isEmpty()) {
            this.queryParams = new Parameters(queryString);
        }
    }

    public void parseBodyParameters(String body, String contentType) {
        if (body == null || body.isEmpty()) {
            return;
        }

        if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
            this.bodyParams = new Parameters(body);
        }
    }

    private void parseQueryParameters() {
        if (nettyRequest == null) {
            return;
        }

        try {
            QueryStringDecoder queryDecoder = new QueryStringDecoder(nettyRequest.uri(), StandardCharsets.UTF_8);
            Map<String, List<String>> params = queryDecoder.parameters();
            
            Parameters queryParameters = new Parameters();
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                for (String value : entry.getValue()) {
                    queryParameters.append(entry.getKey(), value);
                }
            }
            this.queryParams = queryParameters;
        } catch (Exception e) {
            this.queryParams = new Parameters();
        }
    }

    public void parseMultipartFormData(HttpRequest request, String content) {
        if (request == null || content == null) {
            return;
        }

        try {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
                new DefaultHttpDataFactory(false), 
                request
            );

            Parameters bodyParameters = new Parameters();
            
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    Attribute attribute = (Attribute) data;
                    try {
                        bodyParameters.append(attribute.getName(), attribute.getValue());
                    } catch (IOException e) {
                    }
                }
            }
            
            this.bodyParams = bodyParameters;
            decoder.destroy();
        } catch (Exception e) {
            this.bodyParams = new Parameters();
        }
    }

    public HttpRequest getNettyRequest() {
        return nettyRequest;
    }

    public void setNettyRequest(HttpRequest nettyRequest) {
        this.nettyRequest = nettyRequest;
        parseQueryParameters();
    }
}
