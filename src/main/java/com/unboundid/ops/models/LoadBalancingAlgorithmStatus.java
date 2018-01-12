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
 * LDAP load balancing algorithm status. This provides availability status for
 * external LDAP directory servers.
 *
 * @author Jacob Childress
 */
public class LoadBalancingAlgorithmStatus
{
  private final String name;
  private final boolean available;
  private final int numAvailableServers;
  private final int numDegradedServers;
  private final int numUnavailableServers;


  /**
   * Constructs an LBA status instance.
   *
   * @param name
   *          The load balancing algorithm name.
   * @param available
   *          LBA availability.
   * @param numAvailableServers
   *          The number of available LDAP servers.
   * @param numDegradedServers
   *          The number of LDAP servers marked as degraded.
   * @param numUnavailableServers
   *          The number of unavailable LDAP servers.
   */
  public LoadBalancingAlgorithmStatus(String name, boolean available,
                                      int numAvailableServers,
                                      int numDegradedServers,
                                      int numUnavailableServers)
  {
    this.name = name;
    this.available = available;
    this.numAvailableServers = numAvailableServers;
    this.numDegradedServers = numDegradedServers;
    this.numUnavailableServers = numUnavailableServers;
  }


  /**
   * Gets the LBA name.
   *
   * @return The load balancing algorithm name.
   */
  public String getName()
  {
    return name;
  }


  /**
   * Returns the LBA availability.
   *
   * @return True if the LBA is available; otherwise, false.
   */
  public boolean isAvailable()
  {
    return available;
  }


  /**
   * Gets the number of available LDAP servers.
   *
   * @return The number of available LDAP servers.
   */
  public int getNumAvailableServers()
  {
    return numAvailableServers;
  }


  /**
   * Gets the number of degraded LDAP servers.
   *
   * @return The number of degraded LDAP servers.
   */
  public int getNumDegradedServers()
  {
    return numDegradedServers;
  }


  /**
   * Gets the number of unavailable LDAP servers.
   *
   * @return The number of unavailable LDAP servers.
   */
  public int getNumUnavailableServers()
  {
    return numUnavailableServers;
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LoadBalancingAlgorithmStatus that = (LoadBalancingAlgorithmStatus) o;

    return available == that.available &&
            numAvailableServers == that.numAvailableServers &&
            numDegradedServers == that.numDegradedServers &&
            numUnavailableServers == that.numUnavailableServers &&
            name.equals(that.name);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    int result = name.hashCode();
    result = 31 * result + (available ? 1 : 0);
    result = 31 * result + numAvailableServers;
    result = 31 * result + numDegradedServers;
    result = 31 * result + numUnavailableServers;
    return result;
  }
}
