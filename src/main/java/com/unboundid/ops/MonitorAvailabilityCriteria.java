/*
 * Copyright 2018 Ping Identity Corporation
 * All Rights Reserved.
 */
package com.unboundid.ops;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import static com.unboundid.ops.StatusServletExtension.MONITOR_ARG_RX;

/**
 * Represents the criteria used to determine whether or not a cn=monitor entry
 * represents an available state.
 */
public class MonitorAvailabilityCriteria
{
  private String monitorEntryName;
  private DN monitorEntryDN;
  private String availabilityAttribute;
  private Set<String> availabilityValues;


  /**
   * Creates a monitor availability criteria instance.
   *
   * @param arg
   *          A monitor extension argument value.
   * @return A new monitor availability criteria instance.
   * @throws LDAPException if the monitor entry DN cannot be constructed.
   */
  public static MonitorAvailabilityCriteria create(String arg)
      throws LDAPException
  {
    Matcher matcher = MONITOR_ARG_RX.matcher(arg);
    if (matcher.matches())
    {
      MonitorAvailabilityCriteria criteria = new MonitorAvailabilityCriteria();

      criteria.monitorEntryName = matcher.group(1);

      criteria.monitorEntryDN =
          new DN(String.format("cn=%s,cn=monitor",
              criteria.monitorEntryName));
      criteria.availabilityAttribute = matcher.group(2);

      Set<String> availabilityValues = new HashSet<>();
      for (String availabilityValue : matcher.group(3).split(","))
      {
        availabilityValues.add(availabilityValue.toLowerCase());
      }
      criteria.availabilityValues = availabilityValues;

      return criteria;
    }
    // This shouldn't occur, because the argument value will have been
    // validated already by the ArgumentParser.
    throw new IllegalArgumentException(String.format(
        "Argument does not match regex '%s", MONITOR_ARG_RX));
  }


  /**
   * Gets the monitor entry name.
   *
   * @return The monitor entry name.
   */
  public String getMonitorEntryName()
  {
    return monitorEntryName;
  }


  /**
   * Gets the monitor entry DN.
   *
   * @return The monitor entry DN.
   */
  public DN getMonitorEntryDN()
  {
    return monitorEntryDN;
  }


  /**
   * Gets the monitor availability attribute, which is the monitor entry
   * attribute that is used to determine if the monitor represents an available
   * state or not.
   *
   * @return The monitor availability attribute.
   */
  public String getAvailabilityAttribute()
  {
    return availabilityAttribute;
  }


  /**
   * Gets the set of monitor availability values. If the value of the
   * availability attribute is one of these values, then the monitor is
   * considered available.
   *
   * @return The set of monitor availability values.
   */
  public Set<String> getAvailabilityValues()
  {
    return availabilityValues;
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonitorAvailabilityCriteria that = (MonitorAvailabilityCriteria) o;
    return Objects.equals(monitorEntryName, that.monitorEntryName) &&
        Objects
            .equals(availabilityAttribute, that.availabilityAttribute) &&
        Objects.equals(availabilityValues, that.availabilityValues);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return Objects
        .hash(monitorEntryName, availabilityAttribute, availabilityValues);
  }
}
