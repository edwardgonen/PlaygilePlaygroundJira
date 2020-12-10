package com.playgileplayground.jira.impl;

import java.util.*;

public class RoadmapFeatureDescriptor implements Comparator<RoadmapFeatureDescriptor>, Comparable<RoadmapFeatureDescriptor>{
    public String Name;
    public Date BusinessApprovalDate;
    public String Key;
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
