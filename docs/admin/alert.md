# Configure Alerts
Alerts are sent from Hopsworks using Prometheus' 
[Alert manager](https://prometheus.io/docs/alerting/latest/alertmanager/).
In order to send alerts we first need to configure the _Alert manager_.
To do that click on your name in the top right corner of the navigation bar and choose Cluster Settings from the dropdown menu.
In the Cluster Settings' Alerts tab you can configure the alert manager to send alerts
via email, slack or pagerduty.

<figure>
  <a href="../../assets/images/alerts/configure-alerts.png">
    <img src="../../assets/images/alerts/configure-alerts.png" alt="Configure alerts"/>
  </a>
  <figcaption>Configure alerts</figcaption>
</figure>

### 1. Email Alerts
To send alerts via email you need to configure an SMTP server. Click on the _Configure_ 
button on the left side of the **email** row and fill out the form that pops up.

<figure>
  <a href="../../assets/images/alerts/smtp-config.png">
    <img src="../../assets/images/alerts/smtp-config.png" alt="Configure Email Alerts"/>
  </a>
  <figcaption>Configure Email Alerts</figcaption>
</figure>

- _Default from_: the address used as sender in the alert email.
- _SMTP smarthost_: the Simple Mail Transfer Protocol (SMTP) host through which emails are sent.
- _Default hostname (optional)_: hostname to identify to the SMTP server.
- _Authentication method_: how to authenticate to the SMTP server.
  CRAM-MD5, LOGIN or PLAIN.

Optionally cluster wide Email alert receivers can be added in _Default receiver emails_.
These receivers will be available to all users when they create event triggered [alerts](../compute/alerts.md).

### 2. Slack Alerts
Alert can also be sent via Slack message. To be able to send Slack messages you first need to configure
a Slack webhook. Click on the _Configure_ button on the left side of the **slack** row and past in your
[Slack webhook](https://api.slack.com/messaging/webhooks) in _Webhook_.

<figure>
  <a href="../../assets/images/alerts/slack-config.png">
    <img src="../../assets/images/alerts/slack-config.png" alt="Configure slack Alerts"/>
  </a>
  <figcaption>Configure slack Alerts</figcaption>
</figure>

Optionally cluster wide Slack alert receivers can be added in _Slack channel/user_.
These receivers will be available to all users when they create event triggered [alerts](../compute/alerts.md).

### 3. Pagerduty
Pagerduty is another way you can send alerts from Hopsworks. Click on the _Configure_ button on the left side of 
the **pagerduty** row and fill out the form that pops up. 

<figure>
  <a href="../../assets/images/alerts/pagerduty-config.png">
    <img src="../../assets/images/alerts/pagerduty-config.png" alt="Configure Pagerduty Alerts"/>
  </a>
  <figcaption>Configure Pagerduty Alerts</figcaption>
</figure>

Fill in Pagerduty URL: the URL to send API requests to.

Optionally cluster wide Pagerduty alert receivers can be added in _Service key/Routing key_.
By first choosing the PagerDuty integration type:

- _global event routing (routing_key)_: when using PagerDuty integration type `Events API v2`.
- _service (service_key)_: when using PagerDuty integration type `Prometheus`.

Then adding the Service key/Routing key of the receiver/s. PagerDuty provides 
[documentation](https://www.pagerduty.com/docs/guides/prometheus-integration-guide/) on how to integrate with 
Prometheus' Alert manager.

### Advanced configuration
If you are familiar with Prometheus' [Alert manager](https://prometheus.io/docs/alerting/latest/alertmanager/) 
you can also configure alerts by editing the _yaml/json_ file directly.  

<figure>
  <a href="../../assets/images/alerts/advanced-config.png">
    <img src="../../assets/images/alerts/advanced-config.png" alt="Advanced configuration"/>
  </a>
  <figcaption>Advanced configuration</figcaption>
</figure>

_Example:_ Adding the yaml snippet shown below in the global section of the alert manager configuration will
have the same effect as creating the SMTP configuration as shown in [section 1](#1-email-alerts) above.

```yaml
global:
    smtp_smarthost: smtp.gmail.com:587
    smtp_from: hopsworks@gmail.com
    smtp_auth_username: hopsworks@gmail.com
    smtp_auth_password: XXXXXXXXX
    smtp_auth_identity: hopsworks@gmail.com
 ...
```

To test the alerts by creating triggers from Jobs and Feature group validations see [Alerts](../compute/alerts.md).