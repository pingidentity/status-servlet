# status-servlet [![Build Status](https://travis-ci.org/pingidentity/status-servlet.svg?branch=master)](https://travis-ci.org/pingidentity/status-servlet)

This is a status servlet extension for the Data Governance Broker. It may be
used as the health check target for a layer 7 HTTP load balancer such as HAProxy
or Amazon Elastic Load Balancer and can generally be considered a more reliable
indicator of server availability than an arbitrary service path.

## Usage

Request the servlet at the configured path. 
For example, `https://server/status`. If the Data Governance Broker's services
are available, a 200 OK will be returned. Otherwise, a 503 SERVICE UNAVAILABLE
will be returned.

```http
GET /status HTTP/1.1
Accept: application/json
Content-Type: application/json; charset=utf-8
Host: example.com



HTTP/1.1 200 OK
Content-Type: application/json
Date: Sun, 05 Jun 2016 03:59:14 GMT
Transfer-Encoding: chunked

{
    "loadBalancingAlgorithms": [
        {
            "available": true, 
            "name": "User Store LBA", 
            "numAvailableServers": 2, 
            "numDegradedServers": 0, 
            "numUnavailableServers": 0
        }
    ], 
    "servlets": [
        {
            "enabled": true, 
            "name": "SCIM2"
        }, 
        {
            "enabled": true, 
            "name": "UserInfo"
        }, 
        {
            "enabled": true, 
            "name": "JWK"
        }, 
        {
            "enabled": true, 
            "name": "Authentication"
        }, 
        {
            "enabled": true, 
            "name": "OAuth2"
        }
    ], 
    "storeAdapters": [
        {
            "available": true, 
            "name": "UserStoreAdapter"
        }
    ]
}
```

## Installation

Java 7 or up is required.

First, build the extension bundle by running `mvn package`. The extension bundle will be saved as a zip file in the `target` directory.

Next, install the extension bundle.

```
manage-extension --install com.unboundid.ops.status-servlet-VERSION.zip
```

Then configure the extension, assign it to an HTTPS connection handler and 
restart the connection handler.

```
dsconfig create-http-servlet-extension --extension-name Status \
  --type third-party --set "description:Reports Data Governance Broker service availability" \
  --set extension-class:com.unboundid.ops.StatusServletExtension \
  --set extension-argument:path=/status \
  --set extension-argument:monitored-servlet=OAuth2 \
  --set extension-argument:monitored-servlet=SCIM2 \
  --set extension-argument:monitored-servlet=UserInfo \
  --set extension-argument:monitored-servlet=JWK \
  --set extension-argument:monitored-servlet=Authentication
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:false --add http-servlet-extension:Status
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:true
```

The `create-http-servlet-extension` command above takes the following `extension-argument` values:

| Extension argument | Description |
| --- | --- |
| path | The servlet's path. Defaults to `/status`. |
| monitored-servlet | The name of a servlet to monitor. This corresponds to an entry in the Broker's _HTTP Servlet Extensions_ configuration. If the servlet name ends with the word 'Servlet', this should be omitted. For example, 'OAuth2 Servlet' is specified as 'OAuth2'. This argument may be specified multiple times. If not specified, then no servlets are monitored. |

## Support and reporting bugs

This is an unsupported example, but support will be provided on a best-effort basis.

Please report issues using the project's [issue tracker](https://github.com/pingidentity/status-servlet/issues).

## License

This is licensed under the Apache License 2.0.
