package bq.sql.mapper;

import bq.sql.Results;
import bq.sql.RowMapper;
import bq.util.DateNumberPoint;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.sql.SQLException;

public class Mappers {

  public static RowMapper<ObjectNode> jsonObjectMapper() {
    return new ObjectNodeRowMapper();
  }

  public static RowMapper<ArrayNode> jsonArrayMapper() {
    return new ArrayNodeRowMapper();
  }

  public static RowMapper<DateNumberPoint> dateNumberPointMapper() {

    return dateNumberPointMapper(1, 2);
  }

  public static RowMapper<DateNumberPoint> dateNumberPointMapper(String dateCol, String numberCol) {

    bq.sql.RowMapper<DateNumberPoint> mapper =
        new RowMapper<>() {

          @Override
          public DateNumberPoint map(Results rs) throws SQLException {
            DateNumberPoint p =
                new DateNumberPoint(
                    rs.getLocalDate(dateCol).get(), rs.getDouble(numberCol).orElse(null));
            return p;
          }
        };
    return mapper;
  }

  public static RowMapper<DateNumberPoint> dateNumberPointMapper(int dateCol, int numberCol) {

    bq.sql.RowMapper<DateNumberPoint> mapper =
        new RowMapper<>() {

          @Override
          public DateNumberPoint map(Results rs) throws SQLException {
            DateNumberPoint p =
                new DateNumberPoint(
                    rs.getLocalDate(dateCol).get(), rs.getDouble(numberCol).orElse(null));
            return p;
          }
        };
    return mapper;
  }
}
