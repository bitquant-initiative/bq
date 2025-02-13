package bq.sql.mapper;

import bq.sql.Results;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.SQLException;

public class ObjectNodeRowMapper extends JsonNodeRowMapper<ObjectNode> {

  public ObjectNodeRowMapper() {
    super(JsonNodeType.OBJECT);
  }

  @Override
  public ObjectNode map(Results rs) throws SQLException {
    return (ObjectNode) super.doMap(rs);
  }
}
