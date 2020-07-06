package com.playgileplayground.jira.restapi;

import com.atlassian.jira.util.json.JSONObject;

/**
 * Created by Ext_EdG on 7/5/2020.
 */
public class RestAPI {

    public String TestGet()
    {
        RestAPIAdapter raa = new RestAPIAdapter();
        String returnResult = "";
        if (raa.Get("/rest/api/2/project", returnResult))
        {
            System.out.println("$$$ " + returnResult.toString());
        }
        else
        {
            System.out.println("### Error");
        }
        return returnResult.toString();
    }
}
