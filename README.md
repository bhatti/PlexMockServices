# PlexMockServices - Mock Services for REST SERVICES 

PlexMockServices offers a mock service for proxying into REST SERVICES and offering record/play/canned results.

## Requirements:
- Java 1.6+
- Gradle

## Version
- 0.1.x

## License
- MIT

## Building
- Checkout code from 
* git clone git@github.com:bhatti/PlexMockServices.git
- Building
* gradle compile

## Configuring:
- Edit src/main/webapp/WEB-INF/web.xml 
* Specify default recordMode
```xml
        <init-param>
            <param-name>recordMode</param-name> 
            <param-value>true</param-value> 
        </init-param>
```

- Specify target service base URL
```xml
        <init-param>
            <param-name>urlPrefix</param-name> 
            <param-value>http://localhost:8181</param-value> 
        </init-param>
```
Note: It's recommended that you use deploy mock service as root context so mapping of service paths is simple.

## Testing
### Record Mode
```bash
  curl -X POST http://localhost:8181/myservice?mockMode=record -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: record" http://localhost:8181/myservice
```
Note: You can specify mockMode as a request parameter or a header parameter.

### Play Mode
```bash
  curl -X POST http://localhost:8181/myservice?mockMode=play -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: play" http://localhost:8181/myservice
```

### Specifying the id for request
   By default all requests are stored with a file name that is derived from all URL path and SHA1 of parameters/body. However, you can specify the key by passing parameter requestId.

## Sample App
After starting server by:
```bash 
./server.sh
```

You can find a sample REST app based on node.js restify under sample folder, which you can start by running 
```bash 
cd sample
./server.sh
```
You can then look at client.sh for sample curl commands.

## Sample YAML output files
Here is an example of response that is saved in YAML format for easy editing:
```yaml
---
responseCode: 200
headers: {}
contentType: "application/json"
contentClass: "java.util.Map"
contents:
  findByAccountsResponse:
  - date: 1452739584192
    orderId: 1
    account:
      accountId: 1
      accountName: "CX2001"
    security:
      securityId: 1
      symbol: "AAPL"
      name: "Apple"
      securityType: "STOCK"
    exchange: "NYSE"
    orderLegs:
    - side: "BUY"
      price: 169.80
      quantity: 1.82
      fillPrice: 169.80
      fillQuantity: 1.82
    - side: "BUY"
      price: 124.19
      quantity: 3.712
      fillPrice: 124.19
      fillQuantity: 3.71
    - side: "BUY"
      price: 189.60
      quantity: 2.30
      fillPrice: 189.60
      fillQuantity: 2.30
    - side: "BUY"
      price: 114.52
      quantity: 1.81
      fillPrice: 114.52
      fillQuantity: 1.81
    - side: "BUY"
      price: 122.59
      quantity: 5.65
      fillPrice: 122.59536943620424
      fillQuantity: 5.65
    - side: "BUY"
      price: 119.93
      quantity: 7.78
      fillPrice: 119.93
      fillQuantity: 7.78
    - side: "BUY"
      price: 133.77
      quantity: 7.43
      fillPrice: 133.77
      fillQuantity: 7.43
    status: "FILLED"
    marketSession: "OPEN"
    fillDate: 1489812951913
  - date: 1452739584192
    orderId: 2
    account:
      accountId: 1
      accountName: "CX2001"
    security:
      securityId: 1
      symbol: "AAPL"
      name: "Apple"
      securityType: "STOCK"
    exchange: "NYSE"
    orderLegs:
    - side: "BUY"
      price: 169.80
      quantity: 1.82
      fillPrice: 169.80
      fillQuantity: 1.82
    - side: "BUY"
      price: 124.19
      quantity: 3.71
      fillPrice: 124.19
      fillQuantity: 3.71
    status: "FILLED"
    marketSession: "OPEN"
    fillDate: 1489812975262
```

## Contact
Thank you for downloading PlexMockServices. Please send questions or suggestions to bhatti AT plexobject.com.

This software is released under MIT General Public License. 

