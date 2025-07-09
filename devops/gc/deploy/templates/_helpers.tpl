
#  Copyright 2025 Google LLC
#  Copyright 2025 EPAM
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

{{/*
Expand the name of the chart.
*/}}
{{- define "deploy.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "deploy.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "deploy.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "deploy.labels" -}}
helm.sh/chart: {{ include "deploy.chart" . }}
{{ include "deploy.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "deploy.selectorLabels" -}}
app.kubernetes.io/name: {{ include "deploy.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "deploy.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "deploy.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

# Function to check the value of the global.tier
{{- define "schema.getTier" -}}
{{- $tier := .Values.global.tier -}}
{{- $allowedTiers := list "" "DEV" "STAGE" "PROD" -}}

{{- if not (has $tier $allowedTiers) -}}
  {{- fail (printf "Invalid 'global.tier' value: '%s'. Must be one of %v." $tier $allowedTiers) -}}
{{- end -}}
{{- $tier -}}
{{- end -}}

# Function to check the value of the global.autoscalingMode
{{- define "schema.getAutoscaling" -}}
{{- $scale := .Values.global.autoscalingMode -}}
{{- $allowedScale := list "none" "cpu" "requests" -}}

{{- if not (has $scale $allowedScale) -}}
  {{- fail (printf "Invalid 'global.autoscalingMode' value: '%s'. Must be one of %v." $scale $allowedScale) -}}
{{- end -}}
{{- end -}}

# Function to define the minimum number of replicas for spot deployment
{{- define "schema.minReplicasSpot" -}}
{{- $tier := include "schema.getTier" . -}}

{{- if eq $tier "DEV" -}} 1
{{- else if eq $tier "STAGE" -}} 2
{{- else if eq $tier "PROD" -}} 3
{{- else -}} {{ .Values.hpa.minReplicas }}
{{- end -}}
{{- end -}}

# Function to define the maximum number of replicas for spot deployment
{{- define "schema.maxReplicasSpot" -}}
{{- $tier := include "schema.getTier" . -}}

{{- if eq $tier "DEV" -}} 5
{{- else if eq $tier "STAGE" -}} 7
{{- else if eq $tier "PROD" -}} 10
{{- else -}} {{ sub .Values.hpa.maxReplicas 1 }}
{{- end -}}
{{- end -}}

# Function to define the minimum number of replicas for standard deployment
{{- define "schema.replicasStandard" -}}
{{- $tier := include "schema.getTier" . -}}

{{- if eq $tier "DEV" -}} 1
{{- else if eq $tier "STAGE" -}}
  {{- if (ne .Values.global.autoscalingMode "none") }} 2
  {{- else -}} 3 
  {{- end -}}
{{- else if eq $tier "PROD" -}}
  {{- if (ne .Values.global.autoscalingMode "none") }} 3 
  {{- else -}} 5 
  {{- end -}}
{{- else if eq $tier "" -}} 1
{{- end -}}
{{- end -}}
