package com.playgileplayground.jira.impl;

import java.awt.*;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Ext_EdG on 8/8/2020.
 */
public class RoadmapFeatureDescriptor implements Comparator<RoadmapFeatureDescriptor>, Comparable<RoadmapFeatureDescriptor>{
    public String Name;
    public double TeamVelocity;
    public double ProjectVelocity;
    public TotalViewMisc.FeatureStatus Status;
    public Date StartDate;
    public double SprintLength;
    public AnalyzedStories EstimatedStories;
    public Date IdealEndOfProjet;
    public Date PredictedEndOfProjet;
    public Color ProgressDataColor;

    public RoadmapFeatureDescriptor() {
        Name = "";
        Status = TotalViewMisc.FeatureStatus.NOT_STARTED;
    }

    @Override
    public int compareTo(RoadmapFeatureDescriptor o) {
        return Name.compareTo(o.Name);
    }

    @Override
    public int compare(RoadmapFeatureDescriptor o1, RoadmapFeatureDescriptor o2) {
        return o1.Name.compareTo(o2.Name);
    }
}
