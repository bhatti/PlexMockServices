# PlexMockServices - Mocking REST SERVICES 

PlexMockServices offers a mock service for proxying into REST SERVICES and saves responses. You can then playback those services using recorded/canned results. You can also interject random failures and delays and thus test robustness of your client side connection.

## Features:
- Record API response in easy to manage YAML/JSON files 
- Playback YAML/JSON stored responses 
- Store YAML/JSON stored responses 
- Support mock data for generating random data and using files for random responses
- Support random responses when there are multiple response files for a given request.
- Define dynamic responses using Velocity or Thymeleaf templates so that you can return responses based on the request parameters or other factors.
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
```bash
git clone git@github.com:bhatti/PlexMockServices.git
```
- Building
```bash
./gradlew compile
```
## Configuring:
- Edit src/main/resources/application.properties
* Specify default mockMode
```xml
            mockMode=RECORD
```

- Specify format of export files:
```xml
            defaultExportFormat=YAML
```
Note: You can specify YAML, JSON, Velocity or Thymeleaf format.

- Add random failures/wait times
```xml
            injectFailuresAndWaitTimesPerc=10
```
This means that 10% of responses will either fail or will have high wait times.
You can configure minWaitTimeMillis and maxWaitTimeMillis for minimum/maximum wait that request will take.
- Add random failures/wait times
```xml
            minWaitTimeMillis=10
            maxWaitTimeMillis=1000
```

- Specify target service base URL
```xml
            urlPrefix=http://localhost:9000
```
Note: It's recommended that you use deploy mock service as root context so mapping of service paths is simple.

## Testing
### Start server
```bash
./gradlew war run
```
### Record Mode
```bash
  curl -X POST http://localhost:8000/myservice?mockMode=RECORD -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: RECORD" http://localhost:8000/myservice
```
Note: You can specify mockMode as a request parameter or a header parameter.

### ThymeLeaf support
```
curl -X POST "http://localhost:$port/API/mdm/devices/apps?searchBy=Udid&id=111&page=0&pageSize=10&mockMethod=GET&mockExportFormat=THYMELEAF" -H "XMockMode: store" -H 'Content-Type: application/json' -d @../fixtures/device_apps.th
```

### Velocity support
```
curl -X POST "http://localhost:$port/API/mdm/devices/search?page=0&pageSize=10&mockMethod=GET&mockExportFormat=VELOCITY" -H "XMockMode: store" -H 'Content-Type: application/json' -d @../fixtures/android_devices.vm
```

### Play Mode
```bash
  curl -X POST http://localhost:8000/myservice?mockMode=PLAY -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: PLAY " http://localhost:8000/myservice
```

### ThymeLeaf playback
```bash
curl "http://localhost:$port/API/mdm/devices/apps?searchBy=Udid&id=111&page=0&pageSize=10" -H "XMockMode: play" |jq '.'
```

### Velocity playback
```bash
curl "http://localhost:$port/API/mdm/devices/search?page=1&pageSize=10" -H "XMockMode: play" |jq '.'
```

### Store Mode for storing mock data explicitly
```bash
  curl -X POST http://localhost:8000/myservice?mockMode=STORE -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: STORE" http://localhost:8000/myservice
```

### Store text data file
curl -X POST "http://localhost:$port/lines?mockMethod=GET&mockExportFormat=TEXT" -H "XMockMode: store" -H 'Content-Type: multipart/x-www-form-urlencoded' -F 'image=@../fixtures/lines.txt'

### Specifying the id for request
   By default all requests are stored with a file name that is derived from all URL path and SHA1 of parameters/body. However, you can specify the key by passing parameter mockRequestId.

### Specifying method for request
   By default method is derived from http request but you can specify mockMethod to overwrite it.

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

## Static JSON output files
Here is an example of response that is saved in JSON format for easy editing:
```json
{
  "responseCode": 200,
  "headers": {
    "Transfer-Encoding": "chunked",
    "Server": "SampleAPI",
    "Connection": "keep-alive",
    "Date": "Wed, 26 Aug 2020 19:57:33 GMT",
    "Content-Type": "application/json; charset=utf-8"
  },
  "contentType": "application/json; charset=utf-8",
  "contents": {
    "ApplicationGroupID": "10335",
    "Name": "ios-white",
    "Platform": "Apple",
    "AppGroupType": "Whitelist",
    "Description": "",
    "ManagedByOrganizationGroupID": "7233",
    "OrganizationGroups": [
      {
        "Name": "Tokyo",
        "Id": "7233"
      }
    ]
  }
}
```

## Dynamic Velocity output files
Here is an example of response that is saved in velocity template format, you can
refer http://velocity.apache.org/engine/1.7/user-guide.html for the syntax
of velocity tags. 

Note: PlexMockServices will automatically register all parameters as velcoity
variables so that you can refer them in the template easily.

```vm
{
#if ( !$mockResponseCode )
#set ( $mockResponseCode = 200 )
#end
  "responseCode": #if($!{$mockResponseCode}) 200 #else $mockResponseCode,
#end
  "headers": {
    "content-type": "application/json; charset=utf-8"
  },
  "contentType": "application/json; charset=utf-8",
  "contents": {
    "Devices": [
#set($start = 1)
#set($end = 100)
#set($range = [$start..$end])
#foreach($i in $range)
      {
	  "EasIds": {},
	  "Udid": "$helper.uuid()",
      "LocationGroupId": {
        "Id": {
          "Value": $helper.number()
        },
        "Name": "$helper.city(5)",
	    "Udid": "$helper.uuid()"
      },
      "LocationGroupName": "$helper.city(5)",
      "Model": "google $helper.androidModel()",
	  "Udid": "$helper.uuid()"
      },
#end
    ],
    "Page": $page,
    "PageSize": $pageSize,
    "Total": $pageSize
  }
}
```

You can then upload velocity and call a curl request such as:
```
curl -X POST "http://localhost:$port/API/mdm/devices/search?page=0&pageSize=10&mockMethod=GET&mockExportFormat=VELOCITY" -H "XMockMode: store" -H 'Content-Type: application/json' -d @../fixtures/android_devices.vm
curl "http://localhost:$port/API/mdm/devices/search?page=0&pageSize=10" -H "XMockMode: play" |jq '.'
```

You can then mimick failure by passing mockResponseCode, e.g.
```bash 
curl -v -H 'Content-Type: application/json' -H "XMockMode: PLAY"  'http://localhost:8080?name=jack&mockResponseCode=404'
```
This would return 404 return code.

## Dynamic Thymeleaf output files
Here is an example of response that is saved in thymeleaf template format, you can
refer http://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html for the syntax
of Thymeleaf tags. 

Note: PlexMockServices will automatically register all parameters as Thymeleaf
variables so that you can refer them in the template easily.

```th 
{
  "responseCode": [(${mockResponseCode}? ${mockResponseCode} : 200)],
  "headers": {
    "content-type": "application/json; charset=utf-8"
  },
  "contentType": "application/json; charset=utf-8",
  "contents": {
    "Devices": [
[# th:each="i : ${#numbers.sequence(1,pageSize)}"]
      {
	  "EasIds": {},
      "LocationGroupName": "[(${helper.city(5)})]",
      "UserId": {
        "Id": {
          "Value": [(${helper.number()})]
        },
        "Name": "[(${helper.name()})] [(${i})]",
	    "Udid": "[(${helper.uuid()})]"
      },
	  "UserName": "[(${helper.string()})]",
      "DataProtectionStatus": [(${helper.number()})],
	  "UserEmailAddress": "[(${helper.email()})]",
      },
[/]
    ],
    "Page": [(${page})],
    "PageSize": [(${pageSize})],
    "Total": [(${pageSize})]
  }
}

```

You can run uplaod template file and use curl command such as: 
```bash
curl -X POST "http://localhost:$port/API/mdm/devices/search?page=1&pageSize=10&mockMethod=GET&mockExportFormat=THYMELEAF" -H "XMockMode: store" -H 'Content-Type: application/json' -d @../fixtures/ios_devices.th
curl "http://localhost:$port/API/mdm/devices/search?page=1&pageSize=10" -H "XMockMode: play" |jq '.'
```

## Sample App
After starting server by:
```bash 
./gradlew war appRun
```

You can find a sample REST app based on node.js restify under sample folder, which you can start by running 
```bash 
cd sample
./server.sh
```
You can then look at fixtures folder for sample templates and client.sh for sample curl commands.

## Contact
Thank you for downloading PlexMockServices. Please send questions or suggestions to bhatti AT plexobject.com.

This software is released under MIT General Public License. 

