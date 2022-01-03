# Python environment

Hopsworks comes out of the box with a Python environment for data engineering, machine learning and more general data science development.

There is one Python environment for each project. A library installed in the project's Python environment can be used in a Job or Jupyter notebook (Python or PySpark) run in the project.

The environment ensures compatibility between the CUDA version and the installed TensorFlow and PyTorch versions for applications using NVIDIA GPUs.

##Navigating to the service

Managing the project's Python environment is provided as a service on Hopsworks and can be found in the `Python libraries` page under the `Project settings` menu.

<p align="center">
  <figure>
    <img src="../../assets/images/python/navigate.gif" alt="Open the Python libraries page">
    <figcaption>Open the Python libraries page</figcaption>
  </figure>
</p>

When a project is created, the python 3.8 environment is automatically enabled. So no additional steps are required to start developing your machine learning application.

##Listing installed libraries

The list of installed libraries is displayed on the page. It is possible to filter based on the name and package manager to find a particular library.

<p align="center">
  <figure>
    <img src="../../assets/images/python/list.png" alt="Viewing installed libraries">
    <figcaption>Viewing installed libraries</figcaption>
  </figure>
</p>

##Installing libraries

Python packages can be installed from the following sources:

* PyPi, using pip package manager
* A conda channel, using conda package manager
* Packages saved in certain file formats, currently we support .whl or .egg
* A public or private git repository
* A requirements.txt file to install many libraries at the same time using pip
* An environment.yml file to install many libraries at the same time using conda and pip

###Install by name and version

Enter the name and optionally the desired version to install.

<p align="center">
  <figure>
    <img src="../../assets/images/python/install_name_version.gif" alt="Installing library by name and version">
    <figcaption>Installing library by name and version</figcaption>
  </figure>
</p>

###Search and install

Enter the search term and select a library and version to install.

<p align="center">
  <figure>
    <img src="../../assets/images/python/install_search.gif" alt="Installing library using search">
    <figcaption>Installing library using search</figcaption>
  </figure>
</p>

###Install from package file

Install a python package by uploading the corresponding package file and selecting it in the file browser.

Currently supported formats include:

* .whl
* .egg
* requirements.txt
* environment.yml

<p align="center">
  <figure>
    <img src="../../assets/images/python/install_dep.gif" alt="Installing library from file">
    <figcaption>Installing library from file</figcaption>
  </figure>
</p>

###Install from .git repository

To install from a git repository simply provide the repository URL. The URL you should provide is the same as you would enter on the command line using `pip install git+{repo_url}`.
In the case of a private git repository, also select whether it is a GitHub or GitLab repository and the preconfigured access token for the repository.

**Note**: If you are installing from a git repository which is not GitHub or GitLab simply supply the access token in the URL. Keep in mind that in this case the access token may be visible in logs for other users in the same project to see.

<p align="center">
  <figure>
    <img src="../../assets/images/python/install_git.gif" alt="Installing library from git repo">
    <figcaption>Installing library from git repo</figcaption>
  </figure>
</p>

##Uninstalling libraries

To uninstall a library, find the library in the list of installed libararies tab and click the trash icon to remove the library.

<p align="center">
  <figure>
    <img src="../../assets/images/python/uninstall.png" alt="Uninstall a library">
    <figcaption>Uninstall a library</figcaption>
  </figure>
</p>

##Debugging the environment

After each installation or uninstall of a library, the environment is analyzed to detect libraries that may not work properly. In order to do so we use the ``pip check`` tool, which is able to identify missing dependencies or if a dependency is installed with the incorrect version.
The alert will automatically show if such an issue was found.


<p align="center">
  <figure>
    <img src="../../assets/images/python/conflicts.png" alt="Conflicts detected in environment">
    <figcaption>Conflicts detected in environment</figcaption>
  </figure>
</p>

##Recreating environment

Sometimes it may be desirable to recreate the environment to start from the default project environment. In order to do that, first click `Remove env`.

<p align="center">
  <figure>
    <img src="../../assets/images/python/remove_env.png" alt="Remove environment">
    <figcaption>Remove environment</figcaption>
  </figure>
</p>

After removing the environment, simply recreate it by clicking `Create Environment`.

<p align="center">
  <figure>
    <img src="../../assets/images/python/create_env.png" alt="Create environment">
    <figcaption>Create environment</figcaption>
  </figure>
</p>

##Exporting an environment

An existing Anaconda environment can be exported as a yml file, clicking the `Export env` will download the `environment.yml` file in your browser.

<p align="center">
  <figure>
    <img src="../../assets/images/python/export_env.png" alt="Export environment">
    <figcaption>Export environment</figcaption>
  </figure>
</p>