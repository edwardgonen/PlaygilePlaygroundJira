package com.playgileplayground.jira.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectConfigurationModel {

    public ProjectConfigurationModel(String viewType, boolean projectEpics, boolean projectTickets, String groupBy) {
        this.viewType = viewType;
        this.projectEpics = projectEpics;
        this.projectTickets = projectTickets;
        this.groupBy = groupBy;
    }

    public ProjectConfigurationModel() {
    }

    private String viewType = ViewTypes.ROADMAP_FEATURE;
    private String groupBy = "";
    private boolean projectEpics = false;
    private boolean projectTickets = false;

    public String getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }

    public boolean isProjectEpics() {
        return projectEpics;
    }

    public void setProjectEpics(boolean projectEpics) {
        this.projectEpics = projectEpics;
    }

    public boolean isProjectTickets() {
        return projectTickets;
    }

    public void setProjectTickets(boolean projectTickets) {
        this.projectTickets = projectTickets;
    }

    public List<String> getAllFields() {
        return Arrays.stream(this.getClass().getDeclaredFields())
            .map(Field::getName).collect(Collectors.toList());
    }
}
