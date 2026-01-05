package org.mojolicious.routing;

import org.mojolicious.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Request context for handling HTTP requests.
 * Provides access to request data and methods for building responses.
 */
public class Context {
    
    private static final Logger log = LoggerFactory.getLogger(Context.class);
    
    private final Application app;
    private final HttpServerRequest request;
    private final HttpServerResponse response;
    private final Route route;
    private final Map<String, Object> stash;
    private final Map<String, String> params;
    
    private String responseBody;
    private String contentType;
    private int statusCode;
    private boolean rendered;
    
    /**
     * Create a new context.
     * 
     * @param app the application
     * @param request the HTTP request
     * @param response the HTTP response
     * @param route the matched route
     */
    public Context(Application app, HttpServerRequest request, HttpServerResponse response, Route route) {
        this.app = app;
        this.request = request;
        this.response = response;
        this.route = route;
        this.stash = new HashMap<>();
        this.statusCode = 200;
        this.rendered = false;
        this.contentType = "text/html; charset=utf-8";
        
        // Extract path parameters
        String path = request.uri();
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        this.params = route.extractParams(path);
        
        // Copy defaults to stash
        stash.putAll(route.getDefaults());
        
        // Copy app defaults to stash
        stash.putAll(app.defaults());
    }
    
    /**
     * Get the application.
     * 
     * @return the application
     */
    public Application app() {
        return app;
    }
    
    /**
     * Get the HTTP request.
     * 
     * @return the request
     */
    public HttpServerRequest request() {
        return request;
    }
    
    /**
     * Get the HTTP response.
     * 
     * @return the response
     */
    public HttpServerResponse response() {
        return response;
    }
    
    /**
     * Get the matched route.
     * 
     * @return the route
     */
    public Route route() {
        return route;
    }
    
    /**
     * Get the request method.
     * 
     * @return the HTTP method
     */
    public String method() {
        return request.method().name();
    }
    
    /**
     * Get the request path.
     * 
     * @return the path
     */
    public String path() {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        return queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
    }
    
    /**
     * Get a path parameter.
     * 
     * @param name the parameter name
     * @return the parameter value, or null if not found
     */
    public String param(String name) {
        return params.get(name);
    }
    
    /**
     * Get a path parameter with a default value.
     * 
     * @param name the parameter name
     * @param defaultValue the default value
     * @return the parameter value, or the default if not found
     */
    public String param(String name, String defaultValue) {
        return params.getOrDefault(name, defaultValue);
    }
    
    /**
     * Get all path parameters.
     * 
     * @return the parameters map
     */
    public Map<String, String> params() {
        return params;
    }
    
    /**
     * Get a query parameter.
     * 
     * @param name the parameter name
     * @return the parameter value, or null if not found
     */
    public String query(String name) {
        return request.param(name);
    }
    
    /**
     * Get a request header.
     * 
     * @param name the header name
     * @return the header value, or null if not found
     */
    public String header(String name) {
        return request.requestHeaders().get(name);
    }
    
    /**
     * Get a stash value.
     * 
     * @param key the stash key
     * @return the value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T stash(String key) {
        return (T) stash.get(key);
    }
    
    /**
     * Set a stash value.
     * 
     * @param key the stash key
     * @param value the value
     * @return this context for chaining
     */
    public Context stash(String key, Object value) {
        stash.put(key, value);
        return this;
    }
    
    /**
     * Get the entire stash.
     * 
     * @return the stash map
     */
    public Map<String, Object> stash() {
        return stash;
    }
    
    /**
     * Render a plain text response.
     * 
     * @param text the text content
     * @return this context for chaining
     */
    public Context text(String text) {
        this.responseBody = text;
        this.contentType = "text/plain; charset=utf-8";
        this.rendered = true;
        return this;
    }
    
    /**
     * Render an HTML response.
     * 
     * @param html the HTML content
     * @return this context for chaining
     */
    public Context html(String html) {
        this.responseBody = html;
        this.contentType = "text/html; charset=utf-8";
        this.rendered = true;
        return this;
    }
    
    /**
     * Render a JSON response.
     * 
     * @param json the JSON content
     * @return this context for chaining
     */
    public Context json(String json) {
        this.responseBody = json;
        this.contentType = "application/json; charset=utf-8";
        this.rendered = true;
        return this;
    }
    
    /**
     * Set the response status code.
     * 
     * @param status the HTTP status code
     * @return this context for chaining
     */
    public Context status(int status) {
        this.statusCode = status;
        return this;
    }
    
    /**
     * Set a response header.
     * 
     * @param name the header name
     * @param value the header value
     * @return this context for chaining
     */
    public Context header(String name, String value) {
        response.header(name, value);
        return this;
    }
    
    /**
     * Redirect to another URL.
     * 
     * @param url the URL to redirect to
     * @return this context for chaining
     */
    public Context redirect(String url) {
        return redirect(url, 302);
    }
    
    /**
     * Redirect to another URL with a specific status code.
     * 
     * @param url the URL to redirect to
     * @param status the HTTP status code (301, 302, 303, 307, 308)
     * @return this context for chaining
     */
    public Context redirect(String url, int status) {
        this.statusCode = status;
        response.header("Location", url);
        this.responseBody = "";
        this.rendered = true;
        return this;
    }
    
    /**
     * Send a "not found" response.
     * 
     * @return this context for chaining
     */
    public Context notFound() {
        return status(404).html(
            "<!DOCTYPE html>\n<html>\n<head><title>404 Not Found</title></head>\n" +
            "<body><h1>404 Not Found</h1></body>\n</html>"
        );
    }
    
    /**
     * Send an error response.
     * 
     * @param status the HTTP status code
     * @param message the error message
     * @return this context for chaining
     */
    public Context error(int status, String message) {
        return status(status).html(
            String.format(
                "<!DOCTYPE html>\n<html>\n<head><title>%d %s</title></head>\n" +
                "<body><h1>%d %s</h1></body>\n</html>",
                status, message, status, message
            )
        );
    }
    
    /**
     * Check if a response has been rendered.
     * 
     * @return true if rendered
     */
    public boolean isRendered() {
        return rendered;
    }
    
    /**
     * Finish the response and send it to the client.
     * 
     * @return Mono that completes when the response is sent
     */
    public Mono<Void> finish() {
        if (!rendered) {
            // Nothing rendered - send empty response
            return response
                .status(statusCode)
                .send()
                .then();
        }
        
        return response
            .status(statusCode)
            .header("Content-Type", contentType)
            .sendString(Mono.just(responseBody != null ? responseBody : ""))
            .then();
    }
    
    /**
     * Get the response body.
     * 
     * @return the response body
     */
    public String getResponseBody() {
        return responseBody;
    }
    
    /**
     * Get the content type.
     * 
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Get the status code.
     * 
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
