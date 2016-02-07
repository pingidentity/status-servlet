/*
 * Copyright 2016 UnboundID Corp.
 *
 * All Rights Reserved.
 */
package com.unboundid.ops.broker.models;

/**
 * A Broker status error.
 *
 * @author Jacob Childress <jacob.childress@unboundid.com>
 */
public class Error
{
  private final Throwable cause;
  private final String message;


  /**
   * Constructor.
   *
   * @param throwable
   *          The {@link Throwable} that this object represents.
   */
  public Error(Throwable throwable)
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

    Error error = (Error) o;

    return message.equals(error.message);
  }


  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return message.hashCode();
  }
}
