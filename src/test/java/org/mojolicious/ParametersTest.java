package org.mojolicious;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParametersTest {

    @Test
    void testParseSimpleQueryString() {
        Parameters params = new Parameters("foo=bar&baz=23");
        assertEquals("bar", params.param("foo"));
        assertEquals("23", params.param("baz"));
    }

    @Test
    void testParseWithUrlEncoding() {
        Parameters params = new Parameters("name=John+Doe&city=New%20York");
        assertEquals("John Doe", params.param("name"));
        assertEquals("New York", params.param("city"));
    }

    @Test
    void testMultipleValues() {
        Parameters params = new Parameters("foo=bar&foo=baz&foo=qux");
        List<String> values = params.everyParam("foo");
        assertEquals(3, values.size());
        assertEquals("bar", values.get(0));
        assertEquals("baz", values.get(1));
        assertEquals("qux", values.get(2));
    }

    @Test
    void testParamReturnsLastValue() {
        Parameters params = new Parameters("foo=bar&foo=baz&foo=qux");
        assertEquals("qux", params.param("foo"));
    }

    @Test
    void testAppendParameters() {
        Parameters params1 = new Parameters("foo=bar");
        Parameters params2 = new Parameters("baz=23");
        params1.append(params2);
        
        assertEquals("bar", params1.param("foo"));
        assertEquals("23", params1.param("baz"));
    }

    @Test
    void testAppendSingleValue() {
        Parameters params = new Parameters();
        params.append("foo", "bar");
        params.append("baz", "23");
        
        assertEquals("bar", params.param("foo"));
        assertEquals("23", params.param("baz"));
    }

    @Test
    void testToString() {
        Parameters params = new Parameters();
        params.append("foo", "bar");
        params.append("baz", "23");
        
        String result = params.toString();
        assertTrue(result.contains("foo=bar"));
        assertTrue(result.contains("baz=23"));
        assertTrue(result.contains("&"));
    }

    @Test
    void testToStringWithSpaces() {
        Parameters params = new Parameters();
        params.append("name", "John Doe");
        
        String result = params.toString();
        assertTrue(result.contains("name=John+Doe"));
    }

    @Test
    void testEmptyQueryString() {
        Parameters params = new Parameters("");
        assertNull(params.param("foo"));
        assertTrue(params.everyParam("foo").isEmpty());
    }

    @Test
    void testNullValue() {
        Parameters params = new Parameters("foo=");
        assertEquals("", params.param("foo"));
    }

    @Test
    void testCharsetEncoding() {
        Parameters params = new Parameters();
        params.setCharset(StandardCharsets.UTF_8);
        params.append("name", "José");
        
        String result = params.toString();
        assertNotNull(result);
        assertTrue(result.contains("name="));
    }

    @Test
    void testClone() {
        Parameters params = new Parameters("foo=bar&baz=23");
        Parameters cloned = params.clone();
        
        assertEquals("bar", cloned.param("foo"));
        assertEquals("23", cloned.param("baz"));
        
        cloned.append("new", "value");
        assertNull(params.param("new"));
    }

    @Test
    void testToHash() {
        Parameters params = new Parameters("foo=bar&baz=23");
        Map<String, Object> hash = params.toHash();
        
        assertEquals("bar", hash.get("foo"));
        assertEquals("23", hash.get("baz"));
    }

    @Test
    void testToHashWithMultipleValues() {
        Parameters params = new Parameters("foo=bar&foo=baz");
        Map<String, Object> hash = params.toHash();
        
        assertTrue(hash.get("foo") instanceof List);
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) hash.get("foo");
        assertEquals(2, values.size());
    }

    @Test
    void testRemove() {
        Parameters params = new Parameters("foo=bar&baz=23");
        params.remove("foo");
        
        assertNull(params.param("foo"));
        assertEquals("23", params.param("baz"));
    }

    @Test
    void testNames() {
        Parameters params = new Parameters("foo=bar&baz=23&qux=42");
        var names = params.names();
        
        assertTrue(names.contains("foo"));
        assertTrue(names.contains("baz"));
        assertTrue(names.contains("qux"));
        assertEquals(3, names.size());
    }

    @Test
    void testSpecialCharacters() {
        Parameters params = new Parameters("email=test%40example.com&path=%2Fhome%2Fuser");
        assertEquals("test@example.com", params.param("email"));
        assertEquals("/home/user", params.param("path"));
    }

    @Test
    void testLazyParsing() {
        Parameters params = new Parameters("foo=bar&baz=23");
        assertEquals("bar", params.param("foo"));
    }

    @Test
    void testEmptyParameterName() {
        Parameters params = new Parameters("=value");
        assertEquals("value", params.param(""));
    }

    @Test
    void testParameterWithoutValue() {
        Parameters params = new Parameters("foo");
        assertEquals("", params.param("foo"));
    }
}
