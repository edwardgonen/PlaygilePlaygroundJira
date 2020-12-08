package com.playgileplayground.jira.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created on 7/16/2020.
 */
public class servletMisc {

    static void simpleResponseToWeb(String response, HttpServletResponse resp)
    {
        try {
            resp.getWriter().write(response);
        } catch (IOException ignored) {
        }
    }
    static void responseToWeb(String response, ManageActiveObjectsResult maor, HttpServletResponse resp)
    {
        String answer;
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
        {
            answer = response;
        } else
        {
            answer = "Failure " + maor.Message;
        }
        simpleResponseToWeb(answer, resp);
    }

    static String serializeToJsonAndSend(Object object, HttpServletResponse resp)
    {
        String result;
        ObjectMapper jsonMapper = new ObjectMapper();
        try {
            result = jsonMapper.writeValueAsString(object);
            simpleResponseToWeb(result, resp);
        }
        catch (Exception e)
        {
            result = null;
        }
        return result;
    }

    static void responseListToWeb(ManageActiveObjectsResult maor, HttpServletResponse resp)
    {
        String answer;
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
        {
            StringBuilder output = new StringBuilder();
            ArrayList<String> allEntities = (ArrayList<String>)maor.Result;
            for (String entity : allEntities)
            {
                output.append(entity).append("<br>");
            }
            answer = output.toString();
        }
        else
        {
            answer = "Failure " + maor.Message;
        }
        simpleResponseToWeb(answer, resp);
    }
}
