package com.playgileplayground.jira.impl;

import com.playgileplayground.jira.api.ProjectMonitor;

public class ProjectConfiguration {
    public ProjectConfiguration(String viewType) {
        ViewType = viewType;
    }

    public ProjectConfiguration() {
    }

    public String ViewType = ProjectMonitor.ROADMAPFEATUREKEY;
}
