# status-servlet [![Build Status](https://travis-ci.org/pingidentity/status-servlet.svg?branch=master)](https://travis-ci.org/pingidentity/status-servlet)

This is a status servlet extension for PingData server products like the Directory
Server and Data Governance. It may be used as the health check target for
a layer 7 HTTP load balancer such as HAProxy or Amazon Elastic Load Balancer. 
Using this status servlet can generally be considered a more reliable indicator
of server availability than an arbitrary service path or '/', because it will
only return a success response if the server's services are in fact online and
available.

## Usage

Request the servlet at the configured path. 
For example, `https://server/status`. If the server's services are available, a
200 OK will be returned. If the server is available but degraded, for example if
disk space if low, a 429 TOO MANY REQUESTS will be returned. Otherwise, a 503
SERVICE UNAVAILABLE will be returned.

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
    "server": "available",
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

Following is an example of the server being degraded due to having
a critical alarm condition:

```http
GET /status HTTP/1.1
Accept: application/json
Content-Type: application/json; charset=utf-8
Host: example.com



HTTP/1.1 429 TOO MANY REQUESTS
Content-Type: application/json
Date: Sun, 05 Jun 2016 03:59:14 GMT
Transfer-Encoding: chunked

{
    "server": "degraded",
    "alertType": [
        "alarm-critical"
    ]
}
```

Following is an example of the server being unavailable due to having
entered lockdown mode:

```http
GET /status HTTP/1.1
Accept: application/json
Content-Type: application/json; charset=utf-8
Host: example.com



HTTP/1.1 503 SERVICE UNAVAILABLE
Content-Type: application/json
Date: Sun, 05 Jun 2016 03:59:14 GMT
Transfer-Encoding: chunked

{
    "server": "unavailable",
    "alertType": [
        "entering-lockdown-mode"
    ]
}
```

Following is an example of status based on an entry from the `cn=monitor` backend:

```http
GET /status HTTP/1.1
Accept: application/json
Content-Type: application/json; charset=utf-8
Host: example.com



HTTP/1.1 200 OK
Content-Type: application/json
Date: Fri, 18 May 2018 06:25:59 GMT
Transfer-Encoding: chunked

{
    "monitors": [
        {
            "available": true,
            "name": "Consent Service Monitor",
            "properties": {
                "cn": "Consent Service Monitor",
                "is-available": "true",
                "num-failed-creates": "0",
                "num-failed-deletes": "0",
                "num-failed-reads": "1",
                "num-failed-updates": "0",
                "num-successful-creates": "1",
                "num-successful-deletes": "1",
                "num-successful-reads": "6",
                "num-successful-updates": "1",
                "objectClass": [
                    "top",
                    "ds-monitor-entry",
                    "ds-consent-service-monitor-entry",
                    "extensibleObject"
                ],
                "total-failed": "1",
                "total-successful": "9"
            }
        }
    ],
    "server": "available",
    "servlets": [
        {
            "enabled": true,
            "name": "Consent"
        }
    ]
}
```

## Installation

Java 7 or up is required.

First, build the extension bundle by running `mvn package`. The extension bundle 
will be saved as a zip file in the `target` directory.

Next, install the extension bundle.

```
manage-extension --install com.unboundid.ops.status-servlet-VERSION.zip
```

Then configure the extension, assign it to an HTTPS connection handler and 
restart the connection handler.

Example configuration for Directory Server:

```
dsconfig create-http-servlet-extension --extension-name Status \
  --type third-party --set "description:Reports DS service availability" \
  --set extension-class:com.unboundid.ops.StatusServletExtension
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:false --add http-servlet-extension:Status
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:true
```

Example configuration for Directory Server with Consent Service:

```
dsconfig create-http-servlet-extension --extension-name Status \
  --type third-party --set "description:Reports DS service availability" \
  --set extension-class:com.unboundid.ops.StatusServletExtension \
  --set extension-argument:path=/status \
  --set extension-argument:monitored-servlet=Consent \
  --set "extension-argument:monitor=Consent Service Monitor:is-available:true"
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:false --add http-servlet-extension:Status
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:true
```

Example configuration for Data Governance:

```
dsconfig create-http-servlet-extension --extension-name Status \
  --type third-party --set "description:Reports Data Governance service availability" \
  --set extension-class:com.unboundid.ops.StatusServletExtension \
  --set extension-argument:path=/status \
  --set extension-argument:monitored-servlet=SCIM2
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:false --add http-servlet-extension:Status
dsconfig set-connection-handler-prop --handler-name "HTTPS Connection Handler" \
  --set enabled:true
```

Please see the following section for a description of the extension arguments.

## Reference

### Extension arguments

The `create-http-servlet-extension` command above takes the following 
`extension-argument` values:

| Extension argument | Required | Description |
| --- | --- | --- |
| path | no | The servlet's path. Defaults to `/status`. |
| monitored-servlet | no | The name of a servlet to monitor. This corresponds to an entry in the server's _HTTP Servlet Extensions_ configuration. If the servlet name ends with the word 'Servlet', this should be omitted. For example, 'SCIM2 Servlet' is specified as 'SCIM2'. This argument may be specified multiple times. If not specified, then no servlets are monitored. |
| monitor | no | Criteria for determining availability using an entry in the `cn=monitor` backend. The format of this argument value is `<Monitor entry name>:<Availability attribute>:<Availability value list>`, where the availability attribute is the name of an attribute of the monitor entry that should be checked to determine availablility, and the availability value list is a comma-separated list of one or more values that positively indicate availability. Availability values are treated case-insensitively. For example, `Consent Service Monitor:is-available:true` means that the Consent Service Monitor entry in `cn=monitor` will be checked and marked as available if its `is-available` attribute has a value of `true`. |

Please be aware that every entity monitored via the `monitored-servlet` or 
`monitor` argument will be considered when determining overall server 
availability. For example, if a servlet referenced by the `monitored-servlet` 
argument does not exist or is disabled, then the status servlet will respond 
with a 503 status code.

### Response codes

| Status code | Description |
| --- | --- |
| 200 OK | The server is operating normally, and all entities monitored by the status servlet are in an available state. |
| 429 TOO MANY REQUESTS | The server is operating in a degraded state. |
| 503 SERVICE UNAVAILABLE | Unavailable. One or more entities monitored by the status servlet is in an unavailable state. |

### Response fields

Note that the fields present in the response will vary depending on the product 
type, the server state, and the status servlet configuration.

| Field | Description |
| --- | --- |
| server | The server's operational status. Values are `unknown`, `available`, `degraded`, or `unavailable`. |
| alertType | An array of any current alerts. |
| servlets | An array consisting of any monitored servlets. |
| monitors | An array consisting of any monitored entries from `cn=monitor`. |
| loadBalancingAlgorithms | An array of load balancing algorithms. |
| storeAdapters | An array of store adapters. Data Governance only. |

## Support and reporting bugs

This is an unsupported example, but support will be provided on a best-effort basis.

Please report issues using the project's [issue tracker](https://github.com/pingidentity/status-servlet/issues). 
See the [contribution guidelines](CONTRIBUTING.md) for more information.

## License

This is licensed under the Apache License 2.0.
