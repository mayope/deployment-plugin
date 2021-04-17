# Gradle Deployment Plugin

This is an opinionated plugin to deploy docker images to kubernetes using helm.

## Requirements

- [helm](https://helm.sh/docs/intro/install/) is installed and available in path
- [docker](https://docs.docker.com/get-docker/) is installed and available in path

## Quickstart

The plugin expects your sources in the following directories:

- Docker files: `src/docker`
- Helm chart: `src/helm`

### Workflow

1. Execute the `prepareDockerBuild` task which you have to configure.
   It should place all files needed for the docker build to the directory `build/buildDocker`.
2. The docker image is built in the directory `build/buildDocker` and tagged with `{registry}/{serviceName}:{timestamp}`
3. The previously build docker image is pushed to `{registry}/{serviceName}:{timestamp}`
   and `{registry}/{serviceName}:{latest}`
4. The helm chart is applied
   through `helm upgrate --install {serviceName} . --image={registry}/{serviceName}:{timestamp}`

Apply the current version of the Plugin:

```gradle
plugins {
  id "net.mayope.deployplugin" version "x.x.x"
}

deploy {
    serviceName = "hello" # docker image and helm deployment name
    dockerRegistryRoot = "registry.mayope.net" # docker registry to use
    dockerLoginUsername = "username" # username for the docker registry, needed on login method classic
    dockerLoginPassword = "password" # password for the docker registry, needed on login method classic
    dockerLoginMethod = DockerLoginMethod.CLASSIC # Docker login method, for AWS see below
    targetNamespaces = listOf("default") # all namespaces where the app should be deployt
    prepareTask = "prepareBuildDocker" # task the copies all needed files to build/buildDocker
    attributes = mapOf("key" to "value") # this map is given to helm if you need to parameterize your helm chart
}
```

## Multi Project Setup

For a multi project setup you can specify `defaultDeploy` parameters in the rootProject. (only the root project is
considered)

These attributes are taken for each deploy plugin if you do not specify them otherwise.

```gradle
plugins {
  id "net.mayope.deployplugin" version "x.x.x"
}

defaultDeploy {
    defaultDockerRegistryRoot = "registry.mayope.net" 
    defaultTargetNamespaces = listOf("default") 
    defaultPrepareTask = "prepareBuildDocker" 
    defaultAttributes = mapOf("key" to "value") # These attributes are merged with the attributes of deploy {}
    
    defaultDockerLoginUsername = "username" 
    defaultDockerLoginPassword = "password" 
    defaultDockerLoginMethod = DockerLoginMethod.CLASSIC 
}
```

## Docker Login Methods

Currently, two login methods are available.

### CLASSIC

This method uses the `docker login` command with username and password. You have to provide the
parameters `dockerLoginUserName` and `dockerLoginPassword`.

### AWS

This method extracts the login token from the `aws ecr get-login-password` command. Therefore, you need to have
the [aws-cli v2](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html) installed and configured for your
account.

