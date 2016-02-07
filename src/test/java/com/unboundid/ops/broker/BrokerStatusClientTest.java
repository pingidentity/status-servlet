/*
 * Copyright 2016 UnboundID Corp.
 *
 * All Rights Reserved.
 */
package com.unboundid.ops.broker;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.ops.broker.models.*;
import com.unboundid.ops.broker.models.Error;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link BrokerStatusClient}.
 *
 * @author jacobc
 */
public class BrokerStatusClientTest
{
  private static final String BASE_DN = "dc=example,dc=com";

  private InMemoryDirectoryServer ds;


  /**
   * Starts an in-memory DS.
   *
   * @throws Exception
   */
  @BeforeClass
  public void setup() throws Exception
  {
    InMemoryDirectoryServerConfig config =
            new InMemoryDirectoryServerConfig(BASE_DN);
    config.setBaseDNs("cn=monitor");
    config.setEnforceSingleStructuralObjectClass(false);
    config.setEnforceAttributeSyntaxCompliance(false);
    config.setSchema(Schema.mergeSchemas(
            Schema.getDefaultStandardSchema(),
            Schema.getSchema(getSystemResourceAsFile("monitor-schema.ldif"))));
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
  public void brokerOkTest() throws Exception
  {
    ds.clear();
    addBaseEntry();

    Entry servletEntry = new Entry("cn=Http Servlet Configuration,cn=monitor");
    servletEntry.addAttribute("objectClass", "top");
    servletEntry.addAttribute("objectClass", "ds-monitor-entry");
    servletEntry.addAttribute("objectClass", "ds-http-servlet-config-monitor-entry");
    servletEntry.addAttribute("objectClass", "extensibleObject");
    servletEntry.addAttribute("cn", "Http Servlet Configuration");
    servletEntry.addAttribute("enabled-servlet-and-path",
                              "Monitored Servlet https://example.com/monitoredServlet");
    servletEntry.addAttribute("enabled-servlet-and-path",
                              "Unmonitored Servlet https://example.com/unmonitoredServlet");

    Entry lbaEntry =
            new Entry("cn=load-balancing algorithm User Store LBA,cn=monitor");
    lbaEntry.addAttribute("objectClass", "top");
    lbaEntry.addAttribute("objectClass", "ds-monitor-entry");
    lbaEntry.addAttribute("objectClass",
                          "ds-load-balancing-algorithm-monitor-entry");
    lbaEntry.addAttribute("objectClass", "extensibleObject");
    lbaEntry.addAttribute("cn", "load-balancing algorithm User Store LBA");
    lbaEntry.addAttribute("algorithm-name", "User Store LBA");
    lbaEntry.addAttribute("config-entry-dn",
                          "cn=User Store LBA,cn=Load-Balancing Algorithms,cn=config");
    lbaEntry.addAttribute("health-check-state", "AVAILABLE");
    lbaEntry.addAttribute("local-servers-health-check-state", "AVAILABLE");
    lbaEntry.addAttribute("non-local-servers-health-check-state", "AVAILABLE");
    lbaEntry.addAttribute("ldap-external-server", "example.com:636:AVAILABLE");
    lbaEntry.addAttribute("num-available-servers", "1");
    lbaEntry.addAttribute("num-degraded-servers", "0");
    lbaEntry.addAttribute("num-unavailable-servers", "0");

    Entry storeAdapterEntry =
            new Entry("cn=Store Adapter UserStoreAdapter,cn=monitor");
    storeAdapterEntry.addAttribute("objectClass", "top");
    storeAdapterEntry.addAttribute("objectClass", "ds-monitor-entry");
    storeAdapterEntry.addAttribute("objectClass", "ds-store-adapter-monitor-entry");
    storeAdapterEntry.addAttribute("objectClass", "extensibleObject");
    storeAdapterEntry.addAttribute("cn", "Store Adapter UserStoreAdapter");
    storeAdapterEntry.addAttribute("store-adapter-name", "UserStoreAdapter");
    storeAdapterEntry.addAttribute("store-adapter-status", "AVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      BrokerStatusClient client =
              new BrokerStatusClient(connection, "Monitored Servlet");
      BrokerStatus status = client.getStatus();
      assertTrue(status.isOK());

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
    addBaseEntry();

    Entry servletEntry = new Entry("cn=Http Servlet Configuration,cn=monitor");
    servletEntry.setAttribute("objectClass", "top");
    servletEntry.setAttribute("objectClass", "ds-monitor-entry");
    servletEntry.setAttribute("objectClass", "ds-http-servlet-config-monitor-entry");
    servletEntry.setAttribute("objectClass", "extensibleObject");
    servletEntry.setAttribute("cn", "Http Servlet Configuration");
    servletEntry.setAttribute("enabled-servlet-and-path",
                              "Unmonitored Servlet https://example.com/unmonitoredServlet");

    Entry lbaEntry =
            new Entry("cn=load-balancing algorithm User Store LBA,cn=monitor");
    lbaEntry.setAttribute("objectClass", "top");
    lbaEntry.setAttribute("objectClass", "ds-monitor-entry");
    lbaEntry.setAttribute("objectClass",
                          "ds-load-balancing-algorithm-monitor-entry");
    lbaEntry.setAttribute("objectClass", "extensibleObject");
    lbaEntry.setAttribute("cn", "load-balancing algorithm User Store LBA");
    lbaEntry.setAttribute("algorithm-name", "User Store LBA");
    lbaEntry.setAttribute("config-entry-dn",
                          "cn=User Store LBA,cn=Load-Balancing Algorithms,cn=config");
    lbaEntry.setAttribute("local-servers-health-check-state", "UNAVAILABLE");
    lbaEntry.setAttribute("non-local-servers-health-check-state", "UNAVAILABLE");
    lbaEntry.setAttribute("ldap-external-server", "example.com:636:UNAVAILABLE");
    lbaEntry.setAttribute("num-available-servers", "0");
    lbaEntry.setAttribute("num-degraded-servers", "0");
    lbaEntry.setAttribute("num-unavailable-servers", "1");

    Entry storeAdapterEntry =
            new Entry("cn=Store Adapter UserStoreAdapter,cn=monitor");
    storeAdapterEntry.setAttribute("objectClass", "top");
    storeAdapterEntry.setAttribute("objectClass", "ds-monitor-entry");
    storeAdapterEntry.setAttribute("objectClass", "ds-store-adapter-monitor-entry");
    storeAdapterEntry.setAttribute("objectClass", "extensibleObject");
    storeAdapterEntry.setAttribute("cn", "Store Adapter UserStoreAdapter");
    storeAdapterEntry.setAttribute("store-adapter-name", "UserStoreAdapter");
    storeAdapterEntry.setAttribute("store-adapter-status", "UNAVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      BrokerStatusClient client =
              new BrokerStatusClient(connection, "Monitored Servlet");
      BrokerStatus status = client.getStatus();
      assertFalse(status.isOK());

      Error error = status.getError();
      assertNotNull(error);
      assertNotNull(error.getMessage());
      assertTrue(error.getMessage().contains(
              "Expected one and only one HTTP servlet config monitor entry"));
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
    addBaseEntry();

    Entry servletEntry = new Entry("cn=Http Servlet Configuration,cn=monitor");
    servletEntry.addAttribute("objectClass", "top");
    servletEntry.addAttribute("objectClass", "ds-monitor-entry");
    servletEntry.addAttribute("objectClass", "ds-http-servlet-config-monitor-entry");
    servletEntry.addAttribute("objectClass", "extensibleObject");
    servletEntry.addAttribute("cn", "Http Servlet Configuration");
    servletEntry.addAttribute("enabled-servlet-and-path",
                              "Monitored Servlet https://example.com/monitoredServlet");
    servletEntry.addAttribute("enabled-servlet-and-path",
                              "Unmonitored Servlet https://example.com/unmonitoredServlet");

    Entry lbaEntry =
            new Entry("cn=load-balancing algorithm User Store LBA,cn=monitor");
    lbaEntry.addAttribute("objectClass", "top");
    lbaEntry.addAttribute("objectClass", "ds-monitor-entry");
    lbaEntry.addAttribute("objectClass",
                          "ds-load-balancing-algorithm-monitor-entry");
    lbaEntry.addAttribute("objectClass", "extensibleObject");
    lbaEntry.addAttribute("cn", "load-balancing algorithm User Store LBA");
    lbaEntry.addAttribute("algorithm-name", "User Store LBA");
    lbaEntry.addAttribute("config-entry-dn",
                          "cn=User Store LBA,cn=Load-Balancing Algorithms,cn=config");
    lbaEntry.addAttribute("health-check-state", "UNAVAILABLE");
    lbaEntry.addAttribute("local-servers-health-check-state", "UNAVAILABLE");
    lbaEntry.addAttribute("non-local-servers-health-check-state", "UNAVAILABLE");
    lbaEntry.addAttribute("ldap-external-server", "example.com:636:UNAVAILABLE");
    lbaEntry.addAttribute("num-available-servers", "1");
    lbaEntry.addAttribute("num-degraded-servers", "0");
    lbaEntry.addAttribute("num-unavailable-servers", "0");

    Entry storeAdapterEntry =
            new Entry("cn=Store Adapter UserStoreAdapter,cn=monitor");
    storeAdapterEntry.addAttribute("objectClass", "top");
    storeAdapterEntry.addAttribute("objectClass", "ds-monitor-entry");
    storeAdapterEntry.addAttribute("objectClass", "ds-store-adapter-monitor-entry");
    storeAdapterEntry.addAttribute("objectClass", "extensibleObject");
    storeAdapterEntry.addAttribute("cn", "Store Adapter UserStoreAdapter");
    storeAdapterEntry.addAttribute("store-adapter-name", "UserStoreAdapter");
    storeAdapterEntry.addAttribute("store-adapter-status", "AVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      BrokerStatusClient client =
              new BrokerStatusClient(connection, "Monitored Servlet");
      BrokerStatus status = client.getStatus();
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
    addBaseEntry();

    Entry servletEntry = new Entry("cn=Http Servlet Configuration,cn=monitor");
    servletEntry.addAttribute("objectClass", "top");
    servletEntry.addAttribute("objectClass", "ds-monitor-entry");
    servletEntry.addAttribute("objectClass", "ds-http-servlet-config-monitor-entry");
    servletEntry.addAttribute("objectClass", "extensibleObject");
    servletEntry.addAttribute("cn", "Http Servlet Configuration");
    servletEntry.addAttribute("enabled-servlet-and-path",
                              "Monitored Servlet https://example.com/monitoredServlet");
    servletEntry.addAttribute("enabled-servlet-and-path",
                              "Unmonitored Servlet https://example.com/unmonitoredServlet");

    Entry lbaEntry =
            new Entry("cn=load-balancing algorithm User Store LBA,cn=monitor");
    lbaEntry.addAttribute("objectClass", "top");
    lbaEntry.addAttribute("objectClass", "ds-monitor-entry");
    lbaEntry.addAttribute("objectClass",
                          "ds-load-balancing-algorithm-monitor-entry");
    lbaEntry.addAttribute("objectClass", "extensibleObject");
    lbaEntry.addAttribute("cn", "load-balancing algorithm User Store LBA");
    lbaEntry.addAttribute("algorithm-name", "User Store LBA");
    lbaEntry.addAttribute("config-entry-dn",
                          "cn=User Store LBA,cn=Load-Balancing Algorithms,cn=config");
    lbaEntry.addAttribute("health-check-state", "AVAILABLE");
    lbaEntry.addAttribute("local-servers-health-check-state", "AVAILABLE");
    lbaEntry.addAttribute("non-local-servers-health-check-state", "AVAILABLE");
    lbaEntry.addAttribute("ldap-external-server", "example.com:636:AVAILABLE");
    lbaEntry.addAttribute("num-available-servers", "1");
    lbaEntry.addAttribute("num-degraded-servers", "0");
    lbaEntry.addAttribute("num-unavailable-servers", "0");

    Entry storeAdapterEntry =
            new Entry("cn=Store Adapter UserStoreAdapter,cn=monitor");
    storeAdapterEntry.addAttribute("objectClass", "top");
    storeAdapterEntry.addAttribute("objectClass", "ds-monitor-entry");
    storeAdapterEntry.addAttribute("objectClass", "ds-store-adapter-monitor-entry");
    storeAdapterEntry.addAttribute("objectClass", "extensibleObject");
    storeAdapterEntry.addAttribute("cn", "Store Adapter UserStoreAdapter");
    storeAdapterEntry.addAttribute("store-adapter-name", "UserStoreAdapter");
    storeAdapterEntry.addAttribute("store-adapter-status", "UNAVAILABLE");

    ds.add(servletEntry);
    ds.add(lbaEntry);
    ds.add(storeAdapterEntry);

    LDAPConnection connection = ds.getConnection();
    try
    {
      BrokerStatusClient client =
              new BrokerStatusClient(connection, "Monitored Servlet");
      BrokerStatus status = client.getStatus();
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



  private void addBaseEntry() throws Exception
  {
    ds.add("dn: cn=monitor",
           "objectClass: top",
           "objectClass: ds-monitor-entry",
           "objectClass: ds-general-monitor-entry",
           "objectClass: extensibleObject",
           "cn: monitor");
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
