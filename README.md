# Deployment of Spring application in AWS

The objective ot this repository is to deploy the Spring Boot application of the repo in AWS, using a EC2 instance that connects to a MySQL database in RDS (only the EC2 instance will be able to connect to the database).

Spring Boot application will use a S3 bucket, in order to store the images of the events that will be created using the exposed REST API using HTTPS. If S3 bucket doesn't exist, the application will create it.

Default spring profile is ```dev``` in order that application can be tested locally using H2 database and local filesystem to store event images, but when it will be deployed it will be launched with ```production``` profile using in this case, MySQL database in RDS and a S3 bucket to store event images.

## Steps

### Step 1: create Security Groups

Creation of Security Group for EC2 (__EC2_Events_SecurityGroup__):

    22	TCP     my_ip	
    8443	TCP     0.0.0.0/0

Creation of Security Group for RDS (__RDS_EventsApp_SecurityGroup__):

    MYSQL/Aurora	TCP	3306	sg-XXXXXXXXXXXXXXXX (EC2_Events_SecurityGroup)

### Step 2: create Role with S3 access

Creation of a role __S3FullAccess__ for EC2 with policy AmazonS3FullAccess

### Step 3: launch RDS

- MySQL 8.0
- Free Tier (db.t2.micro)
- USER: admin
- PASS: password
- DB:   events_db
- Security Group: __RDS_EventsApp_SecurityGroup__

### Step 4: launch EC2

- Ubuntu Server 18.04 LTS
- Free tier (t2.micro)
- Role de IAM: __S3FullAccess__
- Security Group: __EC2_Events_SecurityGroup__

### Step 5: connect to EC2 instance and configure it

local:

```sh
export EC2_DNS=
export KEYS=
ssh -i ${KEYS} ubuntu@${EC2_DNS}
```

remote (EC2):

```sh
sudo apt-get update
sudo apt install -y openjdk-11-jdk
java -version
```

### Step 6: generate jar file and copy it to remote server (from local to EC2)

local:

```sh
mvn install -DskipTests
scp -i ${KEYS} target/d.rojo.2020-0.0.1-SNAPSHOT.jar ubuntu@${EC2_DNS}:/home/ubuntu/app.jar
```

### Step 7: launch application

remote (EC2):

```sh
export RDS_DNS=
sudo java -jar -Dspring.profiles.active=production app.jar \
    --server.port=443 \
    --spring.datasource.url=jdbc:mysql://${RDS_DNS}/events_db \
    --spring.datasource.username=admin \
    --spring.datasource.password=password \
    --amazon.s3.bucket-name=practice-1.cloudapps.davidr \
    --amazon.s3.endpoint=https://practice-1.cloudapps.davidr.s3.eu-west-1.amazonaws.com/ \
    --amazon.s3.region=eu-west-1
```

## Author

[David Rojo (@david-rojo)](https://github.com/david-rojo)
