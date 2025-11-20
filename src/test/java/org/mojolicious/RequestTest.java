package org.mojolicious;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testEmptyRequest() {
        Request request = new Request();
        assertNull(request.param("foo"));
        assertTrue(request.everyParam("foo").isEmpty());
    }

    @Test
    void testQueryParameters() {
        Request request = new Request();
        request.parseQueryString("foo=bar&baz=23");
        
        assertEquals("bar", request.param("foo"));
        assertEquals("23", request.param("baz"));
    }

    @Test
    void testBodyParameters() {
        Request request = new Request();
        request.parseBodyParameters("name=John&age=30", "application/x-www-form-urlencoded");
        
        assertEquals("John", request.bodyParams().param("name"));
        assertEquals("30", request.bodyParams().param("age"));
    }

    @Test
    void testMergedParameters() {
        Request request = new Request();
        request.parseQueryString("foo=query");
        request.parseBodyParameters("bar=body", "application/x-www-form-urlencoded");
        
        Parameters merged = request.params();
        assertEquals("query", merged.param("foo"));
        assertEquals("body", merged.param("bar"));
    }

    @Test
    void testQueryOverridesBody() {
        Request request = new Request();
        request.parseBodyParameters("foo=body", "application/x-www-form-urlencoded");
        request.parseQueryString("foo=query");
        
        assertEquals("query", request.param("foo"));
    }

    @Test
    void testEveryParam() {
        Request request = new Request();
        request.parseQueryString("foo=bar&foo=baz");
        
        List<String> values = request.everyParam("foo");
        assertEquals(2, values.size());
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("baz"));
    }

    @Test
    void testSetQueryParams() {
        Request request = new Request();
        Parameters params = new Parameters("foo=bar");
        request.setQueryParams(params);
        
        assertEquals("bar", request.param("foo"));
    }

    @Test
    void testSetBodyParams() {
        Request request = new Request();
        Parameters params = new Parameters("baz=23");
        request.setBodyParams(params);
        
        assertEquals("23", request.bodyParams().param("baz"));
    }

    @Test
    void testNullQueryString() {
        Request request = new Request();
        request.parseQueryString(null);
        
        assertNull(request.param("foo"));
    }

    @Test
    void testEmptyQueryString() {
        Request request = new Request();
        request.parseQueryString("");
        
        assertNull(request.param("foo"));
    }

    @Test
    void testNullBody() {
        Request request = new Request();
        request.parseBodyParameters(null, "application/x-www-form-urlencoded");
        
        assertNull(request.bodyParams().param("foo"));
    }

    @Test
    void testEmptyBody() {
        Request request = new Request();
        request.parseBodyParameters("", "application/x-www-form-urlencoded");
        
        assertNull(request.bodyParams().param("foo"));
    }

    @Test
    void testNonFormContentType() {
        Request request = new Request();
        request.parseBodyParameters("foo=bar", "application/json");
        
        assertNull(request.bodyParams().param("foo"));
    }

    @Test
    void testMultipleValuesInMergedParams() {
        Request request = new Request();
        request.parseBodyParameters("foo=body1&foo=body2", "application/x-www-form-urlencoded");
        request.parseQueryString("foo=query1&foo=query2");
        
        Parameters merged = request.params();
        List<String> values = merged.everyParam("foo");
        assertEquals(4, values.size());
    }

    @Test
    void testQueryParamsAccessor() {
        Request request = new Request();
        request.parseQueryString("foo=bar");
        
        Parameters queryParams = request.queryParams();
        assertEquals("bar", queryParams.param("foo"));
    }

    @Test
    void testBodyParamsAccessor() {
        Request request = new Request();
        request.parseBodyParameters("baz=23", "application/x-www-form-urlencoded");
        
        Parameters bodyParams = request.bodyParams();
        assertEquals("23", bodyParams.param("baz"));
    }
}
