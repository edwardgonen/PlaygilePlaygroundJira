/**
 * Created by Ext_EdG on 11/12/2020.
 */
package com.playgileplayground.jira.servlet;

    import com.atlassian.activeobjects.external.ActiveObjects;
    import com.atlassian.activeobjects.tx.Transactional;
    import com.atlassian.jira.component.ComponentAccessor;
    import com.atlassian.jira.security.JiraAuthenticationContext;
    import com.atlassian.jira.user.ApplicationUser;
    import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
    import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
    import com.atlassian.sal.api.transaction.TransactionCallback;
    import com.atlassian.templaterenderer.TemplateRenderer;
    import com.playgileplayground.jira.persistence.ManageActiveObjects;
    import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
    import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
    import com.playgileplayground.jira.persistence.UserLastLocations;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import javax.servlet.*;
    import javax.servlet.http.HttpServlet;
    import javax.servlet.http.HttpServletRequest;
    import javax.servlet.http.HttpServletResponse;
    import java.io.IOException;
    import java.text.ParseException;
    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.HashMap;
    import java.util.Map;
    import java.util.Optional;

@Scanned
public class pluginConfiguration extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(activeObjectsAccess.class);
    @ComponentImport
    TemplateRenderer templateRenderer;
    ActiveObjects ao;

    private static final String PLUGIN_CONFIGURATION_TEMPLATE = "/templates/plugin-configuration.vm";

    public pluginConfiguration(ActiveObjects ao,TemplateRenderer templateRenderer)
    {
        this.ao = ao;
        this.templateRenderer = templateRenderer;
    }
    @Override
    @Transactional
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        ao.executeInTransaction((TransactionCallback<Void>) () -> {
            String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
            String propertyName = Optional.ofNullable(req.getParameter("propertyName")).orElse("");
            String propertyValue = Optional.ofNullable(req.getParameter("propertyValue")).orElse("");
            String roadmapFeature = Optional.ofNullable(req.getParameter("roadmapFeature")).orElse("");


            String action = Optional.ofNullable(req.getParameter("actionType")).orElse("");

            Map<String, Object> context = new HashMap<>();
            resp.setContentType("text/html;charset=utf-8");

            context.put("test", "my answer");

            try {
                templateRenderer.render(PLUGIN_CONFIGURATION_TEMPLATE, context, resp.getWriter());
            }
            catch (IOException e) {
                String aaa = e.toString();
            }


/*

            ManageActiveObjectsResult maor = new ManageActiveObjectsResult();
            servletMisc.responseToWeb("Succeeded", maor, resp);
*/

            return null;
        });
    }
}
