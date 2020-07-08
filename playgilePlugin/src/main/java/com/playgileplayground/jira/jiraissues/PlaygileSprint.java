package com.playgileplayground.jira.jiraissues;

import org.joda.time.DateTime;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Ext_EdG on 7/7/2020.
 */
public class PlaygileSprint implements Comparable<PlaygileSprint>{
    /*
    [com.atlassian.greenhopper.service.sprint.Sprint@5b3d69da
        [
            id=2,rapidViewId=1,
            state=FUTURE,name=FAPFP Sprint 1,
            startDate=<null>,
            endDate=<null>,
            completeDate=<null>,
            sequence=2,
            goal=<null>
        ]
    ]
     */
    private long id = -1;
    private long rapidViewId = -1;
    private SprintState state = SprintState.UNDEFINED;
    private String name = "";
    private Date startDate = new Date();
    private Date endDate = new Date();
    private Date completeDate = new Date();
    private long sequence;
    private String goal = "";

    //parse the string
    public PlaygileSprint parse(String input)
    {
        PlaygileSprint result = null;

        if (input != null && !input.isEmpty())
        {
            System.out.println("input = " + input);
            //find second "["
            int beginIndex = input.lastIndexOf("[");
            if (beginIndex >= 0) {
                int endIndex = input.indexOf("]", beginIndex);
                if (endIndex > beginIndex)
                {
                    //get the substring
                    String sprintInfo = input.substring(beginIndex + 1, endIndex);
                    if (!sprintInfo.isEmpty())
                    {
                        //split it
                        String[] items = sprintInfo.split(",");
                        if (items != null && items.length > 0)
                        {
                            for (String item : items)
                            {
                                //split
                                String[] pair = item.split("=");
                                if (pair != null && pair.length == 2)
                                {
                                    ConvertToField(pair[0], pair[1]);
                                }
                            }
                            result = this;
                        }
                    }
                }
            }
        }
        return result;
    }
    @Override
    public int compareTo(PlaygileSprint o) {
        return getStartDate().compareTo(o.getStartDate());
    }

    public String toString()
    {
        String strStartDate = startDate == null?"": startDate.toString();
        String strEndDate = endDate == null?"": endDate.toString();

        return "id=" + id + ","
            + "name=" + name + ","
            + "state=" + state + ","
            + "startDate=" + strStartDate + ","
            + "endDate=" + strEndDate;
    }


    public long getId() {
        return id;
    }

    public long getRapidViewId() {
        return rapidViewId;
    }

    public SprintState getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getCompleteDate() {
        return completeDate;
    }

    public long getSequence() {
        return sequence;
    }

    public String getGoal() {
        return goal;
    }

    private boolean ConvertToField(String itemName, String value)
    {
        boolean result = true;
        switch (itemName)
        {
            case "id":
                try {
                    id = Long.parseLong(value);
                }
                catch (NumberFormatException ex)
                {
                    id = -1;
                }
                break;
            case "rapidViewId":
                try {
                    rapidViewId = Long.parseLong(value);
                }
                catch (NumberFormatException ex)
                {
                    rapidViewId = -1;
                }
                break;
            case "state":
                switch (value)
                {
                    case "FUTURE":
                        state = SprintState.FUTURE;
                        break;
                    case "CLOSED":
                        state = SprintState.CLOSED;
                        break;
                    case "ACTIVE":
                        state = SprintState.ACTIVE;
                        break;
                    default:
                        state = SprintState.UNDEFINED;
                        break;
                }
                break;
            case "name":
                if (!value.equals("<null>"))
                    name = value;
                else
                    name = "";
                break;
            case "startDate":
                try
                {
                    startDate = new DateTime(value).toDate();
                }
                catch (Exception ex)
                {
                    startDate = null;
                }
                break;
            case "endDate":
                try
                {
                    endDate = new DateTime(value).toDate();
                }
                catch (Exception ex)
                {
                    endDate = null;
                }
                break;
            case "completeDate":
                try
                {
                    completeDate = new DateTime(value).toDate();
                }
                catch (Exception ex)
                {
                    completeDate = null;
                }
                break;
            case "sequence":
                try {
                    sequence = Long.parseLong(value);
                }
                catch (NumberFormatException ex)
                {
                    sequence = -1;
                }
                break;
            case "goal":
                if (!value.equals("<null>"))
                    goal = value;
                else
                    goal = "";
                break;
            default:
                result = false;
                break;
        }
        return result;
    }
}
