<!--- Deploy --->

# Deploy helm chart

## Introduction

This chart installs a deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

## Prerequisites

The code was tested on **Kubernetes cluster** (v1.21.11) with **Istio** (1.12.6)
> It is possible to use other versions, but it hasn't been tested

### Operation system

The code works in Debian-based Linux (Debian 10 and Ubuntu 20.04) and Windows WSL 2. Also, it works but is not guaranteed in Google Cloud Shell. All other operating systems, including macOS, are not verified and supported.

### Packages

Packages are only needed for installation from a local computer.

* **HELM** (version: v3.7.1 or higher) [helm](https://helm.sh/docs/intro/install/)
* **Kubectl** (version: v1.21.0 or higher) [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Installation

First you need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Global variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**global.domain** | your domain for the external endpoint, ex `example.com` | string | - | yes
**global.onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**global.limitsEnabled** | whether CPU and memory limits are enabled | boolean | true | yes

### Configmap variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**data.dataPartitionId** | data partition id | string | - | yes
**data.entitlementsHost** | entitlements host | string | "http://entitlements" | yes
**data.javaOptions** | java options | string | "-Xms512M -Xmx1024M -XX:+UseG1GC -XX:+UseStringDeduplication -XX:InitiatingHeapOccupancyPercent=45" | yes
**data.logLevel** | logging level | string | INFO | yes
**data.partitionHost** | partition host | string | "http://partition" | yes
**data.schemaTopicName** | topic for schema changes events | string | "schema-changed" | yes
**data.springProfilesActive** | active spring profile | string | gcp | yes

### Deployment variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**data.requestsCpu** | amount of requested CPU | string | 220m | yes
**data.requestsMemory** | amount of requested memory| string | 1.7G | yes
**data.limitsCpu** | CPU limit | string | 1 | only if `global.limitsEnabled` is true
**data.limitsMemory** | memory limit | string | 1.5G | only if `global.limitsEnabled` is true
**data.bootstrapImage** | bootstrap image | string | - | yes
**data.bootstrapServiceAccountName** | bootstrap service account name | string | - | yes
**data.image** | service image | string | - | yes
**data.imagePullPolicy** | when to pull image | string | IfNotPresent | yes
**data.serviceAccountName** | name of your service account | string | schema | yes

### Configuration variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**conf.appName** | name of the app | string | `schema` | yes
**conf.bootstrapSecretName** | secret for bootstrap | string | `datafier-secret` | yes
**conf.configmap** | configmap to be used | string | `schema-config` | yes
**conf.minioSecretName** | secret for minio | string | `schema-minio-secret` | yes
**conf.postgresSecretName** | secret for postgres | string | `schema-postgres-secret` | yes
**conf.rabbitmqSecretName** | secret for rabbitmq | string | `rabbitmq-secret` | yes

### Datastore cleanup and bootstrap schemas variables

> Datastore cleanup is used for cleaning Datastore Schema Entities if they are not present in Schema bucket

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**data.datastoreKind** | Datastore Kind for Schema | string | "system_schema_osm" | yes
**data.datastoreNamespace** | Datastore Namespace for Schema | string | "dataecosystem" | yes
**data.enableCleanup** | whether cleanup is enabled | boolean | false | yes
**data.schemaBucket** | name of the bucket with schemas | string | - | yes
**data.schemaHost** | schema host | string | "http://schema" | yes

### Istio variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**istio.proxyCPU** | CPU request for Envoy sidecars | string | `90m` | yes
**istio.proxyCPULimit** | CPU limit for Envoy sidecars | string | `500m` | yes
**istio.proxyMemory** | memory request for Envoy sidecars | string | `100Mi` | yes
**istio.proxyMemoryLimit** | memory limit for Envoy sidecars | string | `512Mi` | yes
**istio.bootstrapProxyCPU** | CPU request for Envoy sidecars | string | `10m` | yes
**istio.bootstrapProxyCPULimit** | CPU limit for Envoy sidecars | string | `100m` | yes

### Install the helm chart

Run this command from within this directory:

```console
helm install gc-schema-deploy .
```

## Uninstalling the Chart

To uninstall the helm deployment:

```console
helm uninstall gc-schema-deploy
```

[Move-to-Top](#deploy-helm-chart)
