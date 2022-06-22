package com.playgileplayground.jira.impl;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GroupByTypes {
    public static final String NONE = "";
    public static final String COMPONENT = "components";
    public static final String ASSIGNEE = "assignee";
    public static final String CREATOR = "creator";
    public static final String BP_TEAM = "bp_team";
    public static final String INITIATIVE = "initiative";


    public static List<String> getViewTypes() {
        return Arrays.stream(GroupByTypes.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .map(field -> {
                try {
                    return (String) field.get(GroupByTypes.class);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
    }
}
