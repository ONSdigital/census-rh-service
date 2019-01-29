[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0c2913652fd94878b4c61838b54db11e)](https://www.codacy.com/app/zekizeki/census-rh-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ONSdigital/census-rh-service&amp;utm_campaign=Badge_Grade) [![Docker Pulls](https://img.shields.io/docker/pulls/sdcplatform/rhsvc.svg)]()
[//] # [![Build Status](https://travis-ci.org/ONSdigital/census-rh-service.svg?branch=master)](https://travis-ci.org/ONSdigital/census-rh-service)
[//] # [![codecov](https://codecov.io/gh/ONSdigital/census-rh-service/branch/master/graph/badge.svg)](https://codecov.io/gh/ONSdigital/census-rh-service)

# Respondent Data Service
This repository contains the Respondent Data service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/). It manages respondent data, where a Respondent Data object represents an expected response from the Respondent Data service, which provides all the data that is required by Respondent Home in order for it to verify the respondent's UAC code and connect them to the relevant EQ questionnaire.

## Set Up

Do the following steps to set up the code to run locally:
* Install Java 11 locally
* Make sure that you have a suitable settings.xml file in your local .m2 directory
* Clone the census-rh-service locally

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

## End Point

When running successfully the words "Hello Census Integration!" should be found at the following endpoint:
    
* localhost:8171/respondent/data
    

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)
