# Respondent Home Service API
This page documents the Respondent Home service API endpoints. These endpoints are secured using HTTP basic authentication. All endpoints return an `HTTP 200 OK` status code except where noted otherwise.

## Respondent Home Details
For the endpoints that return the details, to be used by RH, which come from the following 5 RM services via a message queue:
- case
- iac
- collex
- collection-instrument
- sample

* `GET /getRespondentData


