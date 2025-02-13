![logo](.img/bq-icon-5.png)

# bq-duckdb

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.bitquant-initiative/bq-duckdb?color=blue)](https://central.sonatype.com/artifact/io.github.bitquant-initiative/bq-duckdb) [![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/bitquant-initiative/bq-duckdb/.github%2Fworkflows%2Fbuild.yml?branch=main)](https://github.com/bitquant-initiative/bq-duckdb/actions?query=branch%3Amain)

DuckDB is a bit different than other RDBMS.  bq-duckdb does a few simple things to make it easy to work with.

Connection pools don't make sense in a DuckDB world.  In many cases a DuckDB in-memory database may be created and destroyed
in a fraction of a second.  


# Usage

Maven dependency is as follows:

```
<dependency>
    <groupId>io.github.bitquant-initiative</groupId>
    <artifactId>bq-duckdb</artifactId>
    <version>VERSION</version>
</dependency>
```

Use the version from the badge above.

## Create DuckDb Instance

### Create In-Memory

Create an in-memory DB:

```
var db = DuckDB.createInMemory()
```

This is functionally identical to:
```
var db = DriverManager.getConnection("jdbc:duckdb:");
```

You can then use this by calling:

```
var con = db.getConnection();
```

The returned connection is wrapped and the connection's `close()` method is turned into a no-op.
This allows existing code and patterns to be used that use close() to indicate that the connection
can be released.

```
con.close(); // no-op
```

To close the database, call:

```
db.close();
```

### Shared DuckDb In-Memory Instance

It can be handy to have a single global in-memory instance. In the following code, `db1` and `db2` are the same instance:
```
var db1 = DuckDb.getSharedInMemory();
var db2 = DuckDb.getSharedInMemory();
// db1 == db2
```

Additionally, their restpective connections are the same:
```
var con1 = db1.getConnection();
var con2 = db2.getConnection();
// con1 == con2
```

The library prevents you from closing the database:

```
db1.close(); // throws and exception
```


## Create DuckDb From a File

```
var db = DuckDb.create(new File("./mydb.db");
```

## Misc Methods

To find out if a table exists:

```
db.tableExists("mytable"); 
```

To get a list of tables:
```
List<String> tables = db.getTableNames();
```

To delete a row by DuckDb rowid:

```
db.deleteRow("order",312);
```

