<p align="center">
  <img src="https://www.convertigo.com/wp-content/themes/EightDegree/images/logo_convertigo.png">
  <h2 align="center"> C8oSDK Android</h2>
</p>
<p align="center">
  <a href="/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://travis-ci.org/convertigo/c8osdk-android"><img
  src="https://travis-ci.org/convertigo/c8osdk-android.svg?branch=master" alt="Travis Status"></a>
</a> 
</p>

## TOC ##

- [TOC](#toc)
- [Introduction](#introduction)
  - [About SDKs](#about-sdks)
  - [About Convertigo Platform](#about-convertigo-platform)
- [Requirements](#requirements)
- [Installation](#installation)
- [Documentation](#documentation)
  - [Initializing a Convertigo Endpoint](#initializing-a-convertigo-endpoint)
    - [2 - Advanced instance settings](#2---advanced-instance-settings)
      - [2.1 - The common way](#21---the-common-way)
      - [2.2 - The verbose way](#22---the-verbose-way)
      - [2.3 - Customize settings](#23---customize-settings)
        - [2.3.1 - From existing settings](#231---from-existing-settings)
        - [2.3.2 - From a C8o instance](#232---from-a-c8o-instance)
      - [2.4 - Retrieve some settings](#24---retrieve-some-settings)
    - [3 - Calling a Convertigo Requestable](#3---calling-a-convertigo-requestable)
      - [3.1 - Returning JSON](#31---returning-json)
      - [3.2 - Returning XML](#32---returning-xml)
    - [4 - Call parameters](#4---call-parameters)
      - [4.1 - The common way with parameters](#41---the-common-way-with-parameters)
      - [4.2 - the verbose way](#42---the-verbose-way)
    - [5 - Working with threads](#5---working-with-threads)
      - [5.1 - Locking the current thread](#51---locking-the-current-thread)
      - [5.2 - Freeing the current thread](#52---freeing-the-current-thread)
      - [5.2.1 - Then](#521---then)
      - [5.2.1 - ThenUI](#521---thenui)
    - [6 - Chaining calls](#6---chaining-calls)
    - [7 - Handling failures](#7---handling-failures)
      - [7.1 - Try / catch handling](#71---try--catch-handling)
      - [7.2 - Then / ThenUI handling](#72---then--thenui-handling)
- [More information](#more-information)
- [About Convertigo](#about-convertigo)


## Introduction ##

### About SDKs ###

This is the Convertigo library for native Java Android

Convertigo Client SDK is a set of libraries used by mobile or Windows desktop applications to access Convertigo Server services. An application using the SDK can easily access Convertigo services such as Sequences and Transactions.

The Client SDK will abstract the programmer from handling the communication protocols, local cache, FullSync off line data management, UI thread management and remote logging. So the developer can focus on building the application.

Client SDK is available for:
* [Android Native](https://github.com/convertigo/c8osdk-android) apps as a standard Gradle dependency
* [iOS native](https://github.com/convertigo/c8osdk-ios) apps as a standard Cocoapod
* [React Native](https://github.com/convertigo/react-native-c8osdk) as a NPM package
* [Google Angular framework](https://github.com/convertigo/c8osdk-angular) as typescript an NPM package
* [Vue.js](https://github.com/convertigo/c8osdk-js), [ReactJS](https://github.com/convertigo/c8osdk-js), [AngularJS](https://github.com/convertigo/c8osdk-js) Framework, or any [Javascript](https://github.com/convertigo/c8osdk-js) project as a standard Javascript NPM package
* [Windows desktop](https://github.com/convertigo/c8osdk-dotnet) or [Xamarin apps](https://github.com/convertigo/c8osdk-dotnet) as Nugets or Xamarin Components


This current package is the Native Android SDK. For others SDKs see official [Convertigo Documentation.](https://www.convertigo.com/document/all/cmp-7/7-5-1/reference-manual/convertigo-mbaas-server/convertigo-client-sdk/programming-guide/)

### About Convertigo Platform ###

Convertigo Mobility Platform supports native Android developers. Services brought by the platform are available for Android clients applications thanks to the Convertigo MBaaS SDK. SDK provides an Android framework you can use to access Convertigo Server’s services such as:

- Connectors to back-end data (SQL, NoSQL, REST/SOAP, SAP, - WEB HTML, AS/400, Mainframes)
- Server Side Business Logic (Protocol transform, Business logic augmentation, ...)
- Automatic offline replicated databases with FullSync technology
- Security and access control (Identity managers, LDAP , SAML, oAuth)
- Server side Cache
- Push notifications (APND, GCM)
- Auditing Analytics and logs (SQL, and Google Analytics)

[Convertigo Technology Overview](http://download.convertigo.com/webrepository/Marketing/ConvertigoTechnologyOverview.pdf)

[Access Convertigo mBaaS technical documentation](http://www.convertigo.com/document/latest/)

[Access Convertigo SDK Documentations](https://www.convertigo.com/document/all/cmp-7/7-5-1/reference-manual/convertigo-mbaas-server/convertigo-client-sdk/)

## Requirements ##

* Android studio

## Installation ##

The Convertigo Client SDK for Android is provided on jCenter:

To use it in your project, you just have to reference the SDK in the “dependencies” closure of your module build.gradle file. Be sure to update the sdk reference to the correct version.

Sample gradle file : 

```gradle
apply plugin: 'com.android.application'

android {
compileSdkVersion 24
        buildToolsVersion "24.0.0"

        packagingOptions {
            exclude 'META-INF/LICENSE'
            exclude 'META-INF/NOTICE'
        }
    
        defaultConfig {
            applicationId "com.example.opic.myc8osdkapp"
            minSdkVersion 10
            targetSdkVersion 24
            versionCode 1
            versionName "1.0"
        }
        buildTypes {
            release {
                minifyEnabled false
                proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            }
    }
}

dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    testCompile 'junit:junit:4.12'
    compile 'com.android.support:appcompat-v7:24.1.1'

    /*
    * Include Convertigo Client SDK version 2.1.2
    */
    compile 'com.convertigo.sdk:c8oSDKAndroid:2.1.2'
}
```
    
By default Convertigo SDK Brings several Database Storage engines :
SQLite for standard SQLite storage
SQLiteCipher for Encrypted SQLite storage
ForestDB for High performance Encrypted or not Storage
You can reduce the size of your applications by excluding the unwanted storage engines.

```gradle
compile ('com.convertigo.sdk:c8oSDKAndroid:2.1.2') {
    exclude module: 'couchbase-lite-android-sqlcipher' // Exclude if you dont need the optional SQLCipher Storage Engine
    exclude module: 'couchbase-lite-android-forestdb'  // Exclude if you dont need the optional ForestDB Storage Engine
}
```
As a reminder, don’t forget to grant INTERNET permission to your app in the AndroidManifest.xml file.

```xml
‹uses-permission android:name="android.permission.INTERNET" /›
```

## Documentation ##

### Initializing a Convertigo Endpoint ###

 A C8o instance is linked to a server through is endpoint and cannot be changed after.

You can have as many C8o instances, pointing to a same or different endpoint. Each instance handles its own session and settings. We strongly recommend using a single C8o instance per application because server licensing can based on the number of sessions used.

```java
import C8o

import com.convertigo.clientsdk.*;
  …
  C8o c8o = new C8o(getApplicationContext(), "https://demo.convertigo.net/cems/projects/sampleMobileCtfGallery");
  // the C8o instance is ready to interact over https with the demo.convertigo.net server, using sampleMobileUsDirectoryDemo as default project.
```	

#### 2 - Advanced instance settings ####

The endpoint is the mandatory setting to get a C8o instance, but there is additional settings through the C8oSettings class.  
A C8oSettings instance should be passed after the endpoint. Settings are copied inside the C8o instance and a C8oSettings instance can be modified and reused after the C8o constructor.  
Setters of C8oSettings always return its own instance and can be chained.  
A C8oSettings can be instantiated from an existing C8oSettings or C8o instance.

##### 2.1 - The common way #####

    C8o c8o = new C8o(getApplicationContext(), "https://demo.convertigo.net/cems/projects/sampleMobileCtfGallery", new C8oSettings()
                        .setDefaultDatabaseName("mydb_fullsync")
                        .setTimeout(30000));
##### 2.2 - The verbose way #####

    String endpoing = "https://demo.convertigo.net/cems/projects/sampleMobileCtfGallery";
    C8oSettings c8oSettings = new C8oSettings();
    c8oSettings.setDefaultDatabaseName("mydb_fullsync");
    c8oSettings.setTimeout(30000);
    c8o = new C8o(getApplicationContext(), endpoint, c8oSettings);
                   
##### 2.3 - Customize settings #####
###### 2.3.1 - From existing settings ######
    C8oSettings customSettings = new C8oSettings(c8oSettings).setTimeout(60000);
    
###### 2.3.2 - From a C8o instance ######
    customSettings = new C8oSettings(c8o).setTimeout(60000);

##### 2.4 - Retrieve some settings #####

    int timeout = c8o.getTimeout();

#### 3 - Calling a Convertigo Requestable ####

With a C8o instance you can call Convertigo Sequence and Transaction or make query to your local FullSync database. You must specify the result type you want: an XML Document or a JSON Object response.
  
##### 3.1 - Returning JSON #####
Just use the `c8o.callJson` method to request a JSON response.

	import org.json.JSONObject;
    …
    // c8o is a C8o instance
    JSONObject jObject = c8o.callJson(".getSimpleData").sync();

##### 3.2 - Returning XML #####
Just use the c8o.callXml method to request a XML response.

	import org.w3c.dom.Document;
    …
    // c8o is a C8o instance
    Document document = c8o.callXml(".getSimpleData").sync();

#### 4 - Call parameters ####

The call method expects the requester string of the following syntax:

- For a transaction: [project].connector.transaction  
- For a sequence: [project].sequence


The project name is optional, i.e. if not specified, the project specified in the endpoint will be used.  
Convertigo requestables generally need key/value parameters. The key is always a string and the value can be any object but a string is the standard case.  
Here a sample with JSON but this would be the same for XML calls:

##### 4.1 - The common way with parameters #####

	JSONObject jObject = c8o.callJson(".getSimpleData",
      "firstname", "John",
      "lastname", "Doe"
    ).sync();

	
##### 4.2 - the verbose way #####

	Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("firstname", "John");
    parameters.put("lastname", "Doe");
    JSONObject jObject = c8o.callJson(".getSimpleData", parameters).sync();
	
#### 5 - Working with threads ####

##### 5.1 - Locking the current thread #####

Maybe you noticed that the calls methods doesn’t return the result directly and that all the sample code chains to the `.sync()` method.  
This is because the call methods return a `C8oPromise` instance. That allows the developer to choose if he wants to block the current thread, make an async request or get the response in a callback.  
The `.sync()` method locks the current thread and return the result as soon as it’s avalaible. Of course this should not be used in a UI thread as this will result to a frozen UI untill data is returned by the server. You should use the `.sync()` method only in worker threads.  

	// lock the current thread while the request is done
    JSONObject jObject = c8o.callJson(".getSimpleData").sync();
    // the response can be used in this scope
    
##### 5.2 - Freeing the current thread #####

As in many cases, locking the current thread is not recommended, the `.then()` method allows to register a callback that will be executed on a worker thread.  
The `.thenUI()` method does the same but the callback will be executed on a UI thread. This is useful for quick UI widgets updates.  
The `.then()` and `.thenUI()` callbacks receives as parameters the response and the request parameters.

##### 5.2.1 - Then #####
  	// doesn't lock the current thread while the request is done
    c8o.callJson(".getSimpleData").then(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        // the jObject is available, the current code is executed in an another working thread
        …
        return null; // return null for a simple call
      }
    });
    // following lines are executed immediately, before the end of the request.
	
##### 5.2.1 - ThenUI #####
	c8o.callJson(".getSimpleData").thenUI(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        // the jObject is available, the current code is executed in the UI thread
        output.setText(jObject.toString());
        …
        return null; // return null for a simple call
      }
    });
    // following lines are executed immediately, before the end of the request.
	
#### 6 - Chaining calls ####

The `.then()` or `.thenUI()` returns a C8oPromise that can be use to chain other promise methods, such as `.then()` or `.thenUI()` or failure handlers.  
 The last `.then()` or `.thenUI()` must return a nil value. `.then()` or `.thenUI()` can be mixed but the returning type must be the same: XML or JSON.
 
	c8o.callJson(".getSimpleData", "callNumber", 1).then(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        // you can do stuff here and return the next C8oPromise<JSONObject> instead of deep nested blocks
        return c8o.callJson(".getSimpleData", "callNumber", 2);
      }
    }).thenUI(new C8oOnResponse<JSONObject>() { // use .then or .thenUI is allowed
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        // you can do stuff here and even modify previous parameters
        parameters.put("callNumber", 3);
        parameters.put("extraParameter", "ok");
        return c8o.callJson(".getSimpleData", parameters);
      }
    }).then(new C8oOnResponse<JSONObject>() { // use .then or .thenUI is allowed
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        // you can do stuff here and return null because this is the end of the chain
        return null;
      }
    });

#### 7 - Handling failures ####

A call can throw an error for many reasons: technical failure, network error and so on.  
The standard do/catch should be used to handle this.  
This is the case for the `.sync()` method: if an exception occurs during the request execution, the original exception is thrown by the method and can be encapsulated in a `C8oException`.

##### 7.1 - Try / catch handling #####
	try {
      c8o.callJson(".getSimpleData").sync();
    } catch (Exception exception) {
      // process the exception
    }


##### 7.2 - Then / ThenUI handling #####
When you use the `.then()` or the `.thenUI()` methods, the do/catch mechanism can’t catch a “future” exception or throwable: you have to use the `.fail()` or `.failUI()` methods at the end on the promise chain.  
One fail handler per promise chain is allowed. The fail callback provide the object thrown (like an Exception) and the parameters of the failed request.


	c8o.callJson(".getSimpleData", "callNumber", 1).then(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        return c8o.callJson(".getSimpleData", "callNumber", 2);
      }
    }).thenUI(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        return null;
      }
    }).fail(new C8oOnFail() {
      @Override
      public void run(Throwable throwable, Map<String, Object> parameters) {
        // throwable catched from the first or the second callJson, can be an Exception
        // this code runs in a worker thread
        …
      }
    });
    
    
    c8o.callJson(".getSimpleData", "callNumber", 1).then(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        return c8o.callJson(".getSimpleData", "callNumber", 2);
      }
    }).thenUI(new C8oOnResponse<JSONObject>() {
      @Override
      public C8oPromise<JSONObject> run(JSONObject jObject, Map<String, Object> parameters) throws Throwable {
        return null;
      }
    }).failUI(new C8oOnFail() {
      @Override
      public void run(Throwable throwable, Map<String, Object> parameters) {
        // throwable catched from the first or the second callJson, can be an Exception
        // this code runs in a UI thread
        …
      }
    });

## More information ##
<img src='http://www.convertigo.com/wp-content/uploads/2015/11/convertigoClientSDK-300x203.png' style='float:right'>

Please see about other SDKs:

- [.NET (Xamarin)](https://components.xamarin.com/view/convertigo-mbaas "Xamarin")

- [.NET](https://www.nuget.org/packages/C8oFullSyncExtensions/)

- [Angular](https://www.npmjs.com/package/c8osdkangular)

- [Swift](https://cocoapods.org/pods/C8oSDK)

## About Convertigo ##

<div style="text-align:center">  

<img src='http://image.slidesharecdn.com/convertigompjuill2015usglobal-newversion-150820100604-lva1-app6891/95/convertigo-mobility-platform-mobile-application-development-for-enterprises-madp-mbaas-4-638.jpg'>

</div>
    
