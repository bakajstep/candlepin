# Podman Production Image Demo

## Directories
- **cp-ubi-base-image**: a Candlepin image build that uses the UBI 8 OpenJDK JRE base container and Candlepin war file.
- **ubi_tomcat**: a Candlepin image build that uses the JBoss Web Server 3.1 - Tomcat 8 base image and Candlepin rpm.

The following are the steps for building and running a demo Candlepin production image in a pod that includes a database container.

## Prerequisites
- Login to registry.redhat.io ( https://access.redhat.com/RegistryAuthentication )
- System is registered with subscription-manager

## Demo steps:

1. Building the Candpepin base image
    - In the ./cp-ubi-base-image directory run the following command to create a Candlepin container image

    ```
    podman build -t <image name>:<version> .
    ```

2. Create a pod that exposes ports for Tomcat, candlepin, and Postgres
    ```
    podman pod create --name <pod name> -p 8080:8080 -p 5432:5432 -p 8443:8443
    ```

3. Run Postgres container in the pod
    ```
    podman run --pod <pod name> --name postgres -e POSTGRES_PASSWORD=admin -d postgres
    ```
    - You will also need to add the 'candlepin' user before starting the candlepin container as the cpsetup script requires it.

4. Run Candlepin container in the pod
    ```
    podman run --pod <pod name> -itd <image>
    ```