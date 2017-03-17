#PlexMockServices - Mock Services for Proxying into REST SERVICES and offering record/play/canned results.

This is a mock service for recording requests and responses to your REST API (or any web sever) and then playback.

##Requirements:
- Java 1.6+
- Gradle

##Version
- 0.1.x

##License
- MIT

##Building
- Checkout code from 
* git clone git@github.com:bhatti/PlexMockServices.git
- Building
* gradle compile

##Configuring:
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

##Testing
### Record Mode
```bash
  curl -X POST http://localhost:8181/myservice?mockMode=record -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: record" http://localhost:8181/myservice
```
Note: You can specify mockMode as a request parameter or a header parameter.

### Record Mode
```bash
  curl -X POST http://localhost:8181/myservice?mockMode=play -H 'Content-Type: application/json' -d {'json':true}
  curl -H 'Content-Type: application/json' -H "XMockMode: play" http://localhost:8181/myservice
```

### Specifying the id for request
   By default all requests are stored with a file name that is derived from all URL path and SHA1 of parameters/body. However, you can specify the key by passing parameter mockId.


Thank you for downloading PlexMockServices. Please send questions or suggestions to bhatti AT plexobject.com.

This software is released under MIT General Public License. 

