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

Before installing deploy Helm chart you need to install [configmap Helm chart](../configmap).
First you need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Common variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**requestsCpu** | amount of requested CPU | string | 0.1 | yes
**requestsMemory** | amount of requested memory| string | 1G | yes
**limitsCpu** | CPU limit | string | 1 | yes
**limitsMemory** | memory limit | string | 1.5G | yes
**image** | service image | string | - | yes
**imagePullPolicy** | when to pull image | string | IfNotPresent | yes
**serviceAccountName** | name of your service account | string | schema | yes

### Bootstrap variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**bootstrapLimitsCpu** | CPU limit | string | 0.2 | yes
**bootstrapLimitsMemory** | memory limit | string | 200M | yes
**bootstrapImage** | bootstrap image | string | - | yes
**bootstrapServiceAccountName** | bootstrap service account name | string | - | yes

### Config variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**appName** | name of the app | string | `schema` | yes
**configmap** | configmap to be used | string | `schema-config` | yes
**bootstrapSecretName** | secret for bootstrap | string | `datafier-secret` | yes
**domain** | your domain | string | - | yes
**minioSecretName** | secret for minio | string | `schema-minio-secret` | yes
**onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**postgresSecretName** | secret for postgres | string | `schema-postgres-secret` | yes
**rabbitmqSecretName** | secret for rabbitmq | string | `rabbitmq-secret` | yes

### Install the helm chart

Run this command from within this directory:

```console
helm install gcp-schema-deploy .
```

## Uninstalling the Chart

To uninstall the helm deployment:

```console
helm uninstall gcp-schema-deploy
```

[Move-to-Top](#deploy-helm-chart)
