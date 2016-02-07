/*
 * Copyright 2016 UnboundID Corp.
 *
 * All Rights Reserved.
 */
package com.unboundid.ops.broker;

import com.unboundid.directory.sdk.http.api.HTTPServletExtension;
import com.unboundid.directory.sdk.http.config.HTTPServletExtensionConfig;
import com.unboundid.directory.sdk.http.types.HTTPServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;

import javax.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.List;

/**
 * An {@link HTTPServletExtension} for the {@link BrokerStatusServlet}, which
 * reports status for an UnboundID Data Broker server, and can be used as a
 * health check endpoint for HTTP load balancers.
 *
 * @author Jacob Childress <jacob.childress@unboundid.com>
 */
public class BrokerStatusServletExtension extends HTTPServletExtension
{
  private static final String ARG_PATH = "path";
  private static final String ARG_MONITORED_SERVLET = "monitored-servlet";

  private String path;


  /** {@inheritDoc} */
  @Override
  public String getExtensionName()
  {
    return "Data Broker Status Servlet";
  }


  /** {@inheritDoc} */
  @Override
  public String[] getExtensionDescription()
  {
    return new String[] {
            "This extension provides a status servlet for an UnboundID Data " +
                    "Broker. It reports status for a store adapters, LDAP store " +
                    "adapters, and a configurable set of HTTP servlets. The " +
                    "status servlet (default path '/status') can be used as a " +
                    "health check target for HTTP load balancers. The servlet " +
                    "will return a 200 OK if the Broker's services are " +
                    "available and a 503 SERVICE UNAVAILABLE if they are not."
    };
  }


  /** {@inheritDoc} */
  @Override
  public void defineConfigArguments(final ArgumentParser parser)
          throws ArgumentException
  {
    parser.addArgument(new StringArgument(
            null, ARG_PATH, false, 1, "{path}",
            "The base path of this servlet. The default is '/status'.",
            "/status"));
    parser.addArgument(new StringArgument(
            null, ARG_MONITORED_SERVLET, false, 0, "{servletName}",
            "The name of a servlet that is expected to be enabled. " +
                    "By default, no servlets are monitored."));
  }


  /** {@inheritDoc} */
  @Override
  public HttpServlet createServlet(
          HTTPServerContext httpServerContext,
          HTTPServletExtensionConfig httpServletExtensionConfig,
          ArgumentParser argumentParser) throws LDAPException
  {
    StringArgument pathArgument =
            (StringArgument) argumentParser.getNamedArgument(ARG_PATH);
    path = pathArgument.getValue();
    StringArgument monitoredServlets =
            (StringArgument) argumentParser.getNamedArgument(ARG_MONITORED_SERVLET);
    return new BrokerStatusServlet(httpServerContext,
                                   httpServerContext.getInternalRootConnection(),
                                   monitoredServlets.getValues().toArray(
                                           new String[monitoredServlets.getValues().size()]));
  }


  /** {@inheritDoc} */
  @Override
  public List<String> getServletPaths()
  {
    return Collections.singletonList(path);
  }
}
