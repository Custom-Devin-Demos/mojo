package org.mojolicious.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single route in the routing system.
 * Routes match HTTP requests based on method and path pattern.
 */
public class Route {
    
    private final String pattern;
    private final List<String> methods;
    private final Consumer<Context> handler;
    private final Pattern compiledPattern;
    private final List<String> placeholders;
    private final Map<String, Object> defaults;
    private String name;
    private boolean customName;
    
    /**
     * Create a new route.
     * 
     * @param pattern the URL pattern (e.g., "/users/:id")
     * @param methods the HTTP methods this route responds to
     * @param handler the handler function
     */
    public Route(String pattern, List<String> methods, Consumer<Context> handler) {
        this.pattern = pattern;
        this.methods = new ArrayList<>(methods);
        this.handler = handler;
        this.defaults = new HashMap<>();
        this.placeholders = new ArrayList<>();
        this.compiledPattern = compilePattern(pattern);
        this.name = generateName(pattern);
        this.customName = false;
    }
    
    /**
     * Compile a route pattern into a regex pattern.
     * Supports placeholders like :id, :name, etc.
     * 
     * @param pattern the route pattern
     * @return the compiled regex pattern
     */
    private Pattern compilePattern(String pattern) {
        StringBuilder regex = new StringBuilder("^");
        String[] parts = pattern.split("/");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            
            regex.append("/");
            
            if (part.startsWith(":")) {
                // Named placeholder
                String placeholder = part.substring(1);
                placeholders.add(placeholder);
                regex.append("([^/]+)");
            } else if (part.startsWith("<") && part.contains(":") && part.endsWith(">")) {
                // Typed placeholder like <id:num>
                int colonIndex = part.indexOf(':');
                String placeholder = part.substring(1, colonIndex);
                String type = part.substring(colonIndex + 1, part.length() - 1);
                placeholders.add(placeholder);
                
                if ("num".equals(type)) {
                    regex.append("([0-9]+)");
                } else {
                    regex.append("([^/]+)");
                }
            } else if (part.equals("*")) {
                // Wildcard - matches everything
                placeholders.add("path");
                regex.append("(.*)");
            } else {
                // Literal part
                regex.append(Pattern.quote(part));
            }
        }
        
        // Handle root path
        if (pattern.equals("/")) {
            return Pattern.compile("^/$");
        }
        
        // Allow optional trailing slash
        regex.append("/?$");
        
        return Pattern.compile(regex.toString());
    }
    
    /**
     * Generate a default name for the route based on the pattern.
     * 
     * @param pattern the route pattern
     * @return the generated name
     */
    private String generateName(String pattern) {
        return pattern.replaceAll("[^a-zA-Z0-9]", "");
    }
    
    /**
     * Check if this route matches the given method and path.
     * 
     * @param method the HTTP method
     * @param path the request path
     * @return true if the route matches
     */
    public boolean matches(String method, String path) {
        // Check method
        if (!methods.isEmpty() && !methods.contains(method.toUpperCase())) {
            // HEAD is treated as GET
            if (!"HEAD".equals(method) || !methods.contains("GET")) {
                return false;
            }
        }
        
        // Check path pattern
        return compiledPattern.matcher(path).matches();
    }
    
    /**
     * Extract path parameters from the given path.
     * 
     * @param path the request path
     * @return map of parameter names to values
     */
    public Map<String, String> extractParams(String path) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = compiledPattern.matcher(path);
        
        if (matcher.matches()) {
            for (int i = 0; i < placeholders.size(); i++) {
                if (i + 1 <= matcher.groupCount()) {
                    params.put(placeholders.get(i), matcher.group(i + 1));
                }
            }
        }
        
        return params;
    }
    
    /**
     * Get the route pattern.
     * 
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * Get the HTTP methods this route responds to.
     * 
     * @return the methods list
     */
    public List<String> getMethods() {
        return methods;
    }
    
    /**
     * Get the route handler.
     * 
     * @return the handler
     */
    public Consumer<Context> getHandler() {
        return handler;
    }
    
    /**
     * Get the placeholder names in this route.
     * 
     * @return the placeholder names
     */
    public List<String> getPlaceholders() {
        return placeholders;
    }
    
    /**
     * Get the default values for this route.
     * 
     * @return the defaults map
     */
    public Map<String, Object> getDefaults() {
        return defaults;
    }
    
    /**
     * Set default values for this route.
     * 
     * @param key the parameter name
     * @param value the default value
     * @return this route for chaining
     */
    public Route defaults(String key, Object value) {
        defaults.put(key, value);
        return this;
    }
    
    /**
     * Get the route name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the route name.
     * 
     * @param name the name
     * @return this route for chaining
     */
    public Route name(String name) {
        this.name = name;
        this.customName = true;
        return this;
    }
    
    /**
     * Check if this route has a custom name.
     * 
     * @return true if custom name was set
     */
    public boolean hasCustomName() {
        return customName;
    }
    
    /**
     * Render the route pattern with the given values.
     * 
     * @param values the parameter values
     * @return the rendered path
     */
    public String render(Map<String, String> values) {
        String result = pattern;
        for (String placeholder : placeholders) {
            String value = values.getOrDefault(placeholder, "");
            result = result.replace(":" + placeholder, value);
            result = result.replaceAll("<" + placeholder + ":[^>]+>", value);
        }
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("Route{pattern='%s', methods=%s, name='%s'}", pattern, methods, name);
    }
}
