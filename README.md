# broker-status-servlet

This is a status servlet extension for the UnboundID Data Broker. It may be
used as the health check target for an HTTP load balancer.

## Usage

Request the servlet at the configured path. 
For example, *https://server/status*. If the Broker's services are available,
a 200 OK will be returned. Otherwise, a 503 SERVICE UNAVAILABLE will be 
returned.

## Installation

First, install the extension bundle.

```
manage-extension --update com.unboundid.broker-status-servlet-1.0-SNAPSHOT.zip
```

Then configure the extension, assign it to an HTTPS connection handler and 
restart the connection handler.

```
dsconfig create-http-servlet-extension --extension-name Status \
  --type third-party --set "description:Reports Broker service availability" \
  --set extension-class:com.unboundid.ops.broker.BrokerStatusServletExtension \
  --add extension-argument:path=/status \
  --add extension-argument:monitored-servlet=OAuth \
  --add extension-argument:monitored-servlet=SCIM2 \
  --add extension-argument:monitored-servlet=UserInfo
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:false --add http-servlet-extension:Status
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:true
```

## License

This is licensed under the Apache License 2.0.
