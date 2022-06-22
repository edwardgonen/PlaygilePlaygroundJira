package com.playgileplayground.jira.impl;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ViewTypes {
    public static final String EPIC = "Epic";
    public static final String ROADMAP_FEATURE = "Roadmap Feature";

    public List<String> getViewTypes() {
        return Arrays.stream(this.getClass().getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .map(field -> {
                try {
                    return (String) field.get(this.getClass());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
    }
}
