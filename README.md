# Deployment of Spring application in AWS

## Generate jar file

```
$ mvn clean package
```

## Launch application

```
$ java -DRDS_ENDPOINT="<rds-db-endpoint>" -DRDS_DATABASE="<rds-database>" -DRDS_USER="<rds-user>" -DRDS_PASS="<rds-password>" -DREGION="<aws-region>" -DBUCKET_NAME="<aws-bucket-name>" -jar target/d.rojo.2020-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```
