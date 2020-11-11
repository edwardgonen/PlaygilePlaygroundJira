package com.playgileplayground.jira.impl;

import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.projectprogress.ProgressData;
import com.playgileplayground.jira.projectprogress.ProjectProgress;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Created by Ext_EdG on 11/8/2020.
 */
public class ProjectPreparationPresentation {

    public static final String BR1 = "bobruisk1";
    public static final String BR2 = "bobruisk2";
    public static final String BR3 = "bobruisk3";
    public static final String BR4 = "bobruisk4";
    public static final String BR5 = "bobruisk5";

    public static final String BR777 = "bobruisk777";

    //should consider situation when a month may appear in two or more years. So we will not sort this array
    ArrayList<MonthlyRoadmapFeatures> allRoadmapFeatures = new ArrayList<>();

    public boolean createPresentationData(List<RoadmapFeatureDescriptor> ourRoadmapFeatures)
    {
        boolean result = true;
        //1. Sort the input list by business approval date
        //here we need to sort the RF and product issues
        Collections.sort(ourRoadmapFeatures, RoadmapFeatureDescriptor.compareByBusinessApprovalDate);
        Date firstDate = ourRoadmapFeatures.get(0).BusinessApprovalDate;
        Date lastDate = ourRoadmapFeatures.get(ourRoadmapFeatures.size() - 1).BusinessApprovalDate;

        //go through all months from first record to the last
        for (Date date = firstDate; date.before(lastDate) || date.equals(lastDate); date = ProjectProgress.AddMonths(date, 1)) {
            //go through the list and build our array and find year/month
            MonthlyRoadmapFeatures monthlyRoadmapFeatures = null;
            for (RoadmapFeatureDescriptor feature : ourRoadmapFeatures)
            {
                if (ProjectProgress.CompareDatesByMonthYear(feature.BusinessApprovalDate, date))
                {
                    if (monthlyRoadmapFeatures == null) {
                        monthlyRoadmapFeatures = new MonthlyRoadmapFeatures();
                        monthlyRoadmapFeatures.monthYear = getMonth(date) + ", " + getYear(date);
                        allRoadmapFeatures.add(monthlyRoadmapFeatures);
                    }
                    monthlyRoadmapFeatures.roadMapFeaturesInThatMonth.add(feature);
                }
            }
        }

        return result;
    }

    public StringBuilder dataForBrowser()
    {
        StringBuilder result = new StringBuilder();
        Date today = Calendar.getInstance().getTime();

        for (MonthlyRoadmapFeatures monthlyFeatures : allRoadmapFeatures)
        {
            result.append(monthlyFeatures.monthYear);
            result.append(BR2);
            //now list features for this month
            for (RoadmapFeatureDescriptor rfd : monthlyFeatures.roadMapFeaturesInThatMonth)
            {






                result.append(rfd.Name + " " + rfd.Key + BR777 +
                                ConvertDateToOurFormat(rfd.BusinessApprovalDate) + BR777 +
                                ProjectProgress.convertColorToHexadeimal(featureBackground) + BR777 +
                                ProjectProgress.convertColorToHexadeimal(featureForeground)

                );
                result.append(BR5);
                for (ProjectPreparationIssue preparationIssue : rfd.PreparationIssues)
                {
                    result.append(
                        preparationIssue.issueTypeName + BR777 +
                            preparationIssue.issueName + " " + preparationIssue.issueKey + BR777 +
                            ConvertDateToOurFormat(preparationIssue.createdDate) + BR777 +
                            ConvertDateToOurFormat(preparationIssue.dueDate) + BR777 +
                            "15.0" + BR777 + //percentage completed
                            preparationIssue.assigneeName + BR777 +
                            preparationIssue.issueState
                    );
                    result.append(BR4);
                }
                result.append(BR3);
            }

            result.append(BR1);
        }


        return result;
    }

    public class MonthlyRoadmapFeatures {
        public String monthYear;
        public ArrayList<RoadmapFeatureDescriptor> roadMapFeaturesInThatMonth = new ArrayList<>();
    }
    public String ConvertDateToOurFormat(Date dateToConvert)
    {
        SimpleDateFormat outputDateFormat = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT);
        return outputDateFormat.format(dateToConvert);
    }
    static String getMonth(Date date)
    {
        String[] monthString = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return monthString[c.get(Calendar.MONTH)];
    }
    static int getYear(Date date)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.YEAR);
    }
}

