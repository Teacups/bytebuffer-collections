/*
 * Druid - a distributed column store.
 * Copyright (C) 2012, 2013  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package io.druid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.metamx.http.client.HttpClient;
import io.druid.client.DruidServer;
import io.druid.curator.discovery.ServerDiscoverySelector;
import io.druid.guice.annotations.Global;
import io.druid.guice.annotations.Json;
import io.druid.query.Druids;
import io.druid.query.TableDataSource;
import io.druid.query.aggregation.AggregatorFactory;
import io.druid.query.aggregation.CountAggregatorFactory;
import io.druid.query.spec.MultipleIntervalSegmentSpec;
import io.druid.query.timeboundary.TimeBoundaryQuery;
import io.druid.server.coordinator.rules.IntervalLoadRule;
import io.druid.server.coordinator.rules.Rule;
import junit.framework.Assert;
import org.joda.time.Interval;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 */
public class BrokerSelectorTest
{
  private BrokerSelector brokerSelector;

  @Before
  public void setUp() throws Exception
  {
    brokerSelector = new BrokerSelector(
        new TestRuleManager(null, null, null, null),
        new TierConfig()
        {
          @Override
          public LinkedHashMap<String, String> getTierToBrokerMap()
          {
            return new LinkedHashMap<String, String>(
                ImmutableMap.<String, String>of(
                    "hot", "hotBroker",
                    "medium", "mediumBroker",
                    DruidServer.DEFAULT_TIER, "coldBroker"
                )
            );
          }

          @Override
          public String getDefaultBrokerServiceName()
          {
            return "hotBroker";
          }
        }
    );
  }

  @Test
  public void testBasicSelect() throws Exception
  {
    String brokerName = brokerSelector.select(
        new TimeBoundaryQuery(
            new TableDataSource("test"),
            new MultipleIntervalSegmentSpec(Arrays.<Interval>asList(new Interval("2011-08-31/2011-09-01"))),
            null
        )
    );

    Assert.assertEquals("coldBroker", brokerName);
  }


  @Test
  public void testBasicSelect2() throws Exception
  {
    String brokerName = brokerSelector.select(
        new TimeBoundaryQuery(
            new TableDataSource("test"),
            new MultipleIntervalSegmentSpec(Arrays.<Interval>asList(new Interval("2013-08-31/2013-09-01"))),
            null
        )
    );

    Assert.assertEquals("hotBroker", brokerName);
  }

  @Test
  public void testSelectMatchesNothing() throws Exception
  {
    String brokerName = brokerSelector.select(
        new TimeBoundaryQuery(
            new TableDataSource("test"),
            new MultipleIntervalSegmentSpec(Arrays.<Interval>asList(new Interval("2010-08-31/2010-09-01"))),
            null
        )
    );

    Assert.assertEquals(null, brokerName);
  }


  @Test
  public void testSelectMultiInterval() throws Exception
  {
    String brokerName = brokerSelector.select(
        Druids.newTimeseriesQueryBuilder()
              .dataSource("test")
              .aggregators(Arrays.<AggregatorFactory>asList(new CountAggregatorFactory("count")))
              .intervals(
                  new MultipleIntervalSegmentSpec(
                      Arrays.<Interval>asList(
                          new Interval("2013-08-31/2013-09-01"),
                          new Interval("2012-08-31/2012-09-01"),
                          new Interval("2011-08-31/2011-09-01")
                      )
                  )
              ).build()
    );

    Assert.assertEquals("coldBroker", brokerName);
  }

  @Test
  public void testSelectMultiInterval2() throws Exception
  {
    String brokerName = brokerSelector.select(
        Druids.newTimeseriesQueryBuilder()
              .dataSource("test")
              .aggregators(Arrays.<AggregatorFactory>asList(new CountAggregatorFactory("count")))
              .intervals(
                  new MultipleIntervalSegmentSpec(
                      Arrays.<Interval>asList(
                          new Interval("2011-08-31/2011-09-01"),
                          new Interval("2012-08-31/2012-09-01"),
                          new Interval("2013-08-31/2013-09-01")
                      )
                  )
              ).build()
    );

    Assert.assertEquals("coldBroker", brokerName);
  }

  private static class TestRuleManager extends CoordinatorRuleManager
  {
    public TestRuleManager(
        @Global HttpClient httpClient,
        @Json ObjectMapper jsonMapper,
        Supplier<TierConfig> config,
        ServerDiscoverySelector selector
    )
    {
      super(httpClient, jsonMapper, config, selector);
    }

    @Override
    public boolean isStarted()
    {
      return true;
    }

    @Override
    public List<Rule> getRulesWithDefault(String dataSource)
    {
      return Arrays.<Rule>asList(
          new IntervalLoadRule(new Interval("2013/2014"), ImmutableMap.<String, Integer>of("hot", 1), null, null),
          new IntervalLoadRule(new Interval("2012/2013"), ImmutableMap.<String, Integer>of("medium", 1), null, null),
          new IntervalLoadRule(
              new Interval("2011/2012"),
              ImmutableMap.<String, Integer>of(DruidServer.DEFAULT_TIER, 1),
              null,
              null
          )
      );
    }
  }
}
