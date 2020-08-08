package com.playgileplayground.jira.impl;

import java.util.Date;

/**
 * Created by Ext_EdG on 8/8/2020.
 */
public class RoadmapFeatureDescriptor {
    public String Name;
    public double TeamVelocity;
    public double ProjectVelocity;
    public TotalViewMisc.FeatureStatus Status;
    public Date StartDate;
    public double SprintLength;
    public AnalyzedStories EstimatedStories;
    public Date IdealEndOfProjet;
    public Date PredictedEndOfProjet;

    public RoadmapFeatureDescriptor() {
        Status = TotalViewMisc.FeatureStatus.NOT_STARTED;
    }
}
