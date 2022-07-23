# Mook
Mook is an (intentionally) simple shared diary.

## Building
Mook requires Java 17 and a recent version of Maven.

## Configuration
Configuration for database, file storage and OAuth need to be set for each instance.
See [Quarkus config documentation](https://quarkus.io/guides/config-reference) for
alternatives.
* Database property `quarkus.datasource.jdbc.url` need to be set, with `quarkus.datasource.username` and `quarkus.datasource.password`
* Path to image storage must be set in `mook.image.path`
* Google client ID and callback URL must be set in `google.clientId` and `google.targetUrl`
* Display name and instance prefix may be set with `mook.name` and `mook.prefix`

Example (as .env file):
```shell
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mariadb://localhost:3306/mook_dev
QUARKUS_DATASOURCE_USERNAME=dbuser
QUARKUS_DATASOURCE_PASSWORD=***

MOOK_NAME=Lokal utvikling
MOOK_PREFIX=dev

MOOK_IMAGE_PATH=/home/data/mook-dev/img

GOOGLE_CLIENTID=xxxxx.apps.googleusercontent.com
GOOGLE_TARGETURL=http://localhost:8080
```
