<p align="center">
    <a href="https://hopsworks.ai">
        <img src="https://uploads-ssl.webflow.com/5f6353590bb01cacbcecfbac/6202a13e7cafec5553703f6b_logo.svg" width="55%" >
    </a>
</p>
<br />

<p align="center">
    <a href="https://hopsworks.ai" alt="hopsworks.ai">
        <img src="https://img.shields.io/badge/hopsworks-ai-brightgreen" /></a>
     <a href="https://app.hopsworks.ai" alt="app">
        <img src="https://img.shields.io/badge/Hopsworks-app-green" /></a>    
    <a href="https://docs.hopsworks.ai" alt="docs.hopsworks.ai">
        <img src="https://img.shields.io/badge/hopsworks-docs-orange" /></a>
    <a href="https://community.hopsworks.ai" alt="community.hopsworks.ai">
        <img src="https://img.shields.io/badge/hopsworks-community-blueviolet" /></a>
    <a href="https://twitter.com/hopsworks" alt="Hopsworks Twitter">
        <img src="https://img.shields.io/badge/hopsworks-twitter-blue" /></a>
    <a href="https://bit.ly/publichopsworks" alt="Hopsworks Slack">
        <img src="https://img.shields.io/static/v1?label=Hopsworks&message=Slack&color=36C5F0" /></a>
</p>

<a name="what"></a>
# What is Hopsworks?

Hopsworks is a data platform for ML with a **Python-centric Feature Store** and MLOps capabilities. Hopsworks is a modular platform. You can use it as a standalone Feature Store, you can use it to manage, govern, and serve your models, and you can even use it to develop and operate feature pipelines and training pipelines. Hopsworks brings collaboration for ML teams, providing a secure, governed platform for developing, managing, and sharing ML assets - features, models, training data, batch scoring data, logs, and more.
<br />
<p align="center" style="background-color:white; border-radius:4px;">
<img src="https://uploads-ssl.webflow.com/5f6353590bb01cacbcecfbac/62f21c38bc47b2d313fbf76d_Marchitecture%20-%20readme.svg" width="90%">
</p>
<br />

<a name="quick"></a>
# 🚀 Quickstart

## **APP - Serverless (beta)**
### → **Go to [app.hopsworks.ai](https://app.hopsworks.ai)**
Hopsworks is available as a serverless app, simply head to [app.hopsworks.ai](https://app.hopsworks.ai) and register with your **Gmail** or **Github** accounts. You will then be able to run a tutorial or access Hopsworks directly and try yourself. This is the prefered way to first experience the platform before diving into more advanced uses and installation requirements. 

## **Azure, AWS & GCP**
[Managed Hopsworks](https://managed.hopsworks.ai) is our platform for running Hopsworks and the Feature Store in the cloud and integrates directly with the customer AWS/Azure/GCP environment. It also integrates seamlessly with third party platforms such as Databricks, SageMaker and KubeFlow.

If you wish to run Hopsworks on your Azure, AWS or GCP environement, follow one of the following guides in our documentation:
- [AWS Guide](https://docs.hopsworks.ai/3.0/setup_installation/aws/getting_started/#step-1-connecting-your-aws-account)
- [Azure Guide](https://docs.hopsworks.ai/3.0/setup_installation/azure/getting_started/#step-1-connecting-your-azure-account)
- [GCP Guide](https://docs.hopsworks.ai/3.0/setup_installation/gcp/getting_started/#step-1-connecting-your-gcp-account)

## **Installer - On-premise**
### → **Follow the [installation instructions](https://docs.hopsworks.ai/3.0/setup_installation/on_prem/hopsworks_installer/).**
The hopsworks-installer.sh script downloads, configures, and installs Hopsworks. It is typically run interactively, prompting the user about details of what is installed and where. It can also be run non-interactively (no user prompts) using the '-ni' switch.

### **Requirements**
You need at least one server or virtual machine on which Hopsworks will be installed with at least the following specification:
- Centos/RHEL 7.x or Ubuntu 18.04;
- at least 32GB RAM,
- at least 8 CPUs,
- 100 GB of free hard-disk space,
- outside Internet access (if this server is air-gapped, contact us for support),
- a UNIX user account with sudo privileges.
<br />

<a name="docs"></a>
# 🎓 Documentation and API
### **Documentation**
[Hopsworks documentation](https://docs.hopsworks.ai) includes user guides, feature store documentation and an administration guide. We also include concepts to help user navigates the abstractions and logics of the feature stores and MLOps in general:
- **Feature Store:** [https://docs.hopsworks.ai/3.0/concepts/fs/](https://docs.hopsworks.ai/3.0/concepts/fs/)
- **Projects:** [https://docs.hopsworks.ai/3.0/concepts/projects/governance/](https://docs.hopsworks.ai/3.0/concepts/projects/governance/)
- **MLOps:** [https://docs.hopsworks.ai/3.0/concepts/mlops/prediction_services/](https://docs.hopsworks.ai/3.0/concepts/mlops/prediction_services/)

### **APIs**
Hopsworks API documentation is divided in 3 categories; Hopsworks API covers project level APIs, Feature Store API covers covers feature groups, feature views and connectors, and finally MLOps API covers Model Registry, serving and deployment. 
- **Hopsworks API** - [https://docs.hopsworks.ai/hopsworks-api/3.0.1/generated/api/connection/](https://docs.hopsworks.ai/hopsworks-api/3.0.1/generated/api/connection/)
- **Feature Store API** - [https://docs.hopsworks.ai/feature-store-api/3.0.0/generated/api/connection_api/](https://docs.hopsworks.ai/feature-store-api/3.0.0/generated/api/connection_api/)
- **MLOps API** - [https://docs.hopsworks.ai/machine-learning-api/3.0.0/generated/connection_api/](https://docs.hopsworks.ai/machine-learning-api/3.0.0/generated/connection_api/)

### **Tutorials**
Most of the tutorials require you to have at least an account on [app.hopsworks.ai](https://app.hopsworks.ai). You can explore the dedicated [https://github.com/logicalclocks/hopsworks-tutorials](https://github.com/logicalclocks/hopsworks-tutorials) repository containing our tutorials or jump directly in one of the existing use cases:
- Fraud (batch): [https://github.com/logicalclocks/hopsworks-tutorials/tree/master/fraud_batch](https://github.com/logicalclocks/hopsworks-tutorials/tree/master/fraud_batch)
- Fraud (online): [https://github.com/logicalclocks/hopsworks-tutorials/tree/master/fraud_online](https://github.com/logicalclocks/hopsworks-tutorials/tree/master/fraud_online)
- Churn prediction [https://github.com/logicalclocks/hopsworks-tutorials/tree/master/churn](https://github.com/logicalclocks/hopsworks-tutorials/tree/master/churn)
<br />

<a name="features"></a>
# 📦 Main Features

### **Project-based Multi-Tenancy and Team Collaboration**
Hopsworks provides projects as a secure sandbox in which teams can collaborate and share ML assets. Hopsworks' unique multi-tenant project model even enables sensitive data to be stored in a shared cluster, while still providing fine-grained sharing capabilities for ML assets across project boundaries. Projects can be used to structure teams so that they have end-to-end responsibility from raw data to managed features and models. Projects can also be used to create development, staging, and production environments for data teams. All ML assets support versioning, lineage, and provenance provide all Hopsworks users with a complete view of the MLOps life cycle, from feature engineering through model serving.

### **Development and Operations**
Hopsworks provides development tools for Data Science, including conda environments for Python, Jupyter notebooks, jobs, or even notebooks as jobs. You can build production pipelines with the bundled Airflow, and even run ML training pipelines with GPUs in notebooks on Airflow. You can train models on as many GPUs as are installed in a Hopsworks cluster and easily share them among users. You can also run Spark, Spark Streaming, or Flink programs on Hopsworks, with support for elastic workers in the cloud (add/remove workers dynamically).

### **Available on any Platform**
Hopsworks is available as a both managed platform in the cloud on AWS, Azure, and GCP, and can be installed on any Linux-based virtual machines (Ubuntu/Redhat compatible), even in air-gapped data centers. Hopsworks is also available as a serverless platform that manages and serves both your features and models.
<br />

<a name="community"></a>
# 🧑‍🤝‍🧑 Community

### **Contribute**
We are building the most complete and modular ML platform available in the market, and we count on your support to continuously improve Hopsworks. Feel free to give us suggestions, [report bugs](https://github.com/logicalclocks/hopsworks/issues) and [add features to our library](https://github.com/logicalclocks/feature-store-api) anytime.

### **Join the community**
- Ask questions and give us feedback in the [Hopsworks Community](https://community.hopsworks.ai/)
- Join our Public [Slack Channel](https://join.slack.com/t/public-hopsworks/shared_invite/zt-1e9czaq2o-nxNFcSMFN0p5_x4KTBpuUA)
- Follow us on [Twitter](https://twitter.com/hopsworks)
- Check out all our latest [product releases](https://github.com/logicalclocks/hopsworks/releases)

### **Open-Source**
Hopsworks is available under the **AGPL-V3 license**. In plain English this means that you are free to use Hopsworks and even build paid services on it, but if you modify the source code, you should also release back your changes and any systems built around it as AGPL-V3.
