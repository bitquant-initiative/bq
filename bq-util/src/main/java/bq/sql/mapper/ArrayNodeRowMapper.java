package bq.sql.mapper;

import bq.sql.Results;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import java.sql.SQLException;

public class ArrayNodeRowMapper extends JsonNodeRowMapper<ArrayNode> {

  public ArrayNodeRowMapper() {
    super(JsonNodeType.ARRAY);
  }

  @Override
  public ArrayNode map(Results rs) throws SQLException {
    return (ArrayNode) super.doMap(rs);
  }
}
