package org.mojolicious;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Parameters {
    private final Map<String, List<String>> params = new LinkedHashMap<>();
    private String rawString;
    private Charset charset = StandardCharsets.UTF_8;
    private boolean parsed = false;

    public Parameters() {
    }

    public Parameters(String queryString) {
        this.rawString = queryString;
    }

    public Parameters parse(String queryString) {
        this.rawString = queryString;
        this.parsed = false;
        this.params.clear();
        return this;
    }

    public Parameters append(Parameters other) {
        ensureParsed();
        for (Map.Entry<String, List<String>> entry : other.getParamsMap().entrySet()) {
            String name = entry.getKey();
            for (String value : entry.getValue()) {
                params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            }
        }
        return this;
    }

    public Parameters append(String name, String value) {
        ensureParsed();
        params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    public Parameters append(String name, List<String> values) {
        ensureParsed();
        for (String value : values) {
            params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
        return this;
    }

    public String param(String name) {
        List<String> values = everyParam(name);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    public List<String> everyParam(String name) {
        ensureParsed();
        return params.getOrDefault(name, Collections.emptyList());
    }

    public Parameters setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public Parameters clone() {
        Parameters cloned = new Parameters();
        cloned.charset = this.charset;
        if (this.rawString != null && !this.parsed) {
            cloned.rawString = this.rawString;
            cloned.parsed = false;
        } else {
            ensureParsed();
            for (Map.Entry<String, List<String>> entry : this.params.entrySet()) {
                cloned.params.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            cloned.parsed = true;
        }
        return cloned;
    }

    @Override
    public String toString() {
        ensureParsed();
        if (params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String name = entry.getKey();
            for (String value : entry.getValue()) {
                if (!first) {
                    sb.append('&');
                }
                first = false;

                String encodedName = urlEncode(name);
                String encodedValue = urlEncode(value);

                encodedName = encodedName.replace("%20", "+");
                encodedValue = encodedValue.replace("%20", "+");

                sb.append(encodedName).append('=').append(encodedValue);
            }
        }

        return sb.toString();
    }

    private void ensureParsed() {
        if (!parsed && rawString != null) {
            parseInternal();
        }
        parsed = true;
    }

    private void parseInternal() {
        if (rawString == null || rawString.isEmpty()) {
            return;
        }

        String[] pairs = rawString.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }

            String name;
            String value;

            int idx = pair.indexOf('=');
            if (idx >= 0) {
                name = pair.substring(0, idx);
                value = pair.substring(idx + 1);
            } else {
                name = pair;
                value = "";
            }

            name = name.replace('+', ' ');
            value = value.replace('+', ' ');

            name = urlDecode(name);
            value = urlDecode(value);

            params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
    }

    private String urlEncode(String str) {
        if (str == null) {
            return "";
        }
        try {
            String charsetName = charset != null ? charset.name() : StandardCharsets.UTF_8.name();
            return URLEncoder.encode(str, charsetName);
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    private String urlDecode(String str) {
        if (str == null) {
            return "";
        }
        try {
            String charsetName = charset != null ? charset.name() : StandardCharsets.UTF_8.name();
            return URLDecoder.decode(str, charsetName);
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return str;
        }
    }

    Map<String, List<String>> getParamsMap() {
        ensureParsed();
        return params;
    }

    public Set<String> names() {
        ensureParsed();
        return new TreeSet<>(params.keySet());
    }

    public Map<String, Object> toHash() {
        ensureParsed();
        Map<String, Object> hash = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            List<String> values = entry.getValue();
            if (values.size() == 1) {
                hash.put(entry.getKey(), values.get(0));
            } else {
                hash.put(entry.getKey(), new ArrayList<>(values));
            }
        }
        return hash;
    }

    public Parameters remove(String name) {
        ensureParsed();
        params.remove(name);
        return this;
    }
}
