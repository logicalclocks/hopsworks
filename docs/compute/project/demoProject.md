# Start with A Demo Project

When you log in to Hopsworks for the first time you will be presented with a landing page as shown in the figure below.
From the landing page you can either create a new project or start with a demo project.
The demo project is a good place to start if you are new to Hopsworks.
  <figure>
    <a  href="../../../assets/images/project/landing-page.png">
      <img alt="landing page" src="../../../assets/images/project/landing-page.png">
    </a>
    <figcaption>Landing page</figcaption>
  </figure>

You can create a demo project by clicking on the **Run a demo project** button on the landing page or the create 
project page.

  <figure>
    <a  href="../../../assets/images/project/demo-fs.png">
      <img src="../../../assets/images/project/demo-fs.png" alt="Demo feature store">
    </a>
    <figcaption>Demo project</figcaption>
  </figure>

After creating a demo project you can go in to the project by clicking on **Open project**. The demo project will 
create a job that in turn will create sample feature groups and training datasets.

If you go to **Jobs** on the side menu you will find the job named **featurestore_tour_job** as shown in the figure 
below.  

  <figure>
    <a  href="../../../assets/images/project/demo-job.png">
      <img src="../../../assets/images/project/demo-job.png" alt="Demo job">
    </a>
    <figcaption>Demo job</figcaption>
  </figure>

Wait until the job succeeds.

  <figure>
    <a  href="../../../assets/images/project/tour-success.png">
      <img src="../../../assets/images/project/tour-success.png" alt="Demo job success">
    </a>
    <figcaption>Job succeeded</figcaption>
  </figure>

Once the job has succeeded you can go to **Feature Groups** on the side menu to inspect the created feature groups. 
The figure below shows some sample feature groups created by the demo job.
  <figure>
    <a  href="../../../assets/images/project/tour-fs.png">
      <img src="../../../assets/images/project/tour-fs.png" alt="Demo project feature groups">
    </a>
    <figcaption>Created feature groups</figcaption>
  </figure>

Similarly, you can go to the **Training dataset** on the side menu to inspect the created training dataset.
  <figure>
    <a  href="../../../assets/images/project/tour-td.png">
      <img src="../../../assets/images/project/tour-td.png" alt="Demo project training datasets">
    </a>
    <figcaption>Created training dataset</figcaption>
  </figure>

If you want to look at or edit the job that created the feature groups and training datasets go to **Jupyter** on the 
side menu.

To learn how to create your own project go to [Create a New Project](./createProject.md)