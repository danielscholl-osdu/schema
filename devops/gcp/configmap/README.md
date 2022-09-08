<!--- Configmap --->

# Configmap helm chart

This chart installs a configmap deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

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

### Common variables for GCP and Anthos implementation

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**dataPartitionId** | data partition id | string | - | yes
**entitlementsHost** | entitlements host | string | "http://entitlements" | yes
**javaOptions** | java options | string | "-Xms512M -Xmx1024M -XX:+UseG1GC -XX:+UseStringDeduplication -XX:InitiatingHeapOccupancyPercent=45" | yes
**logLevel** | logging level | string | INFO | yes
**partitionHost** | partition host | string | "http://partition" | yes
**schemaTopicName** | topic for schema changes events | string | "schema-changed" | yes
**springProfilesActive** | active spring profile | string | gcp | yes

### GCP specific variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**googleAudiences** | your GCP client ID | string | - | yes

### Datastore cleanup and bootstrap schemas variables

> Datastore cleanup is used for cleaning Datastore Schema Entities if they are not present in Schema bucket

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**datastoreKind** | Datastore Kind for Schema | string | "system_schema_osm" | yes
**datastoreNamespace** | Datastore Namespace for Schema | string | "dataecosystem" | yes
**enableCleanup** | whether cleanup is enabled | boolean | false | yes
**schemaBucket** | name of the bucket with schemas | string | - | yes
**schemaHost** | schema host | string | "http://schema" | yes

### Config variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**appName** | name of the app | string | schema | yes
**configmap** | configmap to be used | string | schema-config | yes
**onPremEnabled** | whether on-prem is enabled | boolean | false | yes

### Install the helm chart

Run this command from within this directory:

```console
helm install gcp-schema-configmap .
```

## Uninstalling the chart

To uninstall the helm deployment:

```console
helm uninstall gcp-schema-configmap
```

[Move-to-Top](#configmap-helm-chart)
