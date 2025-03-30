package com.akashgill3.githubcrawler.github.service;

import com.akashgill3.githubcrawler.github.model.Principle;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PrincipleCache {
    private final ConcurrentMap<String, Principle> principles = new ConcurrentHashMap<>();

    public void put(String name, Principle principle) {
        principles.put(name, principle);
    }

    public Principle get(String name) {
        return principles.get(name);
    }

    public Map<String, Principle> getAll() {
        return Map.copyOf(principles);  // Return an unmodifiable copy for safety
    }

    public boolean hasPrinciple(String name) {
        return principles.containsKey(name);
    }

    public void putAll(Map<String, Principle> updatedPrinciples) {
        principles.putAll(updatedPrinciples);
    }

    public void remove(Set<String> principleNames) {
        principleNames.forEach(principles::remove);
    }

    public void clear() {
        principles.clear();
    }

    public int size() {
        return principles.size();
    }
}
