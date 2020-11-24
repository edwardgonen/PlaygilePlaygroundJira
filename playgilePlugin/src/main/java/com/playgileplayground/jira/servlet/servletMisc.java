package com.playgileplayground.jira.servlet;

import com.atlassian.templaterenderer.TemplateRenderer;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public class servletMisc {

    static void renderAndResponseToWeb(TemplateRenderer templateRenderer, String templateName, Map<String, Object> context, HttpServletResponse resp)
    {
        try {
            templateRenderer.render(templateName, context, resp.getWriter());
        } catch (IOException e) {
            ManageActiveObjectsResult maorLocal = new ManageActiveObjectsResult();
            maorLocal.Code = ManageActiveObjectsResult.STATUS_CODE_DATA_FORMAT_PARSING_ERROR;
            maorLocal.Message = "Failed to render list of features " + e.toString();
            servletMisc.responseToWeb("", maorLocal, resp);
        }
    }
    static void responseToWeb(String response, ManageActiveObjectsResult maor, HttpServletResponse resp)
    {
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
        {
            try {
                resp.getWriter().write(response);
            } catch (IOException e) {
            }
        } else
        {
            try {
                resp.getWriter().write("Failure " + maor.Message);
            } catch (IOException e) {
            }
        }
    }
    static void responseListToWeb(ManageActiveObjectsResult maor, HttpServletResponse resp)
    {
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
        {
            StringBuilder output = new StringBuilder();
            ArrayList<String> allEntities = (ArrayList<String>) maor.Result;
            for (String entity : allEntities)
            {
                output.append(entity + "<br>");
            }
            try {
                resp.getWriter().write(output.toString());
            } catch (IOException e) {
            }
        }
        else
        {
            try {
                resp.getWriter().write("Failure " + maor.Message);
            } catch (IOException e) {
            }
        }
    }
}
