package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset;


import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;

public class OptimizedPitFunction {
  final static SqlOperator optimizedPitFunction = new SqlFunction(
    "PIT",
    SqlKind.OTHER_FUNCTION,
    ReturnTypes.BOOLEAN,
    null,
    OperandTypes.STRING,
    SqlFunctionCategory.USER_DEFINED_FUNCTION
  );
}