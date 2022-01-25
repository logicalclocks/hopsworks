# Alerts

Alerts can be triggered by Jobs or Feature group validations. This can be achieved by creating alert for a job, 
a feature group or a project. Project alerts ([Global Alerts](#global-alerts)) can be triggered by any job or feature 
group validation in that project. 
Whereas alerts created for a job ([Job Alerts](#job-alerts)) or a feature group ([Feature validation Alerts](#feature-validation-alerts)) 
are only triggered by the specific job or feature group they are created for. 

When an alert is triggered it sends a message, via the chosen channel (email, slack or pagerduty), to a user or 
group of users referred here as _Receivers_. 

## Create Alert Receivers

Receivers are the destinations to which your alerts will be sent to.
To create _Alert Receivers_ go a project you want to send alerts for and click on **Settings** on the side menu.
In the submenu you will find **Alerts**, click on it to go to the alerts page. In the alerts page you can see if 
alert channels (email, slack or pagerduty) are configured for the cluster. If not go to the 
[Alert Configuration](../admin/alert.md) to see how you can configure them. 

If the alert channels you want to use are configured you can create a receiver for those channels by clicking on **Add 
receiver**. Receivers can also be created by an admin when configuring channels in [Alert Configuration](../admin/alert.md). 

<figure>
  <a href="../../assets/images/alerts/create-receiver.png">
    <img src="../../assets/images/alerts/create-receiver.png" alt="Create receiver"/>
  </a>
  <figcaption>Alert receivers</figcaption>
</figure>

- Channel: the channel through which the receiver will get alerts.
- Receiver name: a unique name that will represent the receiver/s.
- Slack channels/users: channel names or users. This filed will be Emails if the channel chosen is email and service 
  key or routing key if pagerduty is the channel.

!!!note
   
    Receivers created inside a project can only be used by alerts created in that project. Receivers created by an 
    admin on the otherhand can be used by any alert created in the cluster.

## Global Alerts

To create a _Global Alert_ you need a channel and at least one receiver. See the section above on how to 
[Create Alert Receivers](#create-alert-receivers).

A _Global Alert_ is created for a project and can be configured to trigger on any job or feature validation event in 
that project. You can create a _Global Alert_ by going to your project setting’s alerts section and clicking on 
**Add global event**. 

<figure>
  <a href="../../assets/images/alerts/project-alerts.png">
    <img src="../../assets/images/alerts/project-alerts.png" alt="Project alerts"/>
  </a>
  <figcaption>Project alerts</figcaption>
</figure>

- Trigger: the event that will trigger the alert. Alerts can be triggered on events:
    1. on job success, fail or killed
    2. on data ingestion success, warning or fail
- Receiver: the receiver that will be notified when an alert is triggered.
- Severity: the potential severity this alert should report. Can be info, warning or critical.

## Feature validation Alerts

You can create alerts on a per-feature-group basis if you want to receive notifications for feature group specific 
events. Before you proceed make sure you have created receivers 
(see section [Create Alert Receivers](#create-alert-receivers) on how to do that). 

Feature validation Alerts can be created when a feature group is created or from the edit feature group page for 
existing feature groups.

When creating a new Feature group you will find the **Alerts** card on the bottom of the _Create New Feature Group_ 
page. From the _Alerts_ card you can add alerts by clicking on _Add another alert_ and choose the trigger, 
severity and receiver as explained in the section above.

<figure>
  <a href="../../assets/images/alerts/create-fg-alert.png">
    <img src="../../assets/images/alerts/create-fg-alert.png" alt="Create Feature Validation Alert"/>
  </a>
  <figcaption>Create Feature Validation Alert for New Feature group</figcaption>
</figure>

To create alerts for an existing feature group go to the feature group overview page (by clicking on the magnifying glass 
icon). On the bottom of the page you will find the _Alerts_ card. Click on **Add an alert** and choose the 
trigger, receiver and severity as explained in the [Global Alerts](#global-alerts) section above.

<figure>
  <a href="../../assets/images/alerts/create-fg-alert-existing.png">
    <img src="../../assets/images/alerts/create-fg-alert-existing.png" alt="Create Feature Validation Alert Existing"/>
  </a>
  <figcaption>Create Feature Validation Alert for an Existing Feature Group</figcaption>
</figure>

From here you can also edit existing alerts, test the alerts or delete them. Click on the pen icon to edit the 
severity or the receivers of an alert. The play icon will let you send a test alert to the receivers of that alert.  

Feature group specific alerts override any _Global Alert_ in the project that will trigger for the same event type.

## Job Alerts

To create a job alert see [Job Alert](jobs.md#alerts).