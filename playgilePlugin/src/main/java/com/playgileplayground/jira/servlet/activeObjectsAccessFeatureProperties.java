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
    import com.atlassian.sal.api.transaction.TransactionCallback;
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
    import java.util.Optional;

@Scanned
public class activeObjectsAccessFeatureProperties extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(activeObjectsAccess.class);
    ActiveObjects ao;
    public activeObjectsAccessFeatureProperties(ActiveObjects ao)
    {
        this.ao = ao;
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


            if (projectKey.isEmpty() || roadmapFeature.isEmpty() || propertyName.isEmpty())
            {
                try {
                    resp.getWriter().write("Empty project key or feature or property name");
                } catch (IOException e) {
                }
                return null;
            }

            ManageActiveObjectsEntityKey key =  new ManageActiveObjectsEntityKey(projectKey, roadmapFeature);
            ManageActiveObjects mao = new ManageActiveObjects(this.ao);
            ManageActiveObjectsResult maor = mao.CreateProjectEntity(key); //will not create if exists
            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS || maor.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {
                //set team velocity if available

                switch (propertyName)
                {
                    case "sprintLengthValue":
                        //set default sprint length value
                        double sprintLength = 0;
                        try {
                            if (!propertyValue.isEmpty())
                            {
                                sprintLength = Double.parseDouble(propertyValue);
                                maor = mao.SetSprintLength(key, sprintLength);
                                servletMisc.responseToWeb("Succeeded", maor, resp);
                            }
                            else //get value
                            {
                                maor = mao.GetSprintLength(key);
                                servletMisc.responseToWeb(maor.Result.toString(), maor, resp);
                            }
                        }
                        catch (Exception ex)
                        {
                            try {
                                resp.getWriter().write("Wrong value " + propertyValue);
                            } catch (IOException e) {
                            }
                            return null;
                        }
                        break;
                    case "defEstimValue":
                        //set default estimation value for not estimated issues
                        double defNotEstimdIssueValue = 0;
                        try {
                            if (!propertyValue.isEmpty())
                            {
                                defNotEstimdIssueValue = Double.parseDouble(propertyValue);
                                maor = mao.SetDefaultNotEstimatedIssueValue(key, defNotEstimdIssueValue);
                                servletMisc.responseToWeb("Succeeded", maor, resp);
                            }
                            else //get value
                            {
                                maor = mao.GetDefaultNotEstimatedIssueValue(key);
                                servletMisc.responseToWeb(maor.Result.toString(), maor, resp);
                            }
                        }
                        catch (Exception ex)
                        {
                            try {
                                resp.getWriter().write("Wrong value " + propertyValue);
                            } catch (IOException e) {
                            }
                            return null;
                        }
                        break;
                    default:
                            try {
                                resp.getWriter().write("Unsupported property name " + propertyName);
                            } catch (IOException e) {
                            }
                            return null;
                }

            }
            else
            {
                try {
                    resp.getWriter().write("Failure " + maor.Message);
                } catch (IOException e) {
                }
            }
            return null;
        });
    }
}
