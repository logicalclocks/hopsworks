# Create a New Project
You can create a project by clicking on the **Create new project** button in the Projects list page. 
This will pop-up a dialog, in which you enter the project name and an optional description. You can also select an 
initial set of members for the project, who will be 
given the role of Data Scientist in the project. Member roles can later be updated in the Project settings 
by the project owner or a member with the data owner role. A valid project name can only contain characters 
a-z, A-Z, 0-9 and special characters ‘_’ and ‘.’ but not ‘__’ (double underscore). 
There are also reserved words that are not allowed in project names. A complete list of reserved words can 
be found in section Project name reserved words.

  <figure>
    <a  href="../../../assets/images/project/createProject.png">
      <img src="../../../assets/images/project/createProject.png" alt="Create project">
    </a>
    <figcaption>Create project</figcaption>
  </figure>

As soon as you have created a new project and click on **Open project** in the project list, you will see the project 
main page as illustrated in the figure Project overview.

  <figure>
    <a  href="../../../assets/images/project/projectList.png">
      <img src="../../../assets/images/project/projectList.png" alt="Project list">
    </a>
    <figcaption>Project list</figcaption>
  </figure>

  <figure>
    <a  href="../../../assets/images/project/projectOverview.png">
      <img src="../../../assets/images/project/projectOverview.png" alt="Project overview">
    </a>
    <figcaption>Project overview</figcaption>
  </figure>

On the left-hand side of the project overview page is the Project Menu. On the top we have the feature store section 
with feature groups, training datasets and storage connectors. In the middle we have the compute section 
containing Jupyter and Jobs. Finally, on the bottom of the menu we have the Configuration section with settings for the 
project. 
From Settings, you can configure python libraries, alerts and integrations to other services. From the general 
configuration you can add members, share feature store with another project and delete the project.

  <figure>
    <a  href="../../../assets/images/project/projectSettings.png">
      <img src="../../../assets/images/project/projectSettings.png" alt="Project settings">
    </a>
    <figcaption>Project Settings</figcaption>
  </figure>

On the top navigation bar next to the Hopsworks logo we find the project name. By clicking on the project name you 
can go to other projects or back to the projects list page.

!!! note

    Project name reserved words:

    PROJECTS, HOPS-SYSTEM, HOPSWORKS, INFORMATION_SCHEMA, AIRFLOW, GLASSFISH_TIMERS, GRAFANA, HOPS, METASTORE, MYSQL, 
    NDBINFO, PERFORMANCE_SCHEMA, SQOOP, SYS, GLASSFISH_TIMERS, GRAFANA, HOPS, METASTORE, MYSQL, NDBINFO, 
    PERFORMANCE_SCHEMA, SQOOP, SYS, BIGINT, BINARY, BOOLEAN, BOTH, BY, CASE, CAST, CHAR, COLUMN, CONF, CREATE, CROSS, 
    CUBE, CURRENT, CURRENT_DATE, CURRENT_TIMESTAMP, CURSOR, DATABASE, DATE, DECIMAL, DELETE, DESCRIBE, DISTINCT, DOUBLE,
    DROP, ELSE, END, EXCHANGE, EXISTS, EXTENDED, EXTERNAL, FALSE, FETCH, FLOAT, FOLLOWING, FOR, FROM, FULL, FUNCTION, 
    GRANT, GROUP, GROUPING, HAVING, IF, IMPORT, IN, INNER, INSERT, INT, INTERSECT, INTERVAL, INTO, IS, JOIN, LATERAL, 
    LEFT, LESS, LIKE, LOCAL, MACRO, MAP, MORE, NONE, NOT, NULL, OF, ON, OR, ORDER, OUT, OUTER, OVER, PARTIALSCAN, 
    PARTITION, PERCENT, PRECEDING, PRESERVE, PROCEDURE, RANGE, READS, REDUCE, REVOKE, RIGHT, ROLLUP, ROW, ROWS, SELECT, 
    SET, SMALLINT, TABLE, TABLESAMPLE, THEN, TIMESTAMP, TO, TRANSFORM, TRIGGER, TRUE, TRUNCATE, UNBOUNDED, UNION, 
    UNIQUEJOIN, UPDATE, USER, USING, UTC_TMESTAMP, VALUES, VARCHAR, WHEN, WHERE, WINDOW, WITH, COMMIT, ONLY, REGEXP, 
    RLIKE, ROLLBACK, START, CACHE, CONSTRAINT, FOREIGN, PRIMARY, REFERENCES, DAYOFWEEK, EXTRACT, FLOOR, INTEGER, 
    PRECISION, VIEWS, TIME, NUMERIC, SYNC, BASE, PYTHON37, PYTHON38, FILEBEAT.

    And any word containing _FEATURESTORE.