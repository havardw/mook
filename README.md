# Mook
Mook is an (intentionally) simple shared diary.

## Building
Mook requires Java 17 and a recent version of Maven.

## Configuration
Configuration for database (Postgresql), file storage and OAuth need to be set for the instance.
See [Quarkus config documentation](https://quarkus.io/guides/config-reference) for
alternatives on how to set configuration.
* Database property `quarkus.datasource.jdbc.url` need to be set, with `quarkus.datasource.username` and `quarkus.datasource.password`
* Path to image storage must be configured, _either_ a file path for `mook.image.path` _or_ an Azure Blob Storage connect string in `azure.blob.connect` 
* Google client ID and callback URL must be set in `google.clientId` and `google.targetUrl`
* Display name and instance prefix may be set with `mook.name` and `mook.prefix`

Example (as .env file):
```shell
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql:///db
QUARKUS_DATASOURCE_USERNAME=dbuser
QUARKUS_DATASOURCE_PASSWORD=***

MOOK_IMAGE_PATH=/home/user/mook-dev/img

GOOGLE_CLIENTID=xxxxx.apps.googleusercontent.com
GOOGLE_TARGETURL=http://localhost:8080

_TEST_QUARKUS_DATASOURCE_JDBC_URL=jdbc:h2:mem:unittest;MODE=POSTGRESQL
```
