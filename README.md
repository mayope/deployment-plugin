# Gradle Deployment Plugin[![Maven metadata URL](https://img.shields.io/maven-metadata/v?label=Plugin&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fnet%2Fmayope%2Fdeployplugin%2Fnet.mayope.deployplugin.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/net.mayope.deployplugin)

This is an opinionated plugin to deploy docker images to kubernetes using helm.

Deployment configurations are organised through profiles.
These profiles determine which  `AWS_PROFILE`(in case of multiple accounts) to use and which `KUBECONFIG`(in case of multiple clusters) to take.

The plugin is build for a multi-module setup.
You may define default values in the rootProject and customize them in the subProjects

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
    serviceName = "hello" // docker image and helm deployment name
    default{ // use the default profile
       dockerRegistryRoot = "registry.mayope.net" // docker registry to use
       dockerLoginUsername = "username" // username for the docker registry, needed on login method classic
       dockerLoginPassword = "password" // password for the docker registry, needed on login method classic
       dockerLoginMethod = DockerLoginMethod.CLASSIC // Docker login method, for AWS see below
       targetNamespaces = listOf("default") // all namespaces where the app should be deployt
       prepareTask = "prepareBuildDocker" // task the copies all needed files to build/buildDocker
       attributes = mapOf("key" to "value") // this map is given to helm if you need to parameterize your helm chart
       awsProfile = "default" // default null, not set
       kubeConfig = System.getProperty("user.home")+"/.kube/config" // default null, not set
    }
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

deployDefault {
   default { // Use the default profile
       dockerRegistryRoot = "registry.mayope.net" 
       targetNamespaces = listOf("default") 
       prepareTask = "prepareBuildDocker" 
       awsProfile = ""
       kubeConfig = System.getProperty("user.home")+"/.kube/config"
       
       attributes = mapOf("key" to "value") // These attributes are merged with the attributes of deploy {}
       
       dockerLoginUsername = "username" 
       dockerLoginPassword = "password" 
       dockerLoginMethod = DockerLoginMethod.CLASSIC 
    }
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
Therefore, the `awsProfile` is used as `AWS_PROFILE` environment variable for the `aws`-command.

