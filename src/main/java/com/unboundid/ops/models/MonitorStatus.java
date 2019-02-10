/*
 * Copyright 2018-2019 Ping Identity Corporation
 * All Rights Reserved.
 */
package com.unboundid.ops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.unboundid.ldap.sdk.Attribute;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The status of a cn=monitor entry.
 */
public class MonitorStatus
{
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private String name;
  private boolean available = false;
  private ObjectNode monitorProperties;


  /**
   * Constructs a monitor status instance.
   *
   * @param name
   *          The monitor name.
   */
  public MonitorStatus(String name)
  {
    this.name = name;
  }


  /**
   * Gets the monitor name.
   *
   * @return The monitor name.
   */
  public String getName()
  {
    return name;
  }


  /**
   * Indicates whether or not the monitor is considered available.
   *
   * @return True if the monitor is available, or false if it is not.
   */
  public boolean isAvailable()
  {
    return available;
  }


  /**
   * Sets the monitor availability status.
   *
   * @param available
   *          The availability status.
   * @return This instance.
   */
  public MonitorStatus setAvailable(boolean available)
  {
    this.available = available;
    return this;
  }


  /**
   * Gets the monitor properties.
   *
   * @return The monitor properties.
   */
  @JsonProperty("properties")
  public ObjectNode getMonitorProperties()
  {
    return monitorProperties;
  }


  /**
   * Sets the monitor properties using the list of attributes from a monitor
   * entry. Any known operational attributes will be omitted.
   *
   * @param properties
   *          The monitor entry's attributes.
   * @return This instance.
   */
  public MonitorStatus setMonitorProperties(Collection<Attribute> properties)
  {
    ObjectNode propertiesNode = MAPPER.createObjectNode();
    for (Attribute property : properties)
    {
      if (!isOperationalAttribute(property.getName()))
      {
        if (property.getValues().length > 1)
        {
          ArrayNode valuesNode = MAPPER.createArrayNode();
          for (String value : property.getValues())
          {
            valuesNode.add(value);
          }
          propertiesNode.replace(property.getName(), valuesNode);
        }
        else
        {
          propertiesNode.put(property.getName(), property.getValue());
        }
      }
    }
    this.monitorProperties = propertiesNode;
    return this;
  }


  /**
   * Indicates whether or not the attribute is an operational attribute.
   * This class doesn't actually have access to the server schema, so it cheats
   * and checks a hard-coded list of common operational attributes.
   *
   * @param attributeName
   *          The name of the attribute to check.
   * @return True if the attribute is an operational attribute, or false if
   * it is not.
   */
  private boolean isOperationalAttribute(String attributeName)
  {
    Set<String> operationalAttributes = new HashSet<>(Arrays.asList(
        "entrydn",
        "entryuuid",
        "subschemasubentry",
        "creatorsname",
        "createtimestamp",
        "modifiersname",
        "modifytimestamp",
        "ds-entry-checksum"
    ));
    return operationalAttributes.contains(attributeName.toLowerCase());
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonitorStatus that = (MonitorStatus) o;
    return available == that.available &&
        Objects.equals(name, that.name);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return Objects.hash(name, available);
  }
}
