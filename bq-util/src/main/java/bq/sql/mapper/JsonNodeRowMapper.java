package bq.sql.mapper;

import bq.sql.Results;
import bq.sql.RowMapper;
import bq.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public abstract class JsonNodeRowMapper<T> implements RowMapper<T> {

  ResultSetMetaData md;

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  JsonNodeType targetType = JsonNodeType.OBJECT;

  ArrayNode arrayNodeTarget;
  ObjectNode objectNodeTarget;

  public JsonNodeRowMapper(JsonNodeType type) {
    Preconditions.checkArgument(type == JsonNodeType.OBJECT || type == JsonNodeType.ARRAY);
    this.targetType = type;
  }

  public JsonNode doMap(Results rs) throws SQLException {

    if (md == null) {
      md = rs.getResultSet().getMetaData();
    }
    Preconditions.checkState(
        md == rs.getResultSet().getMetaData(), "JsonRowMapper cannot be re-used");

    if (targetType == JsonNodeType.OBJECT) {
      objectNodeTarget = Json.createObjectNode();
    } else if (targetType == JsonNodeType.ARRAY) {
      arrayNodeTarget = Json.createArrayNode();
    }

    for (int i = 1; i <= md.getColumnCount(); i++) {
      String name = md.getColumnName(i);
      int type = md.getColumnType(i);

      switch (type) {
        case (Types.FLOAT):
        case (Types.DOUBLE):
        case (Types.DECIMAL):
          Optional<Double> d = rs.getDouble(name);
          setDouble(i, name, d.orElse(null));
          break;
        case (Types.BIGINT):
        case (Types.INTEGER):
          setLong(i, name, rs.getLong(name).orElse(null));

          break;
        case (Types.BOOLEAN):
          setBoolean(i, name, rs.getBoolean(name).orElse(null));

          break;
        case (Types.TIME):
        case (Types.TIME_WITH_TIMEZONE):
        case (Types.TIMESTAMP):
        case (Types.TIMESTAMP_WITH_TIMEZONE):
        case (Types.DATE):
          setString(i, name, rs.getString(name).orElse(null));
          break;
        case (Types.VARCHAR):
        case (Types.NVARCHAR):
          setString(i, name, rs.getString(name).orElse(null));
          break;
        default:
          logger.atWarning().atMostEvery(5, TimeUnit.SECONDS).log(
              "no type conversion for name=%s type=%s(%s) (relying on string)",
              name, type, md.getColumnTypeName(i));
          setString(i, name, rs.getString(name).orElse(null));
      }
    }

    if (objectNodeTarget != null) {
      return objectNodeTarget;
    }
    return arrayNodeTarget;
  }

  void setLong(int col, String name, Long val) {
    if (arrayNodeTarget != null) {
      arrayNodeTarget.add(val);
    } else {
      objectNodeTarget.put(name, val);
    }
  }

  void setDouble(int col, String name, Double val) {
    if (arrayNodeTarget != null) {
      arrayNodeTarget.add(val);
    } else {
      objectNodeTarget.put(name, val);
    }
  }

  void setBoolean(int col, String name, Boolean val) {
    if (arrayNodeTarget != null) {
      arrayNodeTarget.add(val);
    } else {
      objectNodeTarget.put(name, val);
    }
  }

  void setString(int col, String name, String val) {
    if (arrayNodeTarget != null) {
      arrayNodeTarget.add(val);
    } else {
      objectNodeTarget.put(name, val);
    }
  }
}
