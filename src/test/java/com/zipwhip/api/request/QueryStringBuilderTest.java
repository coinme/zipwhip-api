package com.zipwhip.api.request;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: jed
 * Date: 8/29/11
 * Time: 1:54 PM
 */
public class QueryStringBuilderTest {

    QueryStringBuilder builder;

    @Before
    public void setUp() throws Exception {
        builder = new QueryStringBuilder();
    }

    @Test
    public void testBuildBasic() throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("1","2");
        params.put("3","4");

        builder.params(params);
        String query1 = builder.build();

        Assert.assertEquals("?3=4&1=2", query1);

        builder = new QueryStringBuilder();
        builder.param("3","4", false);
        builder.param("1","2", false);
        String query2 = builder.build();

        Assert.assertEquals(query1, query2);
    }

    @Test
    public void testBuildCollection() throws Exception {

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("a","1");
        params.put("b","2");

        List<String> c = new ArrayList<String>();
        c.add("3");
        c.add("4");
        c.add("5");

        params.put("c", c);

        builder.params(params);
        String query = builder.build();

        Assert.assertNotNull(query);
        Assert.assertEquals("?b=2&c=3&c=4&c=5&a=1", query);
    }

}
