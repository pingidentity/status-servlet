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
package com.unboundid.ops.models;

/**
 * A status error.
 *
 * @author Jacob Childress
 */
public class StatusError
{
  private final Throwable cause;
  private final String message;


  /**
   * Constructor.
   *
   * @param throwable
   *          The {@link Throwable} that this object represents.
   */
  public StatusError(Throwable throwable)
  {
    this.cause = throwable;
    this.message = throwable.getMessage();
  }


  /**
   * Gets the error cause.
   *
   * @return The error cause.
   */
  public Throwable getCause()
  {
    return cause;
  }


  /**
   * Gets the error message.
   *
   * @return The error message.
   */
  public String getMessage()
  {
    return message;
  }


  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StatusError error = (StatusError) o;

    return message.equals(error.message);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return message.hashCode();
  }
}
