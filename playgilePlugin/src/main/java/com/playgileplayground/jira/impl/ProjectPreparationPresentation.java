package com.playgileplayground.jira.impl;

import java.awt.*;
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
        for (Date date = firstDate; date.before(lastDate) || date.equals(lastDate); date = DateTimeUtils.AddMonths(date, 1)) {
            //go through the list and build our array and find year/month
            MonthlyRoadmapFeatures monthlyRoadmapFeatures = null;
            for (RoadmapFeatureDescriptor feature : ourRoadmapFeatures)
            {
                if (DateTimeUtils.CompareDatesByMonthYear(feature.BusinessApprovalDate, date))
                {
                    if (monthlyRoadmapFeatures == null) {
                        monthlyRoadmapFeatures = new MonthlyRoadmapFeatures();
                        monthlyRoadmapFeatures.monthYear = DateTimeUtils.getMonth(date) + ", " + DateTimeUtils.getYear(date);
                        allRoadmapFeatures.add(monthlyRoadmapFeatures);
                    }
                    monthlyRoadmapFeatures.roadMapFeaturesInThatMonth.add(feature);
                }
            }
        }

        return result;
    }

    enum IssueTardiness {
        OK,
        LATE,
        TOO_LATE
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


                StringBuilder issuesStrings = new StringBuilder();
                IssueTardiness featureTardiness = IssueTardiness.OK;
                Color featureBackgroundColor = Color.GREEN;
                Color featureForegroundColor = Color.RED;

                for (ProjectPreparationIssue preparationIssue : rfd.PreparationIssues)
                {
                    float completeness = 0;


                    if (preparationIssue.issueState == ProjectPreparationIssue.IssueState.ACTIVE) {
                        if (preparationIssue.getStartDate().after(today)) //not started yet
                        {
                            //don't change the tardiness
                        } else {
                            if (preparationIssue.getDueDate().before(today)) //too late
                            {
                                featureTardiness = IssueTardiness.TOO_LATE;
                            } else //we are within issue now
                            {
                                int duration = DateTimeUtils.AbsDays(preparationIssue.getDueDate(), preparationIssue.getStartDate());
                                int daysSinceStart = DateTimeUtils.AbsDays(today, preparationIssue.getStartDate());
                                completeness = Math.round((float)daysSinceStart / (float)duration * 100.0f);
                                if (completeness > 90.0)
                                {
                                    if (featureTardiness != IssueTardiness.TOO_LATE) featureTardiness = IssueTardiness.LATE;
                                }
                                else
                                {
                                    //don't touch
                                }
                            }
                        }
                    } else //not active
                    {
                        //don't touch the tardiness
                    }



                    switch (featureTardiness)
                    {
                        case OK:
                            featureBackgroundColor = Color.GREEN;
                            featureForegroundColor = Color.RED;
                            break;
                        case LATE:
                            featureBackgroundColor = Color.YELLOW;
                            featureForegroundColor = Color.RED;
                            break;
                        case TOO_LATE:
                            featureBackgroundColor = Color.RED;
                            featureForegroundColor = Color.YELLOW;
                            break;
                    }


                    issuesStrings.append(
                        preparationIssue.issueTypeName + BR777 +
                            preparationIssue.issueName + " " + preparationIssue.issueKey + BR777 +
                            DateTimeUtils.ConvertDateToOurFormat(preparationIssue.getStartDate()) + BR777 +
                            DateTimeUtils.ConvertDateToOurFormat(preparationIssue.getDueDate()) + BR777 +
                            completeness + BR777 + //percentage completed
                            preparationIssue.assigneeName + BR777 +
                            preparationIssue.issueState
                    );
                    issuesStrings.append(BR4);
                }


                result.append(rfd.Name + " " + rfd.Key + BR777 +
                    DateTimeUtils.ConvertDateToOurFormat(rfd.BusinessApprovalDate) + BR777 +
                                ProjectMonitoringMisc.convertColorToHexadeimal(featureBackgroundColor) + BR777 +
                                ProjectMonitoringMisc.convertColorToHexadeimal(featureForegroundColor)

                );
                result.append(BR5);
                result.append(issuesStrings);

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

}

