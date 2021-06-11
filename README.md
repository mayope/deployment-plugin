# Gradle Deployment Plugin[![Maven metadata URL](https://img.shields.io/maven-metadata/v?label=Plugin&metadataUrl=https%3A%2F%2Fplugins.gradle.org%2Fm2%2Fnet%2Fmayope%2Fdeployplugin%2Fnet.mayope.deployplugin.gradle.plugin%2Fmaven-metadata.xml)](https://plugins.gradle.org/plugin/net.mayope.deployplugin)

This is an opinionated plugin to deploy docker images and helm charts to kubernetes.

Deployment configurations are organised through profiles and directives.

Available directives are: `dockerLogin`,`dockerBuild`, `dockerPush`, `deploy`,`helmPush`

The plugin is build for a multi-module setup. You may define default values in the rootProject and customize them in the
subProjects

## Requirements

- [helm](https://helm.sh/docs/intro/install/) is installed and available in path (only needed for deploy directive)
- [helmpush](https://github.com/chartmuseum/helm-push) is installed and available in path (only needed for helmPush
  directive)
- [docker](https://docs.docker.com/get-docker/) is installed and available in path

## Quickstart

Per default the plugin expects your sources in the following directories:

- Docker files: `src/docker`
- Helm chart: `src/helm`

### Workflow

Apply the current version of the Plugin:

```gradle
plugins {
  id "net.mayope.deployplugin" version "x.x.x"
}

deploy {
    serviceName = "hello" // docker image and helm deployment name
    default{ // use the default profile
       dockerBuild { 
          prepareTask = "prepareBuildDocker" // task the copies all needed files to build/buildDocker
          version = "0.0.1" // if not set gradle project version "-$timestamp" is used
       }
       dockerPush {
          registryRoot = "registry.mayope.net" // docker registry to use
          loginUsername = "username" // username for the docker registry, needed on login method classic
          loginPassword = "password" // password for the docker registry, needed on login method classic
          loginMethod = DockerLoginMethod.CLASSIC // Docker login method, for AWS see below
          awsProfile = "default" // default null, not set
       }
       deploy {
          targetNamespaces = listOf("default") // all namespaces where the app should be deployt
          attributes = mapOf("key" to "value") // this map is given to helm if you need to parameterize your helm chart
          kubeConfig = System.getProperty("user.home")+"/.kube/config" // default null, not set
       }
       helmPush {
            version = "0.0.1" //if not set gradle project version is used
            repositoryUrl = "https://charts.example.net"
            repositoryUsername = "username"
            repositoryPassword = "123456"
       }
    }
}
```

#### dockerBuild

Execute the `prepareDockerBuild` task which you have to configure.   
It should place all files needed for the docker build to the directory `build/buildDocker`.  
The docker image is built in the directory `build/buildDocker` and tagged with `{serviceName}:{timestamp}`

#### dockerPush

The previous build docker image is pushed to `{registry}/{serviceName}:{timestamp}`
and `{registry}/{serviceName}:{latest}`

#### deploy

The helm chart is applied through `helm upgrate --install {serviceName} . --image={registry}/{serviceName}:{timestamp}`
.  
If a kubeConfig is set it will be set as `KUBECONFIG` environment variable.  
`attributes` will be delivered to helm via the `--set` directive.  
If a `dockerPush` directive is present in the same profile it will also deliver the variables `image.version` with the
docker image version that was just build. And the variable `image.repository` with the image Repository that was just
build( e.g. `registry.mayope.net/demoapp`).

Per default the plugin won't redeploy a docker image that is already present in the same chart but marked with another tag.  
The plugin will check both docker images layer hashes and if they are identical it won't deploy a new version.
This can be turned off by the parameter `skipLayerCheck`.

#### helmPush
The helm chart is pushed to this helm repository using basic auth provided in the parameters

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
       dockerBuild {
         prepareTask = "prepareBuildDocker" 
       }
       dockerPush{
         registryRoot = "registry.mayope.net" 
         loginUsername = "username" 
         loginPassword = "password" 
         loginMethod = DockerLoginMethod.CLASSIC 
         awsProfile = ""
       }
       deploy{
         targetNamespaces = listOf("default") 
         kubeConfig = System.getProperty("user.home")+"/.kube/config"
         attributes = mapOf("key" to "value") // These attributes are merged with the attributes of deploy {}
       }
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
account. Therefore, the `awsProfile` is used as `AWS_PROFILE` environment variable for the `aws`-command.

