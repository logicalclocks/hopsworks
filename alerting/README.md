# Java client for Alertmanager

This is a library that cab be used to communicate with **Prometheus’ Alertmanager**.
There are two operations that can be done on the Alertmanager.

1. Update Alertmanager configuration.
2. Communicate with the Alertmanager via its APIs.

## Table of contents
1. [Installation](#installation)
2. [Usage](#usage)
3. [Testing](#testing)

## Installation

To use the library include the dependency in your `pom.xml` file

    <dependency>
        <groupId>io.hops.hopsworks</groupId>
        <artifactId>alerting</artifactId>
        <version>VERSION</version>
    </dependency>

## Usage

There are two classes that can be used to communicate with the Alertmanager.
 1. **AlertManagerConfigController**: provides methods for updating and reloading the Alertmanager's configuration.
 2. **AlertManagerClient**: provides methods to communicate with the Alertmanager's Rest API.

### AlertManagerConfigController
The **AlertManagerConfigController** can be used to add, update or delete receivers and routes.

> **_NOTE:_**  The *AlertManagerConfigController* is **Not** thread safe.


The following example shows how to add a receiver.
```java

 AlertManagerClient client = new AlertManagerClient.Builder(ClientBuilder.newClient())
    .withTarget("serviceFQDN") // the FQDN of the alertmanager (default) 'alertmanager.prometheus.service.consul'
    .build();
 try {
     AlertManagerConfigController alertManagerConfigController =
        new AlertManagerConfigController(alertmanagerConfigFile, client);

     List<EmailConfig> emailConfigList = new ArrayList<>();
     emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
     Receiver receiver = new Receiver("team-Z-email").withEmailConfigs(emailConfigList);

     AlertManagerConfig alertManagerConfig = alertManagerConfigController.addReceiver(receiver);
     alertManagerConfigController.writeAndReload(alertManagerConfig);
 } finally {
     if (client != null) {
        client.close();
     }
 }

```

Create AlertManagerConfigController with default client.
(alertmanagerConfigFile="/srv/hops/alertmanager/alertmanager/alertmanager.yml" and serviceFQDN="alertmanager.prometheus.service.consul")
```java
 try {
     AlertManagerConfigController alertManagerConfigController =
        new AlertManagerConfigController();
     List<EmailConfig> emailConfigList = new ArrayList<>();
     emailConfigList.add(new EmailConfig("team-Z+alerts@example.org"));
     Receiver receiver = new Receiver("team-Z-email").withEmailConfigs(emailConfigList);

    AlertManagerConfig alertManagerConfig = alertManagerConfigController.addReceiver(receiver);
    alertManagerConfigController.writeAndReload(alertManagerConfig);
 } finally {
    alertManagerConfigController.closeClient();
 }

```


### AlertManagerClient

The **AlertManagerClient** can be used to post *alerts*, *silences* and *reload configuration*.
Additionally, you can query the Alertmanaget for *health*, *readiness*, *status* and existing *alerts* and
*silences*.

The example below shows how to post an alert using the *AlertManagerClient*.

```java
AlertManagerClient client = new AlertManagerClient.Builder(ClientBuilder.newClient())
    .withTarget("serviceFQDN") // the FQDN of the alertmanager (default) 'alertmanager.prometheus.service.consul'
    .build();
try {
    Map<String, String> labels = new HashMap<>();
    labels.put("alertname", "HopsworksAlert");
    labels.put("instance", "hopsworks0");
    labels.put("severity", "warning");
    labels.put("project", "project1");
    Map<String, String> annotations = new HashMap<>();
    annotations.put("info", "project1 info");
    annotations.put("summary", "Job finished.");
    PostableAlert alert = new PostableAlert(labels, annotations);
    Response res = client.alertsApi().postAlerts(alerts);
} finally {
    if (client != null) {
      client.close();
    }
}
```

### Alertmanager HA
Alertmanager supports configuration to create a cluster for high availability. If the Alertmanager is
running in HA mode a list of *AlertManagerClients* can be created to send alerts to all Alertmanagers.

```java
AlertManagerClient client1 = new AlertManagerClient.Builder(ClientBuilder.newClient())
    .withTarget("alertmanager1.prometheus.service.consul")
    .build();
AlertManagerClient client2 = new AlertManagerClient.Builder(ClientBuilder.newClient())
    .withTarget("alertmanager2.prometheus.service.consul")
    .build();
...
```

### Alertmanager *Https* client
```java
AlertManagerClient client = new AlertManagerClient.Builder(ClientBuilder.newClient())
    .enableHttps()
    .build();
...
```


## Testing
There are tests that run against a mocked AlertManager API.

To run tests from ` hopsworks-ee/alerting ` folder run:

    mvn test