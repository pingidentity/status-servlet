/*
 * Copyright 2016 UnboundID Corp.
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
package com.unboundid.ops.broker.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Reports availability information for an UnboundID Data Broker.
 *
 * @author Jacob Childress <jacob.childress@unboundid.com>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokerStatus
{
  private List<ServletStatus> servletStatuses = new ArrayList<>();
  private List<StoreAdapterStatus> storeAdapterStatuses = new ArrayList<>();
  private List<LoadBalancingAlgorithmStatus> lbaStatuses = new ArrayList<>();
  private StatusError error;


  private BrokerStatus()
  {
    // Private constructor. No implementation.
  }


  /**
   * Creates a status instance for a Broker that is available.
   *
   * @param servletStatuses
   *          HTTP servlet statuses.
   * @param storeAdapterStatuses
   *          Store adapter statuses.
   * @param lbaStatuses
   *          LDAP load balancing algorithm statuses.
   * @return A status instance.
   */
  public static BrokerStatus create(List<ServletStatus> servletStatuses,
                                    List<StoreAdapterStatus> storeAdapterStatuses,
                                    List<LoadBalancingAlgorithmStatus> lbaStatuses)
  {
    BrokerStatus status = new BrokerStatus();
    status.servletStatuses = servletStatuses;
    status.storeAdapterStatuses = storeAdapterStatuses;
    status.lbaStatuses = lbaStatuses;
    return status;
  }


  /**
   * Creates a status instance for a Broker that is unavailable.
   *
   * @param error
   *          An error status.
   * @return A status instance.
   */
  public static BrokerStatus create(StatusError error)
  {
    BrokerStatus status = new BrokerStatus();
    status.error = error;
    return status;
  }


  /**
   * Gets the status for any monitored HTTP servlets.
   *
   * @return HTTP servlet status.
   */
  @JsonProperty("servlets")
  public List<ServletStatus> getServletStatuses()
  {
    return servletStatuses;
  }


  /**
   * Gets the status for store adapters.
   *
   * @return Store adapters status.
   */
  @JsonProperty("storeAdapters")
  public List<StoreAdapterStatus> getStoreAdapterStatuses()
  {
    return storeAdapterStatuses;
  }


  /**
   * Gets the status for LDAP load balancing algorithms.
   *
   * @return Load balancing algorithm status.
   */
  @JsonProperty("loadBalancingAlgorithms")
  public List<LoadBalancingAlgorithmStatus> getLoadBalancingAlgorithmStatuses()
  {
    return lbaStatuses;
  }


  /**
   * Gets an error status.
   *
   * @return The error status.
   */
  @JsonProperty("error")
  public StatusError getError()
  {
    return error;
  }


  /**
   * Returns whether or not this status instance represents an available Broker
   * or an unavailable Broker.
   *
   * @return True if the Broker is available; otherwise, false.
   */
  @JsonIgnore
  public boolean isOK()
  {
    boolean ok = true;
    for (ServletStatus servletStatus : servletStatuses)
    {
      if (!servletStatus.isEnabled())
      {
        ok = false;
      }
    }
    for (StoreAdapterStatus storeAdapterStatus : storeAdapterStatuses)
    {
      if (!storeAdapterStatus.isAvailable())
      {
        ok = false;
      }
    }
    for (LoadBalancingAlgorithmStatus lbaStatus : lbaStatuses)
    {
      if (!lbaStatus.isAvailable())
      {
        ok = false;
      }
    }
    if (error != null)
    {
      ok = false;
    }
    return ok;
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BrokerStatus that = (BrokerStatus) o;

    return !(servletStatuses != null ?
            !servletStatuses.equals(that.servletStatuses) :
            that.servletStatuses != null) &&
            !(storeAdapterStatuses != null ?
                    !storeAdapterStatuses.equals(that.storeAdapterStatuses) :
                    that.storeAdapterStatuses != null) &&
            !(lbaStatuses != null ? !lbaStatuses.equals(that.lbaStatuses) :
                    that.lbaStatuses != null) && !(error != null ?
            !error.equals(that.error) : that.error != null);

  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    int result = servletStatuses != null ? servletStatuses.hashCode() : 0;
    result = 31 * result + (storeAdapterStatuses != null ?
            storeAdapterStatuses.hashCode() : 0);
    result = 31 * result + (lbaStatuses != null ? lbaStatuses.hashCode() : 0);
    result = 31 * result + (error != null ? error.hashCode() : 0);
    return result;
  }
}
