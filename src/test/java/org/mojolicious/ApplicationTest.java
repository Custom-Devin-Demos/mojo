package org.mojolicious;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Application class.
 * Basic test to verify the project structure and compilation work properly.
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
    public void testMainMethodDoesNotThrow() {
        assertDoesNotThrow(() -> {
            Application.main(new String[]{});
        });
    }
}
