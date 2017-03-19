# PlexMockServices - Mocking REST SERVICES 

PlexMockServices offers a mock service for proxying into REST SERVICES and saves responses. You can then playback those services using recorded/canned results. You can also interject random failures and delays and thus test robustness of your client side connection.

## Features:
- Record API response in easy to manage YAML files 
- Playback YAML stored responses 
- Support for sequential or random responses when there are multiple response
  files for a given request.
- Define dynamic responses using Velocity templates so that you can return
  responses based on the request parameters or other factors.
- Inject random failures and delays 
- Specify response codes/delays in the request 

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

- Specify order of response
```xml
        <init-param>
            <param-name>randomResponseOrder</param-name> 
            <param-value>true</param-value> 
        </init-param>
```
If randomResponseOrder is true then mock server will send random response, otherwise it will send in the order they were recorded. 
Note: The order is not preserved between restarts of the server.

- Add random failures/wait times
```xml
        <init-param>
            <param-name>injectFailuresAndWaitTimesPerc</param-name> 
            <param-value>10</param-value> 
        </init-param>
```
This means that 10% of responses will either fail or will have high wait times.
You can configure minWaitTimeMillis and maxWaitTimeMillis for minimum/maximum wait that request will take.
- Add random failures/wait times
```xml
        <init-param>
            <param-name>minWaitTimeMillis</param-name> 
            <param-value>10</param-value> 
        </init-param>
        <init-param>
            <param-name>maxWaitTimeMillis</param-name> 
            <param-value>1000</param-value> 
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

### Specifying wait time for request
   You can optionally specify mockWaitTimeMillis parameter to the HTTP request for adding wait time before response is sent. Alternatively, you can use injectFailuresAndWaitTimesPerc to add random failures or delays.

### Specifying response code for request
   You can optionally specify mockResponseCode parameter for HTTP response code to return.

## Recorded Response Files
PlexMockServices supports static responses based on YAML format and dynamic response files based on 
Velcity templates. It allows you to define output based on request parameters 

## Static YAML output files
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

## Dynamic Velocity output files
Here is an example of response that is saved in YAML/Velocity format, you can
refer http://velocity.apache.org/engine/1.7/user-guide.html for the syntax
of velocity tags. 

Note: PlexMockServices will automatically register all parameters as velcoity
variables so that you can refer them in the template easily.

```vm
---
responseCode: #if($!{$mockResponseCode}) 200 #else $mockResponseCode
#end
headers: {}
contentType: "application/json; charset=utf-8"
contentClass: "java.util.Map"
contents: 
#set($start = 1)
#set($end = 10)
#set($range = [$start..$end])
#foreach($i in $range)
  $i:
    name: "$name $i"
    id: $i
#end
```

You can then call a curl request such as:
curl -H 'Content-Type: application/json' -H "XMockMode: play"  'http://localhost:8080?name=name_prefix'
and it would return
```json
{
	"1": {
		"name": "jack 1",
		"id": 1
	},
	"2": {
		"name": "jack 2",
		"id": 2
	},
	"3": {
		"name": "jack 3",
		"id": 3
	},
	"4": {
		"name": "jack 4",
		"id": 4
	},
	"5": {
		"name": "jack 5",
		"id": 5
	},
	"6": {
		"name": "jack 6",
		"id": 6
	},
	"7": {
		"name": "jack 7",
		"id": 7
	},
	"8": {
		"name": "jack 8",
		"id": 8
	},
	"9": {
		"name": "jack 9",
		"id": 9
	},
	"10": {
		"name": "jack 10",
		"id": 10
	}
}
```
You can then mimick failure by passing mockResponseCode, e.g.
```bash 
curl -v -H 'Content-Type: application/json' -H "XMockMode: play"  'http://localhost:8080?name=jack&mockResponseCode=404'
```
This would return 404 return code.
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

## Contact
Thank you for downloading PlexMockServices. Please send questions or suggestions to bhatti AT plexobject.com.

This software is released under MIT General Public License. 

