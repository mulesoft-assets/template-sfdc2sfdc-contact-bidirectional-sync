
# Mule Kick: sfdc2sfdc-bidirectional-contact-sync

+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [Running on CloudHub](#runoncloudhub)
    * [Running on premise](#runonopremise)
        * [Properties to be configured](#propertiestobeconfigured)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [endpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)


# Use Case <a name="usecase"/>

As a Salesforce admin I want to have my Contacts syncronized between two different Salesforce orgs.

This Kick/Template should serve as a foundation for setting an online bi-directional sync of Contacts between two SalesForce instances, being able to specify filtering criterias. 

The integration main behaviour is polling for changes (new Contacts or modified ones) that have occured in any of the Salesforces instances during a certain defined period of time. For those Contacts that both have not been updated yet, and meet the requirements configured in the query, the integration triggers an upsert (update or create depending the case) taking the last modification as the one that should be applied.

Requirements have been set not only to be used as examples, but also to stablish starting point to adapt the integration to any given requirements.


# Run it! <a name="runit"/>

In order to have your application up and running you just need to complete two simple steps:
1. Configure the application properties
2. Run it! (on premise or in Cloudhub)

## Properties to be configured (with examples)<a name="propertiestobeconfigured"/>

In order to use this Mule Kick you need to configure properties (Credentials, configurations, etc.) either in properties file, or in CloudHub as Environment Variables. 

Detail list with examples:

### Application configuration
+ polling.frequency `10000`  
+ watermark.default.expression `YESTERDAY`

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/26.0`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/26.0`

#### Salesforce polling query parameters
+ sfdc.query.fields `Department, Description, Email, FirstName, HomePhone, LastModifiedDate, LastName, MailingCountry, MobilePhone, Title`
+ sfdc.query.filters `AND MailingCountry IN ('U.S.','United States','US')`

### Some points to consider about configuration properties

Polling Frecuency is expressed in miliseconds (different time units can be used) and the Watermark Default Expression defines the date to be used to query the first time the integration runs. [More details about polling and watermarking.](http://www.mulesoft.org/documentation/display/current/Poll+Reference)

The date format accepted in SFDC Query Language is either YYYY-MM-DDThh:mm:ss+hh:mm or you can use Constants (Like YESTERDAY in the example). [More information about Dates in SFDC.](http://www.salesforce.com/us/developer/docs/officetoolkit/Content/sforce_api_calls_soql_select_dateformats.htm)

The query fields list must include both 'Email' and 'LastModifiedDate' fields, as those fields are embedded in the integration business logic

## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, supposing you choose as domain name `sfdc2sfdc-bidirectional-contact-sync` to trigger the use case you just need to hit `http://sfdc2sfdc-bidirectional-contact-sync.cloudhub.io/synccontacts` and report will be sent to the emails configured.

## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

After this, to trigger the use case you just need to hit the local http endpoint with the port you configured in your file. If this is, for instance, `9090` then you should hit: `http://localhost:9090/synccontacts` and this will create a CSV report and send it to the mails set.

# Customize It!<a name="customizeit"/>
This brief guide intends to give a high level idea of how this Kick is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organised by describing all the XML that conform the Kick.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [endpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
This file holds the configuration for Connectors and [Properties Place Holders](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. 
**Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** 
Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.


## endpoints.xml<a name="endpointsxml"/>
This is the file where you will found the inbound and outbound sides of your integration app.
It is intented to define the application API.
...

## businessLogic.xml<a name="businesslogicxml"/>
This file holds the functional aspect of the kick , directed by one flow responsible of conducting the business logic.
...


## errorHandling.xml<a name="errorhandlingxml"/>
This is the right place to handle how your integration will react depending on the different exceptions. 
This file holds a [Choice Exception Strategy](http://www.mulesoft.org/documentation/display/current/Choice+Exception+Strategy) that is referenced by the main flow in the business logic.
...