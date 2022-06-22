package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import com.playgileplayground.jira.persistence.UserLastLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Scanned
public class ActiveObjectsAccess extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(ActiveObjectsAccess.class);
    ActiveObjects ao;
    public ActiveObjectsAccess(ActiveObjects ao)
    {
        this.ao = ao;
    }
    @Override
    @Transactional
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        ao.executeInTransaction((TransactionCallback<Void>) () -> {
            String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
            String currentUser = Optional.ofNullable(req.getParameter("user")).orElse("");
            String teamVelocity = Optional.ofNullable(req.getParameter("teamVelocity")).orElse("");
            String roadmapFeature = Optional.ofNullable(req.getParameter("feature")).orElse("");


            if (projectKey.isEmpty())
            {
                try {
                    resp.getWriter().write("Empty project key");
                } catch (IOException ignored) {
                }
                return null;
            }

            //set team velocity if exists
            double teamVelocityValue = 0;
            try {
                teamVelocityValue = Double.parseDouble(teamVelocity);
            }
            catch (Exception ex)
            {
                //nothing
            }

            ManageActiveObjectsEntityKey key =  new ManageActiveObjectsEntityKey(projectKey, roadmapFeature);
            ManageActiveObjects mao = new ManageActiveObjects(this.ao);
            ManageActiveObjectsResult maor = mao.CreateProjectEntity(key); //will not create if exists
            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS || maor.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {
                //set team velocity if available
                if (teamVelocityValue > 0)
                {
                    maor = mao.SetTeamVelocity(key, teamVelocityValue);
                }
                //add last locations for user
                maor = mao.CreateUserEntity(currentUser); //will not create if exists
                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS || maor.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {
                    UserLastLocations ulc = new UserLastLocations(projectKey, roadmapFeature);
                    mao.SetUserLastLocations(currentUser, ulc);
                    try {
                        resp.getWriter().write("Success");
                    } catch (IOException ignored) {
                    }
                }
            }
            else
            {
                try {
                    resp.getWriter().write("Failure " + maor.Message);
                } catch (IOException ignored) {
                }
            }
            return null;
        });
    }
}
