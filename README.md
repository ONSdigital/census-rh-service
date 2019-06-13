[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c11c38daa91f48818dca0a1e3a6837ea)](https://www.codacy.com/app/philwhiles/census-rh-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ONSdigital/census-rh-service&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/ONSdigital/census-rh-service.svg?branch=master)](https://travis-ci.org/ONSdigital/census-rh-service)
[![codecov](https://codecov.io/gh/ONSdigital/census-rh-service/branch/master/graph/badge.svg)](https://codecov.io/gh/ONSdigital/census-rh-service)

# Respondent Data Service
This repository contains the Respondent Data service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/). It manages respondent data, where a Respondent Data object represents an expected response from the Respondent Data service, which provides all the data that is required by Respondent Home in order for it to verify the respondent's UAC code and connect them to the relevant EQ questionnaire.

## Set Up

Do the following steps to set up the code to run locally:
* Install Java 11 locally
* Install Docker: Sign in to docker hub and then install docker by downloading it from https://hub.docker.com/editions/community/docker-ce-desktop-mac
* Install maven
* Clone the following git repositories:
* https://github.com/ONSdigital/census-int-common-config
* https://github.com/ONSdigital/census-int-common-service
* https://github.com/ONSdigital/census-int-common-test-framework
* https://github.com/ONSdigital/census-rh-service
* Make sure that you have a suitable settings.xml file in your local .m2 directory
* Run a mvn clean install for each of the cloned repos in turn. This will install each of them into your local maven repository.

NB. For more detailed information about any of the above steps please see the following confluence article:
https://collaborate2.ons.gov.uk/confluence/display/SDC/How+to+Set+Up+a+Mac+for+Census+Integration+Development+Work

Do the following steps to set up the Application Default Credential (see the next section to understand what this is used for):
* Install the Google SDK locally
* Create a Google Cloud Platform project
* Open the Google Cloud Platform console, which can be found at the following location: https://console.cloud.google.com/getting-started?pli=1
* In the left hand panel choose 'APIs & Services' > 'Credentials'
* Use the following instructions to set up a service account and create an environment variable: https://cloud.google.com/docs/authentication/getting-started
NB. The instructions will lead to a .json file being downloaded, which can be used for setting up credentials. You should move this to a suitable location for pointing your code at.
* To set up the GOOGLE_APPLICATION_CREDENTIALS environment variable you will need to point to the .json file using a command similar to this:
* export GOOGLE_APPLICATION_CREDENTIALS="/users/ellacook/Documents/census-int-code/<filename>.json"
* Once that is done then you can use the following command to tell your applications to use those credentials as your application default credentials:
* gcloud auth application-default login
*NB. Running the above command will first prompt you to hit 'Y' to continue and will then open up the Google Login page, where you need to select your login account and then click 'Allow'.
* It should then open Google Cloud with the following message displayed: You are now authenticated with the Google Cloud SDK!

NB. For more detailed information about setting up the Application Default Credential please see the following confluence article:
https://collaborate2.ons.gov.uk/confluence/display/SDC/How+to+Set+Up+Google+Cloud+Platform+Locally

Finally, create an environment variable to hold the name of your Google Cloud Platform project, using the export command at the terminal (in your census-rh-service repo):
export GOOGLE_CLOUD_PROJECT="<name of your project>" e.g. export GOOGLE_CLOUD_PROJECT="census-rh-ellacook1"

## Running

There are two ways of running this service

* The first way is from the command line after moving into the same directory as the pom.xml:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
* The second way requires that you first create a JAR file using the following mvn command (after moving into the same directory as the pom.xml):
    ```bash
    mvn clean package
    ```
This will create the JAR file in the Target directory. You can then right-click on the JAR file (in Intellij) and choose 'Run'.

In order to run the Rabbitmq service, so that you can publish CaseCreated, CaseUpdated, and UACUpdated, messages for this census-rh-service to receive and store in Google CLoud Platform, enter the following command (from the same directory as the docker-compose.yml):
    ```bash
    docker-compose up -d
    ```
Messages that are published to the events exchange will be routed to either the Case.Gateway or UAC.Gateway queue (depending on their binding).
They will then be received by census-rh-service and stored in either the case_bucket or the uac_bucket (as appropriate) of the relevant Google Cloud Platform project.
The project to use is given by the Application Default Credentials (These are the credential associated with the service account that your app engine app runs as - to set these up please follow the steps given in the previous section).


## End Points

When running successfully the words "Hello Census Integration!" should be found at the following endpoint:
    
* localhost:8171/respondent/data

NB. You need to enter user as ‘Admin’ and pw as ‘secret’ to access the URL.


The Rabbitmq management console should be found at the following endpoint:

http://localhost:46672/#/queues

CaseCreated and CaseUpdated events can be manually published to the events exchange, using the binding key event.case.lifecycle. They should have the following format:

{
  "event" : {
    "type" : "CASE_UPDATED",
    "source" : "CASE_SERVICE",
    "channel" : "RM",
    "dateTime" : "2011-08-12T20:17:46.384Z",
    "transactionId" : "c45de4dc-3c3b-11e9-b210-d663bd873d93"
  },
  "payload" : {
    "collectionCase" : {
        "id":"bbd55984-0dbf-4499-bfa7-0aa4228700e9",
        "caseRef":"10000000010",
        "survey":"CENSUS",
        "collectionExerciseId":"n66de4dc-3c3b-11e9-b210-d663bd873d93",
        "address": {
		"addressLine1":"1 main street",
  		"addressLine2":"upper upperingham",
  		"addressLine3":"",
  		"townName":"upton",
  		"postcode":"UP103UP",
  		"region":"E",
  		"latitude":"50.863849",
  		"longitude":"-1.229710",
  		"uprn":"XXXXXXXXXXXXX",
  		"arid":"XXXXX",
  		"addressType":"CE",
  		"estabType":"XXX"
	},
	"contact": {
	"title":"Ms",
  	"forename":"jo",
  	"surname":"smith",
  	"email":"me@example.com",
  	"telNo":"+447890000000"
	},
        "state":"ACTIONABLE",
        "actionableFrom":"2011-08-12T20:17:46.384Z"
}
  }
}


UACUpdated events use the binding key event.uac.updates and have the following format:

{
  "event" : {
    "type" : "UAC_UPDATED",
    "source" : "CASE_SERVICE",
    "channel" : "RM",
    "dateTime" : "2011-08-12T20:17:46.384Z",
    "transactionId" : "c45de4dc-3c3b-11e9-b210-d663bd873d93"
  },
  "payload" : {
    "uac" : {
        "uacHash":  "72C84BA99D77EE766E9468A0DE36433A44888E5DEC4AFB84F8019777800B7364",
        "active" : true,
        "questionnaireId":"1110000009",
        "caseType":"H",
        "region":"E",
        "caseId":"bbd55984-0dbf-4499-bfa7-0aa4228700e9",
        "collectionExerciseId":"n66de4dc-3c3b-11e9-b210-d663bd873d93"
}
  }
}

## Docker image build

Is switched off by default for clean deploy. Switch on with;

* mvn dockerfile:build -Dskip.dockerfile=false

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)
