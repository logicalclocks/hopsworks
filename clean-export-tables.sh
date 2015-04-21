#!/bin/bash

perl -pi -e "s/.*DEFINER=\`\w.*//g" kthfsSchema.sql 
perl -pi -e "s/InnoDB/NDBCLUSTER/g" kthfsSchema.sql 
perl -pi -e "s/AUTO_INCREMENT=[0-9]*\b//g" kthfsSchema.sql 

cp kthfsSchema.sql ../hopshub-chef/templates/default/tables.sql.erb
