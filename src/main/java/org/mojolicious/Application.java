package org.mojolicious;

import org.mojolicious.routing.Routes;
import org.mojolicious.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main application class for Mojolicious Java framework.
 * This is the core of the framework, managing lifecycle, configuration, routing, and server.
 */
public class Application {
    
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    
    public static final String VERSION = "1.0.0-SNAPSHOT";
    public static final String CODENAME = "Waffle";
    
    private final Map<String, Object> config;
    private final Map<String, Object> defaults;
    private final List<String> secrets;
    private final Routes routes;
    private final List<Consumer<Application>> hooks;
    
    private String mode;
    private String moniker;
    private HttpServer server;
    private volatile boolean running;
    
    /**
     * Create a new Application instance.
     */
    public Application() {
        this.config = new HashMap<>();
        this.defaults = new HashMap<>();
        this.secrets = new ArrayList<>();
        this.routes = new Routes();
        this.hooks = new ArrayList<>();
        this.running = false;
        
        // Default mode from environment or "development"
        this.mode = System.getenv("MOJO_MODE");
        if (this.mode == null || this.mode.isEmpty()) {
            this.mode = System.getenv("PLACK_ENV");
        }
        if (this.mode == null || this.mode.isEmpty()) {
            this.mode = "development";
        }
        
        // Default moniker based on class name
        this.moniker = decamelize(getClass().getSimpleName());
        
        // Default secret (warn in development)
        this.secrets.add(this.moniker);
        if ("development".equals(this.mode)) {
            log.trace("Your secret passphrase needs to be changed (see FAQ for more)");
        }
        
        // Initialize
        initialize();
    }
    
    /**
     * Initialize the application. Called during construction.
     * Override this method to add custom initialization logic.
     */
    protected void initialize() {
        // Default initialization - subclasses can override
    }
    
    /**
     * Startup hook for the application. Called after initialization.
     * Override this method to configure routes and application settings.
     */
    public void startup() {
        // Default startup - subclasses should override to configure routes
    }
    
    /**
     * Start the application with the HTTP server.
     * 
     * @return Mono that completes when the server is started
     */
    public Mono<Void> start() {
        return start(3000);
    }
    
    /**
     * Start the application with the HTTP server on the specified port.
     * 
     * @param port the port to listen on
     * @return Mono that completes when the server is started
     */
    public Mono<Void> start(int port) {
        if (running) {
            return Mono.empty();
        }
        
        log.info("Starting {} {} ({})", getName(), VERSION, mode);
        
        // Call startup hook
        startup();
        
        // Warmup
        warmup();
        
        // Create and start server
        server = new HttpServer(this, port);
        running = true;
        
        // Emit before_server_start hook
        emitHook("before_server_start");
        
        return server.start()
            .doOnSuccess(v -> log.info("Server started on port {}", port))
            .doOnError(e -> {
                log.error("Failed to start server", e);
                running = false;
            });
    }
    
    /**
     * Stop the application and HTTP server.
     * 
     * @return Mono that completes when the server is stopped
     */
    public Mono<Void> stop() {
        if (!running || server == null) {
            return Mono.empty();
        }
        
        log.info("Stopping application");
        running = false;
        
        return server.stop()
            .doOnSuccess(v -> log.info("Server stopped"))
            .doOnError(e -> log.error("Error stopping server", e));
    }
    
    /**
     * Block and run the application until interrupted.
     */
    public void run() {
        run(3000);
    }
    
    /**
     * Block and run the application on the specified port until interrupted.
     * 
     * @param port the port to listen on
     */
    public void run(int port) {
        start(port).block();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stop().block();
        }));
        
        // Block until stopped
        if (server != null) {
            server.awaitShutdown();
        }
    }
    
    /**
     * Warmup the application. Called before server starts.
     */
    protected void warmup() {
        // Preload any necessary resources
    }
    
    /**
     * Register a hook to be called at specific lifecycle points.
     * 
     * @param name the hook name
     * @param hook the hook callback
     * @return this application for chaining
     */
    public Application hook(String name, Consumer<Application> hook) {
        hooks.add(hook);
        return this;
    }
    
    /**
     * Emit a hook event.
     * 
     * @param name the hook name
     */
    protected void emitHook(String name) {
        for (Consumer<Application> hook : hooks) {
            try {
                hook.accept(this);
            } catch (Exception e) {
                log.error("Error in hook: {}", name, e);
            }
        }
    }
    
    /**
     * Get the application routes.
     * 
     * @return the routes
     */
    public Routes routes() {
        return routes;
    }
    
    /**
     * Get the application configuration.
     * 
     * @return the configuration map
     */
    public Map<String, Object> config() {
        return config;
    }
    
    /**
     * Get a configuration value.
     * 
     * @param key the configuration key
     * @return the value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T config(String key) {
        return (T) config.get(key);
    }
    
    /**
     * Set a configuration value.
     * 
     * @param key the configuration key
     * @param value the value
     * @return this application for chaining
     */
    public Application config(String key, Object value) {
        config.put(key, value);
        return this;
    }
    
    /**
     * Get the default stash values.
     * 
     * @return the defaults map
     */
    public Map<String, Object> defaults() {
        return defaults;
    }
    
    /**
     * Get the application secrets.
     * 
     * @return the secrets list
     */
    public List<String> secrets() {
        return secrets;
    }
    
    /**
     * Set the application secrets.
     * 
     * @param secrets the secrets
     * @return this application for chaining
     */
    public Application secrets(List<String> secrets) {
        this.secrets.clear();
        this.secrets.addAll(secrets);
        return this;
    }
    
    /**
     * Get the application mode.
     * 
     * @return the mode (e.g., "development", "production")
     */
    public String mode() {
        return mode;
    }
    
    /**
     * Set the application mode.
     * 
     * @param mode the mode
     * @return this application for chaining
     */
    public Application mode(String mode) {
        this.mode = mode;
        return this;
    }
    
    /**
     * Get the application moniker.
     * 
     * @return the moniker
     */
    public String moniker() {
        return moniker;
    }
    
    /**
     * Set the application moniker.
     * 
     * @param moniker the moniker
     * @return this application for chaining
     */
    public Application moniker(String moniker) {
        this.moniker = moniker;
        return this;
    }
    
    /**
     * Check if the application is running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Get the HTTP server.
     * 
     * @return the server, or null if not started
     */
    public HttpServer server() {
        return server;
    }
    
    /**
     * Get the application version.
     * 
     * @return the version string
     */
    public String getVersion() {
        return VERSION;
    }
    
    /**
     * Get the framework name.
     * 
     * @return the framework name
     */
    public String getName() {
        return "Mojolicious Java";
    }
    
    /**
     * Main entry point for the application.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Application app = new Application();
        
        // Add a simple default route
        app.routes().get("/", ctx -> ctx.text("Welcome to Mojolicious Java!"));
        
        int port = 3000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // Use default port
            }
        }
        
        System.out.println("Mojolicious Java Framework - " + VERSION);
        System.out.println("Starting server on port " + port + "...");
        
        app.run(port);
    }
    
    /**
     * Convert a CamelCase string to snake_case.
     * 
     * @param str the string to convert
     * @return the snake_case string
     */
    private static String decamelize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
