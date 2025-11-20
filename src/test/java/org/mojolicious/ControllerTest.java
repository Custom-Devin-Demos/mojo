package org.mojolicious;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ControllerTest {

    @Test
    void testEmptyController() {
        Controller controller = new Controller();
        assertNull(controller.param("foo"));
        assertTrue(controller.everyParam("foo").isEmpty());
    }

    @Test
    void testStashPriority() {
        Request request = new Request();
        request.parseQueryString("foo=query");
        
        Controller controller = new Controller(request);
        controller.stash("foo", "stash");
        
        assertEquals("stash", controller.param("foo"));
    }

    @Test
    void testRequestParameterFallback() {
        Request request = new Request();
        request.parseQueryString("foo=query");
        
        Controller controller = new Controller(request);
        assertEquals("query", controller.param("foo"));
    }

    @Test
    void testReservedNamesNotOverridden() {
        Request request = new Request();
        request.parseQueryString("action=query");
        
        Controller controller = new Controller(request);
        controller.stash("action", "stash");
        
        assertEquals("query", controller.param("action"));
    }

    @Test
    void testEveryParamFromStash() {
        Controller controller = new Controller();
        controller.stash("foo", "bar");
        
        List<String> values = controller.everyParam("foo");
        assertEquals(1, values.size());
        assertEquals("bar", values.get(0));
    }

    @Test
    void testEveryParamWithListInStash() {
        Controller controller = new Controller();
        controller.stash("foo", Arrays.asList("bar", "baz"));
        
        List<String> values = controller.everyParam("foo");
        assertEquals(2, values.size());
        assertEquals("bar", values.get(0));
        assertEquals("baz", values.get(1));
    }

    @Test
    void testEveryParamFromRequest() {
        Request request = new Request();
        request.parseQueryString("foo=bar&foo=baz");
        
        Controller controller = new Controller(request);
        List<String> values = controller.everyParam("foo");
        
        assertEquals(2, values.size());
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("baz"));
    }

    @Test
    void testSetParamSingleValue() {
        Controller controller = new Controller();
        controller.param("foo", "bar");
        
        assertEquals("bar", controller.param("foo"));
    }

    @Test
    void testSetParamMultipleValues() {
        Controller controller = new Controller();
        controller.param("foo", Arrays.asList("bar", "baz"));
        
        List<String> values = controller.everyParam("foo");
        assertEquals(2, values.size());
    }

    @Test
    void testStashAccessor() {
        Controller controller = new Controller();
        controller.stash("foo", "bar");
        
        assertEquals("bar", controller.stash("foo"));
    }

    @Test
    void testStashMap() {
        Controller controller = new Controller();
        controller.stash("foo", "bar");
        controller.stash("baz", "qux");
        
        Map<String, Object> stash = controller.stash();
        assertEquals("bar", stash.get("foo"));
        assertEquals("qux", stash.get("baz"));
    }

    @Test
    void testStashWithMap() {
        Controller controller = new Controller();
        Map<String, Object> values = Map.of("foo", "bar", "baz", "qux");
        controller.stash(values);
        
        assertEquals("bar", controller.stash("foo"));
        assertEquals("qux", controller.stash("baz"));
    }

    @Test
    void testReqAccessor() {
        Request request = new Request();
        request.parseQueryString("foo=bar");
        
        Controller controller = new Controller(request);
        assertEquals(request, controller.req());
    }

    @Test
    void testSetRequest() {
        Controller controller = new Controller();
        Request request = new Request();
        request.parseQueryString("foo=bar");
        
        controller.setRequest(request);
        assertEquals("bar", controller.param("foo"));
    }

    @Test
    void testIsReserved() {
        Controller controller = new Controller();
        assertTrue(controller.isReserved("action"));
        assertTrue(controller.isReserved("controller"));
        assertTrue(controller.isReserved("format"));
        assertFalse(controller.isReserved("foo"));
    }

    @Test
    void testAddReservedName() {
        Controller controller = new Controller();
        assertFalse(controller.isReserved("custom"));
        
        controller.addReservedName("custom");
        assertTrue(controller.isReserved("custom"));
    }

    @Test
    void testRemoveReservedName() {
        Controller controller = new Controller();
        assertTrue(controller.isReserved("action"));
        
        controller.removeReservedName("action");
        assertFalse(controller.isReserved("action"));
    }

    @Test
    void testGetReservedNames() {
        Controller controller = new Controller();
        var reserved = controller.getReservedNames();
        
        assertTrue(reserved.contains("action"));
        assertTrue(reserved.contains("controller"));
        assertTrue(reserved.contains("format"));
    }

    @Test
    void testStashOverridesRequestForNonReserved() {
        Request request = new Request();
        request.parseQueryString("foo=query&bar=request");
        
        Controller controller = new Controller(request);
        controller.stash("foo", "stash");
        
        assertEquals("stash", controller.param("foo"));
        assertEquals("request", controller.param("bar"));
    }

    @Test
    void testMultipleParameterSources() {
        Request request = new Request();
        request.parseQueryString("query=value");
        request.parseBodyParameters("body=value", "application/x-www-form-urlencoded");
        
        Controller controller = new Controller(request);
        controller.stash("stash", "value");
        
        assertEquals("value", controller.param("query"));
        assertEquals("value", controller.param("body"));
        assertEquals("value", controller.param("stash"));
    }

    @Test
    void testNullStashValue() {
        Controller controller = new Controller();
        controller.stash("foo", null);
        
        assertNull(controller.stash("foo"));
    }

    @Test
    void testParamReturnsLastValue() {
        Request request = new Request();
        request.parseQueryString("foo=bar&foo=baz&foo=qux");
        
        Controller controller = new Controller(request);
        assertEquals("qux", controller.param("foo"));
    }
}
