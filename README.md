[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0c2913652fd94878b4c61838b54db11e)](https://www.codacy.com/app/zekizeki/census-rh-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ONSdigital/census-rh-service&amp;utm_campaign=Badge_Grade) [![Docker Pulls](https://img.shields.io/docker/pulls/sdcplatform/rhsvc.svg)]()
[//] # [![Build Status](https://travis-ci.org/ONSdigital/census-rh-service.svg?branch=master)](https://travis-ci.org/ONSdigital/census-rh-service)
[//] # [![codecov](https://codecov.io/gh/ONSdigital/census-rh-service/branch/master/graph/badge.svg)](https://codecov.io/gh/ONSdigital/census-rh-service)

# Respondent Data Service
This repository contains the Respondent Data service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/). It manages respondent data, where a Respondent Data object represents an expected response from the Respondent Data service, which provides all the data that is required by Respondent Home in order for it to verify the respondent's UAC code and connect them to the relevant EQ questionnaire.

## Running

There are two ways of running this service

* The easiest way is via docker (https://github.com/ONSdigital/ras-rm-docker-dev)
* Alternatively running the service up in isolation
    ```bash
    cp .maven.settings.xml ~/.m2/settings.xml  # This only needs to be done once to set up mavens settings file
    mvn clean install
    mvn spring-boot:run
    ```

# Code Styler
To use the code styler please goto this url (https://github.com/google/google-java-format) and follow the Intellij instructions or Eclipse depending on what you use

## API
See [API.md](https://github.com/ONSdigital/census-rh-service/blob/master/API.md) for API documentation.

## To test
See curlTests.txt under /test/resources

## Swagger Specifications
To view the Swagger Specifications for the Sample Service, run the service and navigate to http://localhost:8171/swagger-ui.html.

## Copyright
Copyright (C) 2017 Crown Copyright (Office for National Statistics)
