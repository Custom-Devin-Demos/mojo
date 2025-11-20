package org.mojolicious;

import java.util.*;

public class Controller {
    private Map<String, Object> stash;
    private Request request;
    private Set<String> reservedNames;

    public Controller() {
        this.stash = new LinkedHashMap<>();
        this.request = new Request();
        this.reservedNames = new HashSet<>();
        initializeReservedNames();
    }

    public Controller(Request request) {
        this.stash = new LinkedHashMap<>();
        this.request = request;
        this.reservedNames = new HashSet<>();
        initializeReservedNames();
    }

    private void initializeReservedNames() {
        reservedNames.add("action");
        reservedNames.add("app");
        reservedNames.add("cb");
        reservedNames.add("controller");
        reservedNames.add("data");
        reservedNames.add("extends");
        reservedNames.add("format");
        reservedNames.add("handler");
        reservedNames.add("inline");
        reservedNames.add("json");
        reservedNames.add("layout");
        reservedNames.add("namespace");
        reservedNames.add("path");
        reservedNames.add("status");
        reservedNames.add("template");
        reservedNames.add("text");
        reservedNames.add("variant");
    }

    public String param(String name) {
        List<String> values = everyParam(name);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    public List<String> everyParam(String name) {
        if (stash.containsKey(name) && !isReserved(name)) {
            Object value = stash.get(name);
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> listValue = (List<String>) value;
                return new ArrayList<>(listValue);
            } else if (value != null) {
                return Collections.singletonList(value.toString());
            }
        }

        return request.everyParam(name);
    }

    public Controller param(String name, String value) {
        stash.put(name, value);
        return this;
    }

    public Controller param(String name, List<String> values) {
        stash.put(name, values);
        return this;
    }

    public Map<String, Object> stash() {
        return stash;
    }

    public Object stash(String key) {
        return stash.get(key);
    }

    public Controller stash(String key, Object value) {
        stash.put(key, value);
        return this;
    }

    public Controller stash(Map<String, Object> values) {
        stash.putAll(values);
        return this;
    }

    public Request req() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public boolean isReserved(String name) {
        return reservedNames.contains(name);
    }

    public void addReservedName(String name) {
        reservedNames.add(name);
    }

    public void removeReservedName(String name) {
        reservedNames.remove(name);
    }

    public Set<String> getReservedNames() {
        return Collections.unmodifiableSet(reservedNames);
    }
}
