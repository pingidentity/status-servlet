/*
 * Copyright 2017 UnboundID Corp.
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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Overall health of the server.
 */
public class ServerStatus
{

  /** Server status is unknown. */
  static ServerStatus UNKNOWN = new ServerStatus(Status.UNKNOWN);


  /**
   * Possible status.
   */
  private enum Status
  {
    /** Server status is unknown. */
    UNKNOWN,

    /** Server status is unavailable. */
    UNAVAILABLE,

    /** Server status is degraded. */
    DEGRADED,

    /** Server status is available. */
    AVAILABLE
  }



  private final Status status;

  private final String[] reasons;



  /**
   * Return a list of alert type names that are causing the server's
   * degraded or unavailable status, for example 'low-disk-space-error'.
   *
   * @return reasons the server is degraded or unavailable; or an empty
   *         array if the server is available.
   */
  String[] getAlertTypes()
  {
    return reasons;
  }



  /**
   * Status string; one of 'unknown', 'available', 'degraded', or
   * 'unavailable'.
   *
   * @return status string.
   */
  public String getStatus()
  {
    return status.name().toLowerCase();
  }


  /**
   * Indicates whether this status is 'available'.
   *
   * @return boolean where true indicates 'available'.
   */
  public boolean isAvailable()
  {
    return Status.AVAILABLE.equals(status);
  }


  /**
   * Indicates whether this status is 'degraded'.
   *
   * @return boolean where true indicates 'degraded'.
   */
  public boolean isDegraded()
  {
    return Status.DEGRADED.equals(status);
  }



  /**
   * Constructs an instance.
   *
   * @param unavailableAlerts
   *            Array of unavailable alert names.
   * @param degradedAlerts
   *            Array of degraded alert names.
   */
  public ServerStatus(String[] unavailableAlerts,
                      String[] degradedAlerts)
  {
    List<String> alertList = new ArrayList<>();
    if (unavailableAlerts != null)
    {
      alertList.addAll(Arrays.asList(unavailableAlerts));
    }

    if (degradedAlerts != null)
    {
      alertList.addAll(Arrays.asList(degradedAlerts));
    }

    this.reasons = alertList.toArray(new String[alertList.size()]);

    if (unavailableAlerts != null && unavailableAlerts.length > 0)
    {
      this.status = Status.UNAVAILABLE;
    }
    else if (degradedAlerts != null && degradedAlerts.length > 0)
    {
      this.status = Status.DEGRADED;
    }
    else
    {
      this.status = Status.AVAILABLE;
    }
  }


  /**
   * Constructs an instance.
   *
   * @param status server status.
   */
  private ServerStatus(Status status)
  {
    this.status  = status;
    this.reasons = new String[0];
  }
}
