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

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ops.models.MonitorAvailabilityCriteria;
import com.unboundid.ops.models.MonitorStatus;
import com.unboundid.ops.models.Status;
import com.unboundid.ops.models.LoadBalancingAlgorithmStatus;
import com.unboundid.ops.models.ServletStatus;
import com.unboundid.ops.models.StatusError;
import com.unboundid.ops.models.StoreAdapterStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link StatusClient}.
 */
public class StatusClientTest
{
  private static final String BASE_DN = "dc=example,dc=com";
  private static final List<String> SERVLETS_TO_CHECK =
      new ArrayList<>(Collections.singletonList("Monitored Servlet"));
  private static final String MONITOR_NAME_1 = "Test Monitor 1";
  private static final String MONITOR_NAME_2 = "Test Monitor 2";
  private static List<MonitorAvailabilityCriteria> MONITORS_TO_CHECK;
  private static final List<MonitorAvailabilityCriteria> IGNORE_MONITORS =
      Collections.emptyList();

  static
  {
    try
    {
      MonitorAvailabilityCriteria monitorEntryInfo1 =
          MonitorAvailabilityCriteria
              .create(MONITOR_NAME_1 + ":state:READY,running");
      MonitorAvailabilityCriteria monitorEntryInfo2 =
          MonitorAvailabilityCriteria.create(MONITOR_NAME_2 + ":available:true");
      MONITORS_TO_CHECK = new ArrayList<>(Arrays.asList(
          monitorEntryInfo1, monitorEntryInfo2));
    }
    catch (LDAPException e)
    {
      // Ignore. Won't happen.
    }
  }

  private InMemoryDirectoryServer ds;


  /**
   * Starts an in-memory DS.
   */
  @BeforeClass
  public void setup() throws Exception
  {
    InMemoryDirectoryServerConfig config =
            new InMemoryDirectoryServerConfig(BASE_DN);
    config.setBaseDNs("cn=monitor");
    config.setEnforceSingleStructuralObjectClass(false);
    config.setEnforceAttributeSyntaxCompliance(false);
    config.setSchema(null);
    ds = new InMemoryDirectoryServer(config);
    ds.startListening();
  }


  /**
   * Shuts down the in-memory DS.
   *
   * @throws Exception
   */
  @AfterClass
  public void cleanup() throws Exception
  {
    ds.shutDown(true);
  }


  @Test
  public void serverOkTest() throws Exception
  {
    ds.clear();
    addBaseEntry(new String[0], new String[0]);

    Entry servletEntry = createServletEntry(
            "Monitored Servlet https://example.com/monitoredServlet",
            "Unmonitored Servlet https://example.com/unmonitoredServlet");
    Entry monitorEntry1 = createMonitorEntry(MONITOR_NAME_1, "state", "ready");
    Entry monitorEntry2 = createMonitorEntry(MONITOR_NAME_2, "available", "true");
    Entry lbaEntry = createLoadBalancingAlgorithmEntry(
            "User Store LBA", "AVAILABLE", 1, 0, 0);
    Entry storeAdapterEntry = createStoreAdapterEntry(
            "UserStoreAdapter", "AVAILABLE");

    ds.add(servletEntry);
    ds.add(monitorEntry1);
    ds.add(monitorEntry2);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      StatusClient client =
              new StatusClient(connection, SERVLETS_TO_CHECK, MONITORS_TO_CHECK);
      Status status = client.getStatus();
      assertTrue(status.isOK());

      assertEquals(status.getServerStatus(), "available");
      assertEquals(status.getServerAlerts().size(), 0);

      List<ServletStatus> servletStatusList = status.getServletStatuses();
      assertEquals(servletStatusList.size(), 1);
      ServletStatus servletStatus = servletStatusList.get(0);
      assertEquals(servletStatus.getName(), "Monitored Servlet");
      assertTrue(servletStatus.isEnabled());

      List<MonitorStatus> monitorStatusList = status.getMonitorStatuses();
      assertEquals(monitorStatusList.size(), 2);
      for (MonitorStatus monitorStatus : monitorStatusList)
      {
        assertNotNull(monitorStatus.getName());
        assertTrue(monitorStatus.isAvailable());
        assertNotNull(monitorStatus.getMonitorProperties());
      }

      List<LoadBalancingAlgorithmStatus> lbaStatusList =
              status.getLoadBalancingAlgorithmStatuses();
      assertEquals(lbaStatusList.size(), 1);
      LoadBalancingAlgorithmStatus lbaStatus = lbaStatusList.get(0);
      assertEquals(lbaStatus.getName(), "User Store LBA");
      assertTrue(lbaStatus.isAvailable());
      assertEquals(lbaStatus.getNumAvailableServers(), 1);
      assertEquals(lbaStatus.getNumDegradedServers(), 0);
      assertEquals(lbaStatus.getNumUnavailableServers(), 0);

      List<StoreAdapterStatus> storeAdapterStatusList =
              status.getStoreAdapterStatuses();
      assertEquals(storeAdapterStatusList.size(), 1);
      StoreAdapterStatus storeAdapterStatus = storeAdapterStatusList.get(0);
      assertEquals(storeAdapterStatus.getName(), "UserStoreAdapter");
      assertTrue(storeAdapterStatus.isAvailable());
    }
    finally
    {
      connection.close();
    }
  }


  @Test
  public void monitorNotOkTest() throws Exception
  {
    ds.clear();
    addBaseEntry(new String[0], new String[0]);

    Entry servletEntry = createServletEntry(
        "Monitored Servlet https://example.com/monitoredServlet",
        "Unmonitored Servlet https://example.com/unmonitoredServlet");
    Entry monitorEntry1 = createMonitorEntry(MONITOR_NAME_1, "state", "ready");
    Entry monitorEntry2 = createMonitorEntry(MONITOR_NAME_2, "available", "false");
    Entry lbaEntry = createLoadBalancingAlgorithmEntry(
        "User Store LBA", "AVAILABLE", 1, 0, 0);
    Entry storeAdapterEntry = createStoreAdapterEntry(
        "UserStoreAdapter", "AVAILABLE");

    ds.add(servletEntry);
    ds.add(monitorEntry1);
    ds.add(monitorEntry2);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      StatusClient client =
          new StatusClient(connection, SERVLETS_TO_CHECK, MONITORS_TO_CHECK);
      Status status = client.getStatus();
      assertFalse(status.isOK());

      List<ServletStatus> servletStatusList = status.getServletStatuses();
      assertEquals(servletStatusList.size(), 1);
      ServletStatus servletStatus = servletStatusList.get(0);
      assertEquals(servletStatus.getName(), "Monitored Servlet");
      assertTrue(servletStatus.isEnabled());

      List<MonitorStatus> monitorStatusList = status.getMonitorStatuses();
      assertEquals(monitorStatusList.size(), 2);
      for (MonitorStatus monitorStatus : monitorStatusList)
      {
        String name = monitorStatus.getName();
        if (name.equalsIgnoreCase(MONITOR_NAME_1))
        {
          assertTrue(monitorStatus.isAvailable());
        }
        if (name.equalsIgnoreCase(MONITOR_NAME_2))
        {
          assertFalse(monitorStatus.isAvailable());
        }
        assertNotNull(monitorStatus.getMonitorProperties());
      }

      List<LoadBalancingAlgorithmStatus> lbaStatusList =
          status.getLoadBalancingAlgorithmStatuses();
      assertEquals(lbaStatusList.size(), 1);
      LoadBalancingAlgorithmStatus lbaStatus = lbaStatusList.get(0);
      assertEquals(lbaStatus.getName(), "User Store LBA");
      assertTrue(lbaStatus.isAvailable());
      assertEquals(lbaStatus.getNumAvailableServers(), 1);
      assertEquals(lbaStatus.getNumDegradedServers(), 0);
      assertEquals(lbaStatus.getNumUnavailableServers(), 0);

      List<StoreAdapterStatus> storeAdapterStatusList =
          status.getStoreAdapterStatuses();
      assertEquals(storeAdapterStatusList.size(), 1);
      StoreAdapterStatus storeAdapterStatus = storeAdapterStatusList.get(0);
      assertEquals(storeAdapterStatus.getName(), "UserStoreAdapter");
      assertTrue(storeAdapterStatus.isAvailable());
    }
    finally
    {
      connection.close();
    }
  }


  @Test
  public void servletsNotOkTest() throws Exception
  {
    ds.clear();
    addBaseEntry(new String[0], new String[0]);

    Entry servletEntry = createServletEntry(
            "Unmonitored Servlet https://example.com/unmonitoredServlet");
    Entry lbaEntry = createLoadBalancingAlgorithmEntry(
            "User Store LBA", "AVAILABLE", 1, 0, 0);
    Entry storeAdapterEntry = createStoreAdapterEntry(
            "UserStoreAdapter", "AVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      StatusClient client =
              new StatusClient(connection, SERVLETS_TO_CHECK, IGNORE_MONITORS);
      Status status = client.getStatus();
      assertFalse(status.isOK());

      List<ServletStatus> servletStatusList = status.getServletStatuses();
      assertEquals(servletStatusList.size(), 1);
      ServletStatus servletStatus = servletStatusList.get(0);
      assertEquals(servletStatus.getName(), "Monitored Servlet");
      assertFalse(servletStatus.isEnabled());

      List<LoadBalancingAlgorithmStatus> lbaStatusList =
              status.getLoadBalancingAlgorithmStatuses();
      assertEquals(lbaStatusList.size(), 1);
      LoadBalancingAlgorithmStatus lbaStatus = lbaStatusList.get(0);
      assertEquals(lbaStatus.getName(), "User Store LBA");
      assertTrue(lbaStatus.isAvailable());

      List<StoreAdapterStatus> storeAdapterStatusList =
              status.getStoreAdapterStatuses();
      assertEquals(storeAdapterStatusList.size(), 1);
      StoreAdapterStatus storeAdapterStatus = storeAdapterStatusList.get(0);
      assertEquals(storeAdapterStatus.getName(), "UserStoreAdapter");
      assertTrue(storeAdapterStatus.isAvailable());
    }
    finally
    {
      connection.close();
    }
  }


  @Test
  public void lbaNotOkTest() throws Exception
  {
    ds.clear();
    addBaseEntry(new String[0], new String[0]);

    Entry servletEntry = createServletEntry(
            "Monitored Servlet https://example.com/monitoredServlet",
            "Unmonitored Servlet https://example.com/unmonitoredServlet");
    Entry lbaEntry = createLoadBalancingAlgorithmEntry(
            "User Store LBA", "UNAVAILABLE", 0, 0, 1);
    Entry storeAdapterEntry = createStoreAdapterEntry(
            "UserStoreAdapter", "AVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      StatusClient client =
              new StatusClient(connection, SERVLETS_TO_CHECK, IGNORE_MONITORS);
      Status status = client.getStatus();
      assertFalse(status.isOK());

      List<ServletStatus> servletStatusList = status.getServletStatuses();
      assertEquals(servletStatusList.size(), 1);
      ServletStatus servletStatus = servletStatusList.get(0);
      assertEquals(servletStatus.getName(), "Monitored Servlet");
      assertTrue(servletStatus.isEnabled());

      List<LoadBalancingAlgorithmStatus> lbaStatusList =
              status.getLoadBalancingAlgorithmStatuses();
      assertEquals(lbaStatusList.size(), 1);
      LoadBalancingAlgorithmStatus lbaStatus = lbaStatusList.get(0);
      assertEquals(lbaStatus.getName(), "User Store LBA");
      assertFalse(lbaStatus.isAvailable());

      List<StoreAdapterStatus> storeAdapterStatusList =
              status.getStoreAdapterStatuses();
      assertEquals(storeAdapterStatusList.size(), 1);
      StoreAdapterStatus storeAdapterStatus = storeAdapterStatusList.get(0);
      assertEquals(storeAdapterStatus.getName(), "UserStoreAdapter");
      assertTrue(storeAdapterStatus.isAvailable());
    }
    finally
    {
      connection.close();
    }
  }


  @Test
  public void storeAdapterNotOkTest() throws Exception
  {
    ds.clear();
    addBaseEntry(new String[0], new String[0]);

    Entry servletEntry = createServletEntry(
            "Monitored Servlet https://example.com/monitoredServlet",
            "Unmonitored Servlet https://example.com/unmonitoredServlet");
    Entry lbaEntry = createLoadBalancingAlgorithmEntry(
            "User Store LBA", "AVAILABLE", 1, 0, 0);
    Entry storeAdapterEntry = createStoreAdapterEntry(
            "UserStoreAdapter", "UNAVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      StatusClient client =
              new StatusClient(connection, SERVLETS_TO_CHECK, IGNORE_MONITORS);
      Status status = client.getStatus();
      assertFalse(status.isOK());

      List<ServletStatus> servletStatusList = status.getServletStatuses();
      assertEquals(servletStatusList.size(), 1);
      ServletStatus servletStatus = servletStatusList.get(0);
      assertEquals(servletStatus.getName(), "Monitored Servlet");
      assertTrue(servletStatus.isEnabled());

      List<LoadBalancingAlgorithmStatus> lbaStatusList =
              status.getLoadBalancingAlgorithmStatuses();
      assertEquals(lbaStatusList.size(), 1);
      LoadBalancingAlgorithmStatus lbaStatus = lbaStatusList.get(0);
      assertEquals(lbaStatus.getName(), "User Store LBA");
      assertTrue(lbaStatus.isAvailable());

      List<StoreAdapterStatus> storeAdapterStatusList =
              status.getStoreAdapterStatuses();
      assertEquals(storeAdapterStatusList.size(), 1);
      StoreAdapterStatus storeAdapterStatus = storeAdapterStatusList.get(0);
      assertEquals(storeAdapterStatus.getName(), "UserStoreAdapter");
      assertFalse(storeAdapterStatus.isAvailable());
    }
    finally
    {
      connection.close();
    }
  }

  @Test
  public void serverDegradedTest() throws Exception
  {
    ds.clear();

    // Server is degraded, but 'available'.
    addBaseEntry(new String[0],
                 new String[]{"low-disk-space-error"});

    LDAPConnection connection = ds.getConnection();
    try
    {
      StatusClient client = new StatusClient(connection);
      Status status = client.getStatus();
      assertFalse(status.isOK());

      assertEquals(status.getServerStatus(), "degraded");
      assertTrue(status.getServerAlerts().contains("low-disk-space-error"));

      assertFalse(status.isOK());
    }
    finally
    {
      connection.close();
    }
  }

  @Test
  public void serverUnavailableTest() throws Exception
  {
    ds.clear();

    // Server is both unavailable and degraded.
    addBaseEntry(new String[]{"server-shutting-down"},
                 new String[]{"low-disk-space-error"});

    try (LDAPConnection connection = ds.getConnection())
    {
      StatusClient client =
          new StatusClient(connection);
      Status status = client.getStatus();
      assertFalse(status.isOK());

      assertEquals(status.getServerStatus(), "unavailable");
      assertTrue(status.getServerAlerts().contains("server-shutting-down"));
      assertTrue(status.getServerAlerts().contains("low-disk-space-error"));

      assertFalse(status.isOK());
    }
  }

  @Test
  public void connectionFailureTest() throws Exception
  {
    try
    {
      LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
      connectionOptions.setAbandonOnTimeout(true);
      LDAPConnection connection = ds.getConnection(connectionOptions);

      ds.shutDown(true);

      StatusClient client =
              new StatusClient(connection);
      Status status = client.getStatus();

      assertEquals(status.getServerStatus(), "unknown");
      assertEquals(status.getServerAlerts().size(), 0);

      assertFalse(status.isOK());
      StatusError error = status.getError();
      assertNotNull(error);
      assertNotNull(error.getMessage());
      assertTrue(error.getMessage().contains("Socket") ||
                         error.getMessage().contains("connection"),
                 String.format("unexpected error message: '%s'",
                               error.getMessage()));
    }
    finally
    {
      ds.startListening();
    }
  }


  private void addBaseEntry(String[] unavailableAlerts,
                            String[] degradedAlerts) throws Exception
  {
    List<String> entry = new ArrayList<>(Arrays.asList(
        "dn: cn=monitor",
        "objectClass: top",
        "objectClass: ds-monitor-entry",
        "objectClass: ds-general-monitor-entry",
        "objectClass: extensibleObject",
        "cn: monitor"));
    for (String degraded : degradedAlerts)
    {
      entry.add("degraded-alert-type: " + degraded);
    }
    for (String unavailable : unavailableAlerts)
    {
      entry.add("unavailable-alert-type: " + unavailable);
    }
    ds.add(entry.toArray(new String[entry.size()]));
  }


  private Entry createServletEntry(String... enabledServlets)
  {
    Entry entry = new Entry("cn=Http Servlet Configuration,cn=monitor");
    entry.addAttribute("objectClass", "top");
    entry.addAttribute("objectClass", "ds-monitor-entry");
    entry.addAttribute("objectClass", "ds-http-servlet-config-monitor-entry");
    entry.addAttribute("objectClass", "extensibleObject");
    entry.addAttribute("cn", "Http Servlet Configuration");
    for (String enabledServlet : enabledServlets)
    {
      entry.addAttribute("enabled-servlet-and-path", enabledServlet);
    }
    return entry;
  }


  private Entry createMonitorEntry(String name,
                                   String availabilityAttribute,
                                   String availabilityValue)
  {
    Entry entry = new Entry("cn=" + name + ",cn=monitor");
    entry.addAttribute("objectClass", "top");
    entry.addAttribute("objectClass", "ds-monitor-entry");
    entry.addAttribute("objectClass", "ds-test-monitor-entry");
    entry.addAttribute(availabilityAttribute, availabilityValue);
    entry.addAttribute("single-valued", "value");
    entry.addAttribute("multi-valued", "value1", "value2");
    return entry;
  }


  private Entry createLoadBalancingAlgorithmEntry(
          String name, String status, int numAvailableServers,
          int numDegradedServers, int numUnavailableServers)
  {
    Entry entry = new Entry(String.format(
            "cn=load-balancing algorithm %s,cn=monitor", name));
    entry.addAttribute("objectClass", "top");
    entry.addAttribute("objectClass", "ds-monitor-entry");
    entry.addAttribute("objectClass",
                          "ds-load-balancing-algorithm-monitor-entry");
    entry.addAttribute("objectClass", "extensibleObject");
    entry.addAttribute("cn", String.format("load-balancing algorithm %s", name));
    entry.addAttribute("algorithm-name", name);
    entry.addAttribute("config-entry-dn", String.format(
            "cn=%s,cn=Load-Balancing Algorithms,cn=config", name));
    entry.addAttribute("health-check-state", status);
    entry.addAttribute("local-servers-health-check-state", status);
    entry.addAttribute("non-local-servers-health-check-state", status);
    entry.addAttribute("ldap-external-server",
                       String.format("example.com:636:%s", status));
    entry.addAttribute("num-available-servers",
                       String.valueOf(numAvailableServers));
    entry.addAttribute("num-degraded-servers",
                       String.valueOf(numDegradedServers));
    entry.addAttribute("num-unavailable-servers",
                       String.valueOf(numUnavailableServers));
    return entry;
  }


  private Entry createStoreAdapterEntry(String name, String status)
  {
    Entry entry =
            new Entry(String.format("cn=Store Adapter %s,cn=monitor", name));
    entry.addAttribute("objectClass", "top");
    entry.addAttribute("objectClass", "ds-monitor-entry");
    entry.addAttribute("objectClass", "ds-store-adapter-monitor-entry");
    entry.addAttribute("objectClass", "extensibleObject");
    entry.addAttribute("cn", String.format("Store Adapter %s", name));
    entry.addAttribute("store-adapter-name", name);
    entry.addAttribute("store-adapter-status", status);
    return entry;
  }


  private static File getSystemResourceAsFile(String resourceName)
  {
    try
    {
      return new File(ClassLoader.getSystemResource(resourceName).toURI());
    }
    catch (URISyntaxException e)
    {
      throw new RuntimeException(
              String.format("couldn't build URI for system resource '%s': %s",
                            resourceName, e.getMessage()), e);
    }
  }
}
