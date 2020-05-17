# K8sCMS

## Introduction

A headless CMS (with admin UI) featuring MongoDB and [Quarkus](https://quarkus.io).

NoSQL databases are ideal for creating generic APIs on content storage. K8sCMS was developed based on the abstract 
modeling of MongoDB. The power of Quarkus simplified development and minimised development effort. The UI is build 
with vanilla javascript, jQuery and [Material Lite](https://getmdl.io/).

K8sCMS can be used out of the box as a standalone application or a Docker image (to be deployed on k8s).

## 

## Native compilation and docker

Build native app

    mvn clean package -Pnative -Dquarkus.native.container-runtime=docker
    
Build docker image

    docker build -f src/main/docker/Dockerfile.native -t cms .
    
Create Docker network for mongo and cms

Create network

    docker network create cms
    
Start mongo

    docker run -p 27017:27017 --name mongo --net cms mongo
    
Start cms

    docker run -i --rm -p 8080:8080 --name cms --net cms cms
   
# Security
 
## LDAP realm

### Set up docker image

Docker run

    docker run -p 389:389 -p 636:636 --name ldap.test.com --env LDAP_ORGANISATION="Test Company" --env LDAP_DOMAIN="test.com" --env LDAP_ADMIN_PASSWORD="password" --detach osixia/openldap:1.3.0
    
Sample query

    docker exec ldap_test_com ldapsearch -x -H ldap://localhost -b dc=test,dc=com -D "cn=admin,dc=test,dc=com" -w password
    

