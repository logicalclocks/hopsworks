# Start with A Demo Project

When you log in to Hopsworks for the first time you will be presented with a landing page as shown in the figure bellow.
From the landing page you can either create a new project or start with a demo project.
The demo project is a place to start if you are new to Hopsworks.  
<p align="center">
  <figure>
    <a  href="../../../assets/images/project/landing-page.png">
      <img width="800px" src="../../../assets/images/project/landing-page.png" alt="OTP">
    </a>
    <figcaption>Landing page</figcaption>
  </figure>
</p>

You can create a demo project by clicking on the **Run a demo project** button on the landing page or the create 
project page.

<p align="center">
  <figure>
    <a  href="../../../assets/images/project/demo-fs.png">
      <img width="800px" src="../../../assets/images/project/demo-fs.png" alt="OTP">
    </a>
    <figcaption>Demo project</figcaption>
  </figure>
</p>

After creating a demo project you can go in to the project by clicking on **Open project**. The demo project will 
create a job that in turn will create sample feature groups and training dataset.

If you go to **Jobs** on the side menu you will find the job named **featurestore_tour_job** as shown in the figure 
bellow.  

<p align="center">
  <figure>
    <a  href="../../../assets/images/project/demo-job.png">
      <img width="800px" src="../../../assets/images/project/demo-job.png" alt="OTP">
    </a>
    <figcaption>Demo job</figcaption>
  </figure>
</p>

Wait until the job succeeds.

<p align="center">
  <figure>
    <a  href="../../../assets/images/project/tour-success.png">
      <img width="800px" src="../../../assets/images/project/tour-success.png" alt="OTP">
    </a>
    <figcaption>Job succeeded</figcaption>
  </figure>
</p>

If the job succeeds you can go to **Feature Groups** on the side menu to inspect the created feature groups. The 
figure bellow shows some sample feature groups created by the demo job.
<p align="center">
  <figure>
    <a  href="../../../assets/images/project/tour-fs.png">
      <img width="800px" src="../../../assets/images/project/tour-fs.png" alt="OTP">
    </a>
    <figcaption>Created feature groups</figcaption>
  </figure>
</p>

Similarly, you can go to the **Training dataset** on the side menu to inspect the created training dataset.   
<p align="center">
  <figure>
    <a  href="../../../assets/images/project/tour-td.png">
      <img width="800px" src="../../../assets/images/project/tour-td.png" alt="OTP">
    </a>
    <figcaption>Created training dataset</figcaption>
  </figure>
</p>

To learn how to create your own project go to [Create a New Project](./createProject.md)