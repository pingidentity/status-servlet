/*
 * Copyright 2016 UnboundID Corp.
 *
 * All Rights Reserved.
 */
package com.unboundid.ops.broker.models;

/**
 * The status of an HTTP servlet.
 *
 * @author Jacob Childress <jacob.childress@unboundid.com>
 */
public class ServletStatus
{
  private final String name;
  private final boolean enabled;


  /**
   * Constructs an HTTP servlet status instance.
   *
   * @param name
   *          The HTTP servlet name.
   * @param enabled
   *          Whether or not the HTTP servlet is enabled.
   */
  public ServletStatus(String name, boolean enabled)
  {
    this.name = name;
    this.enabled = enabled;
  }


  /**
   * Gets the HTTP servlet name.
   *
   * @return The HTTP servlet name.
   */
  public String getName()
  {
    return name;
  }


  /**
   * Whether or not the HTTP servlet is enabled.
   *
   * @return The enabled state of the HTTP servlet.
   */
  public boolean isEnabled()
  {
    return enabled;
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServletStatus that = (ServletStatus) o;

    return enabled == that.enabled && name.equals(that.name);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    int result = name.hashCode();
    result = 31 * result + (enabled ? 1 : 0);
    return result;
  }
}
