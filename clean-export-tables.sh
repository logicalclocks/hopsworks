#!/bin/bash

perl -pi -e "s/.*DEFINER=\`\w.*//g" kthfsSchema.sql 
perl -pi -e "s/InnoDB/NDBCLUSTER/g" kthfsSchema.sql 

cp kthfsSchema.sql ../hopshub-chef/templates/default/tables.sql.erb
