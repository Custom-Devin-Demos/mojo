package org.mojolicious;

import org.junit.jupiter.api.Test;
import org.mojolicious.routing.Routes;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Application class.
 * Tests the core functionality of the Mojolicious Java framework.
 */
public class ApplicationTest {
    
    @Test
    public void testApplicationVersion() {
        Application app = new Application();
        assertEquals("1.0.0-SNAPSHOT", app.getVersion());
    }
    
    @Test
    public void testApplicationName() {
        Application app = new Application();
        assertEquals("Mojolicious Java", app.getName());
    }
    
    @Test
    public void testApplicationMode() {
        Application app = new Application();
        // Default mode should be "development" when no env vars are set
        assertEquals("development", app.mode());
        
        // Test setting mode
        app.mode("production");
        assertEquals("production", app.mode());
    }
    
    @Test
    public void testApplicationMoniker() {
        Application app = new Application();
        // Default moniker should be decamelized class name
        assertEquals("application", app.moniker());
        
        // Test setting moniker
        app.moniker("my_app");
        assertEquals("my_app", app.moniker());
    }
    
    @Test
    public void testApplicationConfig() {
        Application app = new Application();
        
        // Test setting and getting config
        app.config("key1", "value1");
        assertEquals("value1", app.config("key1"));
        
        // Test config map
        assertNotNull(app.config());
        assertTrue(app.config().containsKey("key1"));
    }
    
    @Test
    public void testApplicationSecrets() {
        Application app = new Application();
        
        // Default secret should be the moniker
        assertFalse(app.secrets().isEmpty());
        assertEquals("application", app.secrets().get(0));
        
        // Test setting secrets
        app.secrets(Arrays.asList("secret1", "secret2"));
        assertEquals(2, app.secrets().size());
        assertEquals("secret1", app.secrets().get(0));
    }
    
    @Test
    public void testApplicationRoutes() {
        Application app = new Application();
        
        // Routes should be initialized
        Routes routes = app.routes();
        assertNotNull(routes);
        assertTrue(routes.isEmpty());
        
        // Test adding a route
        routes.get("/test", ctx -> ctx.text("test"));
        assertEquals(1, routes.size());
    }
    
    @Test
    public void testApplicationDefaults() {
        Application app = new Application();
        
        // Defaults should be initialized
        assertNotNull(app.defaults());
        assertTrue(app.defaults().isEmpty());
        
        // Test adding defaults
        app.defaults().put("layout", "default");
        assertEquals("default", app.defaults().get("layout"));
    }
    
    @Test
    public void testApplicationNotRunningInitially() {
        Application app = new Application();
        assertFalse(app.isRunning());
        assertNull(app.server());
    }
}
