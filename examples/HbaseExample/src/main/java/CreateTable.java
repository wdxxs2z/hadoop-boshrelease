import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import static org.apache.hadoop.hbase.client.ConnectionFactory.createConnection;

/**
 * Created by pivotal on 5/11/17.
 */
public class CreateTable {

    public static void main(String[] args) throws IOException {
        Connection conn = createConnection();

        try {
            // Instantiating table descriptor class
            TableName tableName = createTable(conn);

            Table table = conn.getTable(tableName);

            Put data = new Put("row-1".getBytes());
            data.addColumn("cf-1".getBytes(), "col-1".getBytes(), "value-1".getBytes());
            table.put(data);

            Scan scan = new Scan("row-1".getBytes());
            table.getScanner(scan).forEach(row->{
                Cell cell = row.getColumnLatestCell("cf-1".getBytes(), "col-1".getBytes());
                String cellValue = new String(CellUtil.cloneValue(cell));
                if(!cellValue.equals("value-1")){
                    throw new RuntimeException("expected '" + cellValue + "' to equal 'value-1'");
                }
            });

            conn.getAdmin().disableTable(tableName);
            conn.getAdmin().deleteTable(tableName);

        } finally {
            conn.close();
        }
    }

    private static TableName createTable(Connection conn) throws IOException {
        Random rand = new Random();

        for(;;) {
            HTableDescriptor tableDescriptor = new
                    HTableDescriptor(TableName.valueOf("some-table-" + rand.nextLong()));

            // Adding column families to table descriptor
            tableDescriptor.addFamily(new HColumnDescriptor("cf-1"));

            // Execute the table through admin
            TableName[] tables = conn.getAdmin().listTableNames();
            if (Arrays.stream(tables)
                    .filter(tb -> tb.getNameAsString().equals(tableDescriptor.getNameAsString()))
                    .count() > 0) {
                continue;
            }

            conn.getAdmin().createTable(tableDescriptor);
            System.out.println("Table created: " + tableDescriptor.getNameAsString());
            return tableDescriptor.getTableName();
        }
    }
}
