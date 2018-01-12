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
package com.unboundid.ops.models;

/**
 * The status of an HTTP servlet.
 *
 * @author Jacob Childress
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
