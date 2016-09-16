# status-servlet

This is a status servlet extension for the Data Governance Server. It may be
used as the health check target for an HTTP load balancer such as HAProxy or
Amazon Elastic Load Balancer.

## Usage

Request the servlet at the configured path. 
For example, `https://server/status`. If the Data Governance Server's services
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

First, install the extension bundle.

```
manage-extension --install com.unboundid.status-servlet-1.0-SNAPSHOT.zip
```

Then configure the extension, assign it to an HTTPS connection handler and 
restart the connection handler.

```
dsconfig create-http-servlet-extension --extension-name Status \
  --type third-party --set "description:Reports Data Governance Server service availability" \
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

## Support and reporting bugs

This is an unsupported example, but support will be provided on a best-effort basis.

Please report issues using the project's [issue tracker](https://github.com/UnboundID/status-servlet/issues).

## License

This is licensed under the Apache License 2.0.
