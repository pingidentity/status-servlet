/*
 * Copyright 2016-2018 Ping Identity Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.unboundid.ops;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ops.models.ServerStatus;
import com.unboundid.ops.models.Status;
import com.unboundid.ops.models.StatusError;
import com.unboundid.ops.models.LoadBalancingAlgorithmStatus;
import com.unboundid.ops.models.ServletStatus;
import com.unboundid.ops.models.StoreAdapterStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An LDAP client for a server's monitor backend.
 *
 * @author Jacob Childress
 */
public class StatusClient
{
  private final LDAPInterface connection;
  private List<String> servletsToCheck = new ArrayList<>();


  /**
   * Constructor for the status client.
   *
   * @param connection
   *          An LDAP connection interface.
   * @param servletsToCheck
   *          The HTTP servlets that must be enabled for the server to be
   *          considered available.
   * @throws LDAPException
   */
  public StatusClient(LDAPInterface connection,
                      String... servletsToCheck) throws LDAPException
  {
    this.connection = connection;
    this.servletsToCheck = Arrays.asList(servletsToCheck);
  }


  /**
   * Gets the server status.
   *
   * @return A {@link Status} instance.
   */
  public Status getStatus()
  {
    try
    {
      ServerStatus serverStatus =
              getServerStatus();
      List<ServletStatus> servletStatuses =
              getServletStatuses();
      List<StoreAdapterStatus> storeAdapterStatuses =
              getStoreAdapterStatuses();
      List<LoadBalancingAlgorithmStatus> lbaStatuses =
              getLoadBalancingAlgorithmStatuses();
      return Status.create(serverStatus,
                           servletStatuses,
                           storeAdapterStatuses,
                           lbaStatuses);
    }
    catch (Exception e)
    {
      return Status.create(new StatusError(e));
    }
  }


  private ServerStatus getServerStatus() throws Exception
  {
    SearchResult result =
            findMonitorEntries("ds-general-monitor-entry");
    if (result.getEntryCount() != 1)
    {
      throw new Exception(String.format(
              "Expected one and only one general monitor entry; " +
                      "actual number was %d", result.getEntryCount()));
    }
    SearchResultEntry entry = result.getSearchEntries().get(0);
    return new ServerStatus(
            entry.getAttributeValues("unavailable-alert-type"),
            entry.getAttributeValues("degraded-alert-type"));
  }


  private List<ServletStatus> getServletStatuses() throws Exception
  {
    List<String> enabledServlets = new ArrayList<>();
    List<ServletStatus> servletStatuses = new ArrayList<>();
    if (!servletsToCheck.isEmpty())
    {
      SearchResult result =
              findMonitorEntries("ds-http-servlet-config-monitor-entry",
                                 "enabled-servlet-and-path");
      if (result.getEntryCount() != 1)
      {
        throw new Exception(String.format(
                "Expected one and only one HTTP servlet config monitor entry; " +
                        "actual number was %d", result.getEntryCount()));
      }
      SearchResultEntry entry = result.getSearchEntries().get(0);
      for (String servletAndPath : entry.getAttributeValues("enabled-servlet-and-path"))
      {
        final String parsedServletName = parseServletName(servletAndPath);
        if (parsedServletName != null)
        {
          enabledServlets.add(parsedServletName);
        }
      }
      for (String servletToCheck : servletsToCheck)
      {
        if (enabledServlets.contains(servletToCheck))
        {
          servletStatuses.add(new ServletStatus(servletToCheck, true));
        }
        else
        {
          servletStatuses.add(new ServletStatus(servletToCheck, false));
        }
      }
    }
    return servletStatuses;
  }


  private List<StoreAdapterStatus> getStoreAdapterStatuses() throws LDAPException
  {
    List<StoreAdapterStatus> storeAdapterStatuses = new ArrayList<>();
    SearchResult result =
            findMonitorEntries("ds-store-adapter-monitor-entry",
                               "store-adapter-name", "store-adapter-status");
    for (SearchResultEntry entry : result.getSearchEntries())
    {
      storeAdapterStatuses.add(new StoreAdapterStatus(
              entry.getAttributeValue("store-adapter-name"),
              parseAvailabilityString(entry.getAttributeValue("store-adapter-status"))));
    }
    return storeAdapterStatuses;
  }


  private List<LoadBalancingAlgorithmStatus> getLoadBalancingAlgorithmStatuses()
          throws LDAPException
  {
    List<LoadBalancingAlgorithmStatus> lbaStatuses = new ArrayList<>();
    SearchResult result =
            findMonitorEntries("ds-load-balancing-algorithm-monitor-entry", "*");
    for (SearchResultEntry entry : result.getSearchEntries())
    {
      lbaStatuses.add(new LoadBalancingAlgorithmStatus(
              entry.getAttributeValue("algorithm-name"),
              parseAvailabilityString(entry.getAttributeValue("health-check-state")),
              entry.getAttributeValueAsInteger("num-available-servers"),
              entry.getAttributeValueAsInteger("num-degraded-servers"),
              entry.getAttributeValueAsInteger("num-unavailable-servers")));
    }
    return lbaStatuses;
  }


  private SearchResult findMonitorEntries(String objectClass,
                                          String... attributes) throws LDAPException
  {
    return connection.search(
            "cn=monitor", SearchScope.SUB,
            Filter.createEqualityFilter("objectClass", objectClass),
            attributes);
  }


  private String parseServletName(String monitorAttributeValue)
  {
    Pattern pattern = Pattern.compile("^(.+) (http(.+))$");
    Matcher matcher = pattern.matcher(monitorAttributeValue);
    if (matcher.matches())
    {
      return matcher.group(1);
    }
    return null;
  }


  private boolean parseAvailabilityString(String availabilityString)
  {
    return availabilityString.equalsIgnoreCase("AVAILABLE");
  }
}
