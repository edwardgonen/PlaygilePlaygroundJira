package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.api.ProjectPreparation;
import com.playgileplayground.jira.jiraissues.JiraInterface;

/**
 * Created by Ext_EdG on 8/8/2020.
 */
public class ProjectPreparationMisc {
    private JiraInterface jiraInterface;

    public ProjectPreparationMisc(JiraInterface jiraInterface)
    {
        this.jiraInterface = jiraInterface;
    }


    public ProductPreparationIssue identifyProductPreparationIssue(Issue issue)
    {
        ProductPreparationIssue result = new ProductPreparationIssue();

        if (issue == null) return result; //wrong issue

        //1. get issue key
        String issueKey = issue.getKey();

        //Monetization(PKM), BI(BIT), BA(PKBA), Economics (PKEC)

        if (issueKey.contains("PKM")) {
            result.issueTypeKey = "PKM";
            result.issueTypeName = "Monetization";
        }
        if (issueKey.contains("BIT")) {
            result.issueTypeKey = "BIT";
            result.issueTypeName = "Business Intelligence";
        }
        if (issueKey.contains("PKBA")) {
            result.issueTypeKey = "PKBA";
            result.issueTypeName = "Business Analytics";
        }
        if (issueKey.contains("PKEC")) {
            result.issueTypeKey = "PKEC";
            result.issueTypeName = "Economy";
        }
        //get start date

        //get due date

        return result;
    }
}


