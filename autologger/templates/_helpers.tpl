{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "auto-logger-helm.name" -}}
{{- default .Chart.Name .Values.autologgerServiceAndDeployment.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "auto-logger-helm.fullname" -}}
{{- if .Values.autologgerServiceAndDeployment.fullnameOverride -}}
{{- .Values.autologgerServiceAndDeployment.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.autologgerServiceAndDeployment.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "auto-logger-helm.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "auto-logger-helm.labels" -}}
helm.sh/chart: {{ include "auto-logger-helm.chart" . }}
{{ include "auto-logger-helm.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "auto-logger-helm.selectorLabels" -}}
app.kubernetes.io/name: {{ include "auto-logger-helm.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: automation-logger
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "auto-logger-helm.serviceAccountName" -}}
{{- if .Values.autologgerServiceAndDeployment.serviceAccount.create -}}
    {{ default (include "auto-logger-helm.fullname" .) .Values.autologgerServiceAndDeployment.serviceAccount.name }}
{{- else -}}
    {{ default "default" .Values.autologgerServiceAndDeployment.serviceAccount.name }}
{{- end -}}
{{- end -}}
