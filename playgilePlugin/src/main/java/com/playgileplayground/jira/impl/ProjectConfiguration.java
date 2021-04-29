package com.playgileplayground.jira.impl;

import com.playgileplayground.jira.api.ProjectMonitor;

public class ProjectConfiguration {
    public ProjectConfiguration(String viewType) {
        this.viewType = viewType;
    }

    public ProjectConfiguration() {
    }

    private String viewType = ProjectMonitor.ROADMAPFEATUREKEY;


    public String getViewType() {
        return viewType;
    }

    public void setViewType(String viewType) {
        this.viewType = viewType;
    }
}
