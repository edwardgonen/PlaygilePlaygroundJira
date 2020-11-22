package com.playgileplayground.jira.impl;

import java.awt.*;
import java.util.*;

/**
 * Created by Ext_EdG on 8/8/2020.
 */
public class RoadmapFeatureDescriptor implements Comparator<RoadmapFeatureDescriptor>, Comparable<RoadmapFeatureDescriptor>{
    public String Name;
    public double TeamVelocity;
    public double ProjectVelocity;
    public Date StartDate;
    public double SprintLength;
    public AnalyzedStories EstimatedStories;
    public Date IdealEndOfProjet;
    public Date PredictedEndOfProjet;
    public Date BusinessApprovalDate;
    public String Key;
    public Color ProgressDataColor;
    public ArrayList<ProjectPreparationIssue> PreparationIssues;

    public RoadmapFeatureDescriptor() {
        Name = "";
        PreparationIssues = new ArrayList<>();
    }

    @Override
    public int compareTo(RoadmapFeatureDescriptor o) {
        return Name.compareTo(o.Name);
    }

    @Override
    public int compare(RoadmapFeatureDescriptor o1, RoadmapFeatureDescriptor o2) {
        return o1.Name.compareTo(o2.Name);
    }

    static Comparator<RoadmapFeatureDescriptor> compareByBusinessApprovalDate = new Comparator<RoadmapFeatureDescriptor>() {
        @Override
        public int compare(RoadmapFeatureDescriptor o1, RoadmapFeatureDescriptor o2) {
            return o1.BusinessApprovalDate.compareTo(o2.BusinessApprovalDate);
        }
    };
}
