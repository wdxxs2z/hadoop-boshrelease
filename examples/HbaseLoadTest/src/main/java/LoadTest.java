/**
 * Created by pivotal on 5/12/17.
 */

import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.apache.hadoop.hbase.client.ConnectionFactory.createConnection;

/**
 * Created by pivotal on 5/11/17.
 */
public class LoadTest {

    public static void main(String[] args) throws IOException {
        final int batchSize = 100, cycles = 10000;
        Connection conn = createConnection();

        try {
            // Instantiating table descriptor class
            TableName tableName = createTable(conn);

            Table table = conn.getTable(tableName);

            Put data = new Put("row-1".getBytes());
            data.addColumn("cf-1".getBytes(), "col-1".getBytes(), "value-1".getBytes());

            long start = System.currentTimeMillis();
            for (int i=0; i<cycles; i++){
                table.put(createBatch(batchSize));
            }
            long duration = System.currentTimeMillis() - start;
            System.out.printf("Duration (ms): %d\n", duration);
            System.out.printf("Cycles: %d. Batch Size: %d. Writes/s %.4f\n", cycles, batchSize, batchSize*cycles/(double)duration*1000);

            conn.getAdmin().disableTable(tableName);
            conn.getAdmin().deleteTable(tableName);

        } finally {
            conn.close();
        }
    }

    private static List<Put> createBatch(int size){
        Random rand = new Random();
        List<Put> result = new ArrayList<>();
        for (int i=0; i<size; i++){
            Put data = new Put(("row-" +i).getBytes());
            byte[] start = longToBytes(System.currentTimeMillis()-1000);
            byte[] end= longToBytes(System.currentTimeMillis());
            byte[] statusCode = longToBytes(i%10==0?500:200);

            data.addColumn("outer".getBytes(), "ms".getBytes(), end);
            data.addColumn("inner".getBytes(), "status_code".getBytes(), statusCode);
            data.addColumn("inner".getBytes(), "start".getBytes(), start);
            data.addColumn("inner".getBytes(), "end".getBytes(), end);
            data.addColumn("innerLog".getBytes(), "message".getBytes(), new BigInteger(130, rand).toString(132).getBytes());
        }
        return result;
    }

    private static byte[] longToBytes(long l){
        byte[] result = new byte[8];
        for(int i=7; i>= 0; i--){
            result[i] = (byte)(l&0xFF);
            l>>=8;
        }
        return result;
    }

    private static TableName createTable(Connection conn) throws IOException {
        Random rand = new Random();

        for (; ; ) {
            HTableDescriptor tableDescriptor = new
                    HTableDescriptor(TableName.valueOf("some-table-" + rand.nextLong()));

            // Adding column families to table descriptor
            tableDescriptor.addFamily(new HColumnDescriptor("outer"));
            tableDescriptor.addFamily(new HColumnDescriptor("inner"));

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
