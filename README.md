# Using nginx as a load balancer on Docker
This is a tutorial on how to use nginx as a load balancer on Docker. This tutorial uses a sample Spring Boot Java application launched with multiple instances as an API to show how to use nginx.

## How to start the Docker container
1. Move to the SampleJavaApp folder - `cd SampleJavaApp`
2. Package the Java application - `mvn package`
3. Move back to the root folder - `cd ../`
4. Start the Docker container - `docker-compose up -d`
5. The Sample Java App will now be available at http://localhost:4000, served through nginx.

## Starting multiple instances of the same app in Docker
There are two ways to start multiple instances of the same app in Docker, by modifying the `docker-compose.yml` file:
### Repeating configuration
If the instances have any configuration differences between them (for example, extra environment properties), you can repeat the configuration as many times as possible:
```dockerfile
services:
  backend-1:
    build:
      context: SampleJavaApp
      dockerfile: Dockerfile
    environment:
      - "PROPERTY1=value1"
  backend-2:
    build:
      context: SampleJavaApp
      dockerfile: Dockerfile
    environment:
      - "PROPERTY1=value2"
```
### Deploy settings
If the instances are identical, you can use the deploy properties:
```dockerfile
services:
  backend:
    build:
      context: SampleJavaApp
      dockerfile: Dockerfile
    deploy:
      mode: replicated
      replicas: 4
```
This configuration launches 4 replicas of the application in Docker.

## nginx configuration
To begin with, nginx needs to be added in the `docker-compose.yml` file, under the `services` tag:
```dockerfile
  nginx:
    image: nginx:latest
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    ports:
      - "4000:4000"
```
The `volumes` setting ensures that the configuration we set locally will be used in the Docker instance. Also note the ports that you expose, as you will need them in the configuration.

Now, for the nginx.conf file, let us take a closer look:
```nginx configuration
# The events block is necessary, leaving it empty keeps the default values
events {}
# If you set up HTTPS, change this to https
http {
    upstream backend-stream {
        # Add all of the containers you defined as servers here.
        # Make sure that the port specified is the one that your app actually uses.
        server backend:8080;
    }

    server {
        # The port on which nginx will listen, needs to be the same one you set up in docker-compose
        listen 4000;
        # This configuration passes all requests made to localhost:4000 to our application
        location / {
            proxy_pass http://backend-stream;
        }
    }
}
```
This is the most basic configuration you can use. This will ensure a round-robin balancing to our app instances. 
You can test this out by going to [http://localhost:4000](http://localhost:4000) and checking the Docker instances. 
You'll see that only one of them outputs a log telling you that it was used. Refreshing the page will keep changing the instance that was used.

For further configuration, we need to have the app instances declared separately, as demonstrated in [Repeating configuration](#repeating-configuration).

If you want to change the type of load balancing used, you need to add format the `upstream` block as follows:
```nginx configuration
    upstream backend-stream {
        # Possible options:
        # round-robin - requests are distributed in a round-robin fashion; don't specify anything for this option
        # least_conn - next request is assigned to the server with the least number of active connections
        # ip_hash - uses a hash function to ensure that requests coming from the same IP are handled by the same server
        least_conn;
        server backend-1:8080;
        server backend-2:8080;
    }
```

You are also able to use weights, if you prefer one of your servers more. The default weight is 1.
```nginx configuration
    upstream backend-stream {
        server backend-1:8080 weight=2;
        server backend-2:8080;
    }
```
In this example, the first server will receive twice as many requests as the second one.

### Modifying requests
Requests that pass through nginx can be modified. A classic example is setting new headers:
```nginx configuration
        location / {
            add_header 'X-Header-Name' 'Header-Value';
            proxy_pass http://backend-stream;
        }
```

More information is available by visiting the [official nginx documentation](http://nginx.org/en/docs/).