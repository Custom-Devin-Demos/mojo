package org.mojolicious.server;

import org.mojolicious.Application;
import org.mojolicious.routing.Context;
import org.mojolicious.routing.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Netty-based HTTP server for Mojolicious Java.
 * Provides non-blocking I/O HTTP server functionality using reactor-netty.
 */
public class HttpServer {
    
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    
    private final Application app;
    private final int port;
    private final int inactivityTimeout;
    private final int keepAliveTimeout;
    private final int maxRequests;
    
    private DisposableServer server;
    private final CountDownLatch shutdownLatch;
    
    /**
     * Create a new HTTP server.
     * 
     * @param app the application
     * @param port the port to listen on
     */
    public HttpServer(Application app, int port) {
        this.app = app;
        this.port = port;
        this.shutdownLatch = new CountDownLatch(1);
        
        // Default timeouts from environment or defaults (matching Perl implementation)
        String inactivityEnv = System.getenv("MOJO_INACTIVITY_TIMEOUT");
        this.inactivityTimeout = inactivityEnv != null ? Integer.parseInt(inactivityEnv) : 30;
        
        String keepAliveEnv = System.getenv("MOJO_KEEP_ALIVE_TIMEOUT");
        this.keepAliveTimeout = keepAliveEnv != null ? Integer.parseInt(keepAliveEnv) : 5;
        
        this.maxRequests = 100;
    }
    
    /**
     * Start the HTTP server.
     * 
     * @return Mono that completes when the server is started
     */
    public Mono<Void> start() {
        return Mono.fromRunnable(() -> {
            server = reactor.netty.http.server.HttpServer.create()
                .port(port)
                .idleTimeout(Duration.ofSeconds(inactivityTimeout))
                .handle(this::handleRequest)
                .bindNow();
            
            log.info("Listening at http://127.0.0.1:{}", port);
            System.out.println("Web application available at http://127.0.0.1:" + port);
        });
    }
    
    /**
     * Stop the HTTP server.
     * 
     * @return Mono that completes when the server is stopped
     */
    public Mono<Void> stop() {
        return Mono.fromRunnable(() -> {
            if (server != null) {
                server.disposeNow(Duration.ofSeconds(10));
                server = null;
            }
            shutdownLatch.countDown();
        });
    }
    
    /**
     * Block until the server is shut down.
     */
    public void awaitShutdown() {
        try {
            if (server != null) {
                server.onDispose().block();
            }
        } catch (Exception e) {
            log.debug("Server shutdown interrupted", e);
        }
    }
    
    /**
     * Get the port the server is listening on.
     * 
     * @return the port
     */
    public int getPort() {
        return server != null ? server.port() : port;
    }
    
    /**
     * Check if the server is running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return server != null && !server.isDisposed();
    }
    
    /**
     * Get the inactivity timeout in seconds.
     * 
     * @return the timeout
     */
    public int getInactivityTimeout() {
        return inactivityTimeout;
    }
    
    /**
     * Get the keep-alive timeout in seconds.
     * 
     * @return the timeout
     */
    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }
    
    /**
     * Get the maximum number of requests per connection.
     * 
     * @return the max requests
     */
    public int getMaxRequests() {
        return maxRequests;
    }
    
    /**
     * Handle an incoming HTTP request.
     */
    private Mono<Void> handleRequest(HttpServerRequest request, HttpServerResponse response) {
        String method = request.method().name();
        String path = request.uri();
        
        // Remove query string from path for routing
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        
        log.trace("{} \"{}\"", method, path);
        
        // Set default server header
        response.header("Server", "Mojolicious (Java)");
        
        // Try to match a route
        Route matchedRoute = app.routes().match(method, path);
        
        if (matchedRoute != null) {
            // Create context and dispatch to handler
            Context ctx = new Context(app, request, response, matchedRoute);
            
            try {
                matchedRoute.getHandler().accept(ctx);
                return ctx.finish();
            } catch (Exception e) {
                log.error("Error handling request", e);
                return sendError(response, 500, "Internal Server Error");
            }
        } else {
            // No route found - 404
            log.trace("Route not found for {} {}", method, path);
            return sendError(response, 404, "Not Found");
        }
    }
    
    /**
     * Send an error response.
     */
    private Mono<Void> sendError(HttpServerResponse response, int status, String message) {
        String body = String.format(
            "<!DOCTYPE html>\n<html>\n<head><title>%d %s</title></head>\n" +
            "<body><h1>%d %s</h1></body>\n</html>",
            status, message, status, message
        );
        
        return response
            .status(status)
            .header("Content-Type", "text/html; charset=utf-8")
            .sendString(Mono.just(body))
            .then();
    }
}
