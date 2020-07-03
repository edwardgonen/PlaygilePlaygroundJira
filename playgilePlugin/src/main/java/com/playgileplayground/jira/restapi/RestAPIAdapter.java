package com.playgileplayground.jira.restapi;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.util.json.JSONObject;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.parser.JSONParser;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.ScriptObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Ext_EdG on 7/5/2020.
 */
public class RestAPIAdapter {
    String baseUrl;
    public RestAPIAdapter()
    {
        this.baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
    }

    public boolean Get(String restEndpoint,String returnObject)
    {
        boolean result = false;
        try {
            URL jiraUrl = new URL(baseUrl + restEndpoint);
            HttpURLConnection conn = (HttpURLConnection)jiraUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            int responsecode = conn.getResponseCode();
            if(responsecode != 200) {
                //throw new RuntimeException(“HttpResponseCode: “ +responsecode);
                return result;
            }
            Scanner sc = new Scanner(jiraUrl.openStream());
            String inline = "";
            while(sc.hasNext())
            {
                inline += sc.nextLine();
            }
            sc.close();
            /*
            Global global = Context.getGlobal();
            JSONParser parser = new JSONParser(inline, global, true);
            returnObject = (JSONObject)parser.parse();
            */
            returnObject = inline;
            System.out.println("@@@ " + inline);
            result = true;
            return result;
        }
        catch (MalformedURLException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return result;
    }
}
