[![Codacy Badge](https://api.codacy.com/project/badge/Grade/0c2913652fd94878b4c61838b54db11e)](https://www.codacy.com/app/zekizeki/rm-case-service?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ONSdigital/rm-case-service&amp;utm_campaign=Badge_Grade) [![Docker Pulls](https://img.shields.io/docker/pulls/sdcplatform/rhsvc.svg)]()
[![Build Status](https://travis-ci.org/ONSdigital/rm-case-service.svg?branch=master)](https://travis-ci.org/ONSdigital/rm-case-service)
[![codecov](https://codecov.io/gh/ONSdigital/rm-case-service/branch/master/graph/badge.svg)](https://codecov.io/gh/ONSdigital/rm-case-service)

# Case Service
This repository contains the Case service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/). It manages cases, where a case represents an expected response from an sample unit such as a business or a household. Every sample unit in the survey sample must have at least one associated case. Each case can have multiple questionnaires associated with it, but it must have at least one. Each questionnaire has a question set and a Unique Access Code (UAC). Interesting things that happen during the life cycle of a case are recorded as case events. Case life cycle transitions are published as JMS messages for interested parties to subscribe to.

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
See [API.md](https://github.com/ONSdigital/rm-case-service/blob/master/API.md) for API documentation.

## To test
See curlTests.txt under /test/resources

## Swagger Specifications
To view the Swagger Specifications for the Sample Service, run the service and navigate to http://localhost:8171/swagger-ui.html.

## Copyright
Copyright (C) 2017 Crown Copyright (Office for National Statistics)
