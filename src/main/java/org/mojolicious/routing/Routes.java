package org.mojolicious.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Main routing system for Mojolicious Java.
 * Manages route registration, matching, and dispatch.
 */
public class Routes {
    
    private static final Logger log = LoggerFactory.getLogger(Routes.class);
    
    private final List<Route> routes;
    private final Map<String, Route> namedRoutes;
    private final Map<String, Pattern> types;
    private final Map<String, Route> cache;
    private final List<String> namespaces;
    
    /**
     * Create a new Routes instance.
     */
    public Routes() {
        this.routes = new ArrayList<>();
        this.namedRoutes = new HashMap<>();
        this.types = new HashMap<>();
        this.cache = new ConcurrentHashMap<>();
        this.namespaces = new ArrayList<>();
        
        // Default type: num matches digits
        types.put("num", Pattern.compile("[0-9]+"));
    }
    
    /**
     * Register a route for GET requests.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route get(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.singletonList("GET"), handler);
    }
    
    /**
     * Register a route for POST requests.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route post(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.singletonList("POST"), handler);
    }
    
    /**
     * Register a route for PUT requests.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route put(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.singletonList("PUT"), handler);
    }
    
    /**
     * Register a route for DELETE requests.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route delete(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.singletonList("DELETE"), handler);
    }
    
    /**
     * Register a route for PATCH requests.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route patch(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.singletonList("PATCH"), handler);
    }
    
    /**
     * Register a route for OPTIONS requests.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route options(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.singletonList("OPTIONS"), handler);
    }
    
    /**
     * Register a route for any HTTP method.
     * 
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route any(String pattern, Consumer<Context> handler) {
        return addRoute(pattern, Collections.emptyList(), handler);
    }
    
    /**
     * Register a route for specific HTTP methods.
     * 
     * @param methods the HTTP methods
     * @param pattern the URL pattern
     * @param handler the handler function
     * @return the created route
     */
    public Route any(List<String> methods, String pattern, Consumer<Context> handler) {
        return addRoute(pattern, methods, handler);
    }
    
    /**
     * Add a route to the routing table.
     * 
     * @param pattern the URL pattern
     * @param methods the HTTP methods
     * @param handler the handler function
     * @return the created route
     */
    private Route addRoute(String pattern, List<String> methods, Consumer<Context> handler) {
        Route route = new Route(pattern, methods, handler);
        routes.add(route);
        
        // Index by name
        namedRoutes.put(route.getName(), route);
        
        log.trace("Added route: {} {} -> {}", methods.isEmpty() ? "*" : methods, pattern, route.getName());
        
        return route;
    }
    
    /**
     * Match a request to a route.
     * 
     * @param method the HTTP method
     * @param path the request path
     * @return the matched route, or null if no match
     */
    public Route match(String method, String path) {
        // Check cache first
        String cacheKey = method + ":" + path;
        Route cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Find matching route
        for (Route route : routes) {
            if (route.matches(method, path)) {
                cache.put(cacheKey, route);
                return route;
            }
        }
        
        return null;
    }
    
    /**
     * Find a route by name.
     * 
     * @param name the route name
     * @return the route, or null if not found
     */
    public Route lookup(String name) {
        return namedRoutes.get(name);
    }
    
    /**
     * Get all registered routes.
     * 
     * @return the routes list
     */
    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }
    
    /**
     * Add a placeholder type.
     * 
     * @param name the type name
     * @param pattern the regex pattern
     * @return this routes for chaining
     */
    public Routes addType(String name, Pattern pattern) {
        types.put(name, pattern);
        return this;
    }
    
    /**
     * Add a placeholder type.
     * 
     * @param name the type name
     * @param pattern the regex pattern string
     * @return this routes for chaining
     */
    public Routes addType(String name, String pattern) {
        return addType(name, Pattern.compile(pattern));
    }
    
    /**
     * Get the registered types.
     * 
     * @return the types map
     */
    public Map<String, Pattern> getTypes() {
        return types;
    }
    
    /**
     * Add a namespace for controller lookup.
     * 
     * @param namespace the namespace
     * @return this routes for chaining
     */
    public Routes addNamespace(String namespace) {
        namespaces.add(namespace);
        return this;
    }
    
    /**
     * Get the namespaces.
     * 
     * @return the namespaces list
     */
    public List<String> getNamespaces() {
        return namespaces;
    }
    
    /**
     * Clear the route cache.
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * Get the number of registered routes.
     * 
     * @return the route count
     */
    public int size() {
        return routes.size();
    }
    
    /**
     * Check if there are any registered routes.
     * 
     * @return true if no routes are registered
     */
    public boolean isEmpty() {
        return routes.isEmpty();
    }
}
