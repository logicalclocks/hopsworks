# Manage Services
Hopsworks provides administrators with a view of the status/health of the cluster. 
This information is provided through the Services page.
You can find the Services page by clicking on your name, in the top right corner of the navigation bar, and choosing 
_Cluster Settings_ from the dropdown menu and going to the _Services_ tab.

<figure>
  <a  href="../../assets/images/admin/services/full.png">
    <img src="../../assets/images/admin/services/full.png" alt="services page" />
  </a>
  <figcaption>Services page</figcaption>
</figure>

This page give administrators an overview of which services are running on the cluster. 
It provides information about their status as reported by Kagent which, in turn, monitor the status of the different 
SystemD units.

Columns in the services table represent machines in your cluster. Each service running on a machine will have a status
_running_ (green), _stopped_ (gray), or _bad_ (red). 
Services are divided into groups, and you can search for a service by its name or group. You can also search for
machines by their host name.

<figure>
  <a  href="../../assets/images/admin/services/services.png">
    <img src="../../assets/images/admin/services/services.png" alt="services" />
  </a>
  <figcaption>Services</figcaption>
</figure>

After you find the correct service you will be able to **start**, **stop** or **restart** it, by clicking on its status.
<figure>
  <a  href="../../assets/images/admin/services/start.png">
    <img src="../../assets/images/admin/services/start.png" alt="start services" />
  </a>
  <figcaption>Start, Stop and Restart a services</figcaption>
</figure>

!!!Note

    Stopping some services like the web server (glassfish_domain1) is not recommended. If you stop it you will have to
    access the machine running the service and start it with ```systemctl start glassfish_domain1```. 