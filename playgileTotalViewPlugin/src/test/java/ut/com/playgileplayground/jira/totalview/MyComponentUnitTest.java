package ut.com.playgileplayground.jira.totalview;

import org.junit.Test;
import com.playgileplayground.jira.totalview.api.MyPluginComponent;
import com.playgileplayground.jira.totalview.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}