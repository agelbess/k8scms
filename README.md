![logo](/assets/favicon.ico)
# [K8sCMS](https://k8scms.com)

## Introduction

A headless CMS (with admin UI) featuring [MongoDB](https://mongodb.com) and [Quarkus](https://quarkus.io).

NoSQL databases are ideal for creating generic Rest APIs on abstract data models. [K8sCMS](https://k8scms.com) was developed based on that 
concept. 

The power of Quarkus, simplified the development and minimised the development effort. 

I built the UI with vanilla javascript, jQuery and [Material Lite](https://getmdl.io/). Future releases will offer
[react](https://reactjs.org/) in place of the current UI implementation. Nevertheless, take a look at the `.js` files,
they are not bad for bootstrapping an idea and is surely super fast for development. Do you believe that `react`
is your second nature? try to rewrite the UI without breaking any API and UX before I do.

[K8sCMS](https://k8scms.com) can be used out of the box as a standalone application or a Docker image 
(to be deployed on k8s). Did I mention native compilation by using [GraalVM](https://www.graalvm.org/)?

Written entirely in Java 11.

The UI looks like that.

![overview](/assets/screenshot/cms_overview.png)

## Getting started

* Dock MongoDB on Docker and do not forget to open the `27017` port on localhost

      docker run -d -p 27017-27019:27017-27019 --name mongodb mongo
      
* Download [Robo eT](https://robomongo.org/), it the MongoDB client, use any other client, it is not required in order
to run [K8sCMS](https://k8scms.com)
    
* Git clone

      git clone https://github.com/agelbess/[K8sCMS](https://k8scms.com).git
      
* Run locally

      `mvn compile quarkus:dev`
      
* Open the UI [localhost:8080](http://localhost:8080)

* Login with any user/pass. [K8sCMS](https://k8scms.com) creates the provided user and password as a `super user` 
the first time you log into [K8sCMS](https://k8scms.com).

* Take a look at the `samples` folder and upload some users, roles and data. I have already forced the creation of the
`testDB.test` collection for development and testing purposes. It just waits for you to flood it with data. A good start
is the `samples/testDB_test.xlsx` file. Go to the [upload page](localhost:8080/#UPLOAD) and upload the `testDB_test.xlsx` file.

## Key features

### Rest API

A fluent rest API that covers

#### CRUD operations

GET, POST, PUT, PATCH, DELETE

#### Mongo 'like' queries

Query your data by writing `filters` just like you do on MongoDB.

e.g. the following will `GET`(using `POST` actually in order to overcome url size limitations) all the users containing the role 'su'
> pagination and sorting is also supported from the Rest API using the functionality of the MongoDB driver of Java.

    curl 'http://localhost:8080/api/cms/user/GET' \
      -H 'Connection: keep-alive' \
      -H 'Content-Type: application/json' \
      --data-binary '{"roles":"su","_limit":10,"_skip":0,"_sortDirection":1}' \
      --compressed
      
#### HTTP Basic Authentication

...to secure the API, this will be improved in later releases

#### Modeling of data

Describe the data models in the `cms.model` collection. Dynamically create the models from the UI, upload them as 
JSON files, or load them from the resource files.

For example, the model of the users is

    {
      "database": "cms",
      "collection": "user",
      "fields": [
        {
          "id": true,
          "name": "_id",
          "type": "oid"
        },
        {
          "name": "password",
          "type": "secret1"
        },
        {
          "name": "name"
        },
        {
          "name": "securityRealm",
          "regex": "^local|ldap$"
        },
        {
          "name": "email",
          "type": "email"
        },
        {
          "name": "phone",
          "type": "phone"
        },
        {
          "name": "permissions",
          "regex": "\\z|^.*:.*:.*$"
        },
        {
          "name": "roles",
          "type": "json"
        },
        {
          "name": "rolesRelation",
          "relation": "cms:role:{'name': { '$in': [{roles}] } }",
          "type": "json"
        }
      ],
      "indexes": [
        {
          "index": {
            "name": 1
          },
          "options": {
            "unique": true
          }
        },
        {
          "index": {
            "email": 1
          },
          "options": {
            "unique": true
          }
        }
      ]
    }

> [K8sCMS](https://k8scms.com) uses the same modeling technique for the system models (`user`, `role`, `model`) and the
> user dynamic models. If you need to secure the models from changes in production environments, simply omit the 
> `PUT|PATCH` permissions from the `users` and `roles`.

##### MongoDB indexes 

Models also describe the indexes to be created in Mongo. 

##### Primary Keys

Define `primary keys` with the `id` field in the model.

#### Eager relations

Define the relations in an expressing syntax and let [K8sCMS](https://k8scms.com) eagerly fetch the related documents 
along with the queried ones.

e.g. a user has roles, whenever you `GET` a user, you also 'GET' her `roles`

    {
      "_id": "5ec11d7beec77237a1ce024d",
      "name": "admin",
      "securityRealm": "local",
      "password": "********",
      "roles": [
        "su"
      ],
      "rolesRelation": [
        {
          "_id": "5ec11d7aeec77237a1ce024c",
          "name": "su",
          "permissions": ".*:.*:.*"
        }
      ]
    }

#### Data validation

Read about that later

### Data validation

Create the database models, upload them to the MongoDB collection `cms.model` and you will have validation of data.
> Validation does not prevent the persistence of data.

When using the UI, [K8sCMS](https://k8scms.com) will validate the uploaded data before they are persisted. 

Additionally, [K8sCMS](https://k8scms.com) will also validate the data every time you invoke the `GET` method.

The models describe the validation rules based on the data types.

The available data types are:
* `oid` mapped from the MongoDB `$oid` type as HEX
* `date` mapped from the MongoDB `$date` type as [ISO8601](https://www.iso.org/iso-8601-date-and-time-format.html)
* `string`
* `integer`
* `decimal`
* `boolean`
* `json` e.g `{"key":"value"}` or `["value", 1, true]` or `{"key":{"array":[1,2,"infinity"]}}`
* `email` helper type for email validation and creating email anchors
* `phone` helper type for creating phone anchors
* `secret1` mapped to one-way encrypted text on MongoDB
* `secret2` mapped to two-way encrypted text on MongoDB
* `cron` helper type for [cron expressions](https://en.wikipedia.org/wiki/Cron)

On top of the data types, regular expressions can be applied for syntax validation of the data

Define regular expressions e.g. for validating numbers as currencies and applying enumeration validation on the data.

E.g. the following defines a document field (named `euroRegex`) with numbers containing max tow decimal digits. It will
match for `1`, `12.1` and `0.01` but *not* for `1000.999`.

    {
      "name": "euroRegex",
      "regex": "^\\d*(\\.)?(\\d{1,2})?$"
    }

### Data upload

Upload your data using `xlsx` or `json` formats. Take a look at the `/samples` folder. Use that same functionality 
when updating the models.

### File download

Download the data from MongoDB by using the UI (as `xlsx` files) or by invoking the Rest API (as `json` arrays).

### Data migration

By using the download/upload functionality, you can seamlessly migrate data between, collections, databases and 
different environments. Migrating data from the staging to the production environment was never that easy.

### Logging

Log the CRUD endpoints that you only wish by changing the `application.properties`' `cms.log` property. It is a 
regular expression that defines which requests on collections and HTTP verbs must be logged.

By default `^(cms:(?!log)(.*):(POST|PUT|PATCH|DELETE))$|^(testDB:.*:DELETE)$` is used. 

This way, [K8sCMS](https://k8scms.com) will log only the data input requests on the `cms` database, excluding the 
`cms.log` collection.
> I included the `testDB` database and `DELETE` verb for testing only, you can safely remove it.

### Encryption

#### One way encryption

E.g. for passwords

#### Two way encryption

Data stored in MongoDB using two way encryption cannot be decrypted without the two keys that are stored in the 
source code and in the `application.properties` file.

### Security

#### Authentication

For user authentication, two options are available.

##### Local

A local authentication implementation is in place and uses MongoDB as the persistence of the user passwords.

[K8sCMS](https://k8scms.com) encrypts passwords using one way encryption.

##### LDAP

Setup the admin and the ldap url. Next create the users with the `ldap` `securityRealm`. 

Finally, setup `roles` and `permissions` per `role` or per `user`.

#### Authorization

MongoDB keeps the permissions (who can `GET`, `POST`, `PUT`, `PATCH` or `DELETE` data on collections) of the users in
the `cms.user` and `cms.role` collections as regular expressions.

The syntax is simple and powerful.

    <database regex>:<collection regex>:<http verb regex>

e.g. `.*:.*:.*` gives access to all the databases, all the collections and all the HTTP verbs.

`cms:log:.GET` gives `GET` only access to the `log` collection of the `cms` database.

### Beautiful material ui

* SPA makes your pod's life easy
* Browser navigation, browser refresh and history support by using # queries.
e.g. `http://localhost:8080/#COLLECTION&database=testDB&collection=test&_sort=date&_sortDirection=1&_skip=100&_limit=50`
* Pagination like never seen before
* Sorting
* Write MongoDB filters to query the data
* Upload/download excel with simple clicks
* `POST` or `PUT` documents from uploaded files based on primary keys (`_id` in Mongo)
* Cookie based security with strong encryption

### Native compilation

Minimize the footprint of CPU and memory on your k8s cluster.

## Native compilation and docker

Build docker image

    docker build -f Dockerfile -t k8scms .

Create Docker network for mongo and cms

Create network

    docker network create k8scms.com
    
Start mongo

    docker run -p 27017:27017 --name mongo --net k8scms mongo
    
Start cms

    docker run -i --rm -p 8080:8080 --name k8scms --net cms k8scms
   
### LDAP realm

#### Set up docker image

Docker run

    docker run -p 389:389 -p 636:636 --name ldap.test.com --env LDAP_ORGANISATION="Test Company" --env LDAP_DOMAIN="test.com" --env LDAP_ADMIN_PASSWORD="password" --detach osixia/openldap:1.3.0
    
Sample query

    docker exec ldap_test_com ldapsearch -x -H ldap://localhost -b dc=test,dc=com -D "cn=admin,dc=test,dc=com" -w password
    
## Screenshots

The `cms.user` table

![cms.user table](/assets/screenshot/cms_user.png)

The upload page with the pre-persist validation

![upload](/assets/screenshot/cms_upload.png)

Pagination, sorting and querying

![pagination validation quering](/assets/screenshot/cms_query_sort_pagination.png)

Data validations and JSON view

![validation and JSON](/assets/screenshot/cms_validations_json.png)

Drawer

![drawer](/assets/screenshot/cms_model_drawer.png)

Logs

![log](/assets/screenshot/cms_log.png)

Download excel

![log](/assets/screenshot/cms_download_excel.png)

## TODO

* LDAP authentication has not been tested over TLS
* The scheduling service needs some work. Documentation is also missing.
* A dark theme for the UI
