# CiscoVVB-AppDynamics-Plugin

[![published](https://static.production.devnetcloud.com/codeexchange/assets/images/devnet-published.svg)](https://developer.cisco.com/codeexchange/github/repo/jbsouthe/CiscoUCCE-AppDynamics-Plugin)

This iSDK plugin adds custom configuration for Cisco UCCE telemetry reporting to the AppDynamics Java Agent. This spares the end customer from having to manually configure the controller application specifically for UCCE. As we explore instrumentation of this Cisco application with the UCCE Engineering team, we will add features to this plugin to allow more customers to expand instrumentation without having to modify their configuration or environment directly.

### Prerequisite

The AppDynamics Java Agent v22.4+ must already be installed for this plugin to that agent to work.

### To Install 

Place the CiscoUCCEAgentPlugin-< version number>.jar file into the < AppDynamics Java Agent Home>/ver#*/sdk-plugins directory

### Features Added By Using This Plugin

UCCE has a specific configuration required within AppDynamics in order to instrument the application more fully. This plugin will add the following to AppDynamics:

- Business Transaction Auto Detection and Configuration
- Exit Call support for backend calls made during call management, including added correlation headers
- Custom Data for both Snapshots and Analytics to aid in troubleshooting and reporting.

### Please create an issue on the github project for any features or bugs you would like addressed.