import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KuduUtils {
    private static final Double DEFAULT_DOUBLE = 12.345;

    public static void main(String[] args) throws InterruptedException, KuduException {
        KuduClient client = new KuduClient.KuduClientBuilder("******").build();

        List<ColumnSchema> columns = new ArrayList<ColumnSchema>(2);
        columns.add(new ColumnSchema.ColumnSchemaBuilder("key1", Type.STRING)
                .key(true)
                .build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("key2", Type.STRING)
                .build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("key3", Type.STRING)
                .build());
        columns.add(new ColumnSchema.ColumnSchemaBuilder("key4", Type.STRING)
                .build());
//        columns.add(new ColumnSchema.ColumnSchemaBuilder("key5", Type.STRING)
//                .build());
        List<String> rangeKeys = new ArrayList<>();
        rangeKeys.add("key1");
       create(client,"sp_test",columns,rangeKeys);

 //       delete(client,"sp_test");
//        Map<String, Object> valueMap = new HashMap<String,Object>();
//        valueMap.put("key1", "value14");
//        valueMap.put("key2", "value25");
//        valueMap.put("key3", "value35");
//        valueMap.put("key4", "value45");
//
//        upsert(client,client.openTable("sp_test"),valueMap,columns);

//        List<StructField> fields = Arrays.asList(
//                DataTypes.createStructField("key1", DataTypes.StringType, true),
//                DataTypes.createStructField("key2", DataTypes.StringType, true),
//                DataTypes.createStructField("key3", DataTypes.StringType, true),
//                DataTypes.createStructField("key4", DataTypes.StringType, true),
//                DataTypes.createStructField("key5", DataTypes.StringType, true));
//        StructType schema = DataTypes.createStructType(fields);
//        select(client,"sp_test",schema);
//        scanTableAndCheckResults(client, "sp_test", 50);
    }



    private static void scanTableAndCheckResults(KuduClient client, String tableName, int numRows)
            throws KuduException {
        KuduTable table = client.openTable(tableName);
        Schema schema = table.getSchema();

        // Scan with a predicate on the 'key' column, returning the 'value' and
        // "added" columns.
        List<String> projectColumns = new ArrayList<>(2);
        projectColumns.add("key");
        projectColumns.add("value");
        projectColumns.add("added");
        int lowerBound = 0;
        KuduPredicate lowerPred = KuduPredicate.newComparisonPredicate(schema.getColumn("key"),
                KuduPredicate.ComparisonOp.GREATER_EQUAL, lowerBound);
        int upperBound = numRows / 2;
        KuduPredicate upperPred = KuduPredicate.newComparisonPredicate(schema.getColumn("key"), KuduPredicate.ComparisonOp.LESS,
                upperBound);
        KuduScanner scanner = client.newScannerBuilder(table).setProjectedColumnNames(projectColumns)
                .addPredicate(lowerPred).addPredicate(upperPred).build();

        // Check the correct number of values and null values are returned, and
        // that the default value was set for the new column on each row.
        // Note: scanning a hash-partitioned table will not return results in
        // primary key order.
        int resultCount = 0;
        int nullCount = 0;
        while (scanner.hasMoreRows()) {
            RowResultIterator results = scanner.nextRows();
            while (results.hasNext()) {
                RowResult result = results.next();
                if (result.isNull("value")) {
                    nullCount++;
                }
                double added = result.getDouble("added");
                if (added != DEFAULT_DOUBLE) {
                    throw new RuntimeException("expected added=" + DEFAULT_DOUBLE + " but got added= " + added);
                }
                resultCount++;
            }
        }
        int expectedResultCount = upperBound - lowerBound;
        if (resultCount != expectedResultCount) {
            throw new RuntimeException(
                    "scan error: expected " + expectedResultCount + " results but got " + resultCount + " results");
        }
        int expectedNullCount = expectedResultCount / 2 + (numRows % 2 == 0 ? 1 : 0);
        if (nullCount != expectedNullCount) {
            throw new RuntimeException(
                    "scan error: expected " + expectedNullCount + " rows with value=null but found " + nullCount);
        }
        System.out.println("Scanned some rows and checked the results");
    }
    /**
     * 删除kudu表
     * @param client
     * @param tableName
     * @param columns
     */
//	public static void select(KuduClient client,String tableName,StructType schema){
//        try {
//        	SparkSession sparkSession = getSparkSession();
//
//            Dataset ds =  sparkSession.read().format("org.apache.kudu.spark.kudu").
//                    schema(schema).option("kudu.master","172.172.241.228:7051").option("kudu.table","sp_test").load();
//            ds.registerTempTable(tableName);
//            sparkSession.sql("select * from " + tableName).show(1000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 获取spark上下文路径
     * @return
     */
//	public static SparkSession getSparkSession(){
//        SparkConf conf = new SparkConf().setAppName("test")
//                .setMaster("local[*]")
//                .set("spark.driver.userClassPathFirst", "true");
//
//        conf.set("spark.sql.crossJoin.enabled", "true");
//        SparkContext sparkContext = new SparkContext(conf);
//        SparkSession sparkSession = SparkSession.builder().sparkContext(sparkContext).getOrCreate();
//        return sparkSession;
//    }

    /**
     * 删除kudu表
     * @param client
     * @param tableName

     */
    public static void delete(KuduClient client,String tableName){
        try {
            client.deleteTable(tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建kudu表
     * @param client
     * @param tableName
     * @param columns
     */
    public static void create(KuduClient client,String tableName,List<ColumnSchema> columns,List<String> rangeKeys){
        try {
            Schema schema = new Schema(columns);
            client.createTable(tableName, schema,
                    new CreateTableOptions().setRangePartitionColumns(rangeKeys));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * kudu新建数据
     * @param client
     * @param table
     */
    public static void insert(KuduClient client,KuduTable table){
        try{
            KuduSession session = client.newSession();
            System.out.println("-------start--------"+System.currentTimeMillis());
            for (int i = 30000; i < 31000; i++) {
                Insert insert = table.newInsert();
                PartialRow row = insert.getRow();
                row.addString(0, i+"");
                row.addString(1, "aaa");
                OperationResponse operationResponse =  session.apply(insert);

                System.out.print(operationResponse.getRowError());
            }
            System.out.println("-------end--------"+System.currentTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * kudu更新已有数据
     * @param client
     * @param table
     */
    public static void update(KuduClient client,KuduTable table){
        try {
            KuduSession session = client.newSession();
            Update update = table.newUpdate();
            PartialRow row = update.getRow();
            row.addString("key", 4+"");
            row.addString("value", "value " + 10);
            OperationResponse operationResponse =  session.apply(update);

            System.out.print(operationResponse.getRowError());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * kudu merge操作
     * @param client
     * @param table
     * @param valueMap
     * @param columns
     */
    public static void upsert(KuduClient client, KuduTable table, Map<String,Object> valueMap, List<ColumnSchema> columns){
        try {
            KuduSession session = client.newSession();
            Upsert upsert = table.newUpsert();
            PartialRow row = upsert.getRow();
            columns.forEach(column -> {
                System.out.println("key :" + column.getName() + "----value:" + valueMap.get(column.getName()) + " type:" + column.getType());

                getColumnValue(row,column.getName(),valueMap.get(column.getName()),column.getType());
            });
            OperationResponse operationResponse =  session.apply(upsert);

            System.out.println("kudu表upsert：" + operationResponse.getRowError());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 数据赋值
     * @param row
     * @param key
     * @param value
     * @param type
     */
    private static void getColumnValue(PartialRow row,String key,Object value,Type type){
        if(value == null){
            row.addString(key, null);
            return;
        }
        switch (type) {
            case UNIXTIME_MICROS:
                row.addString(key, value.toString());
                break;
            case STRING:
                row.addString(key, value.toString());
                break;
            case DOUBLE:
                try {
                    double result = Double.parseDouble(value.toString());
                    row.addDouble(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的double：" + value);
                    row.addDouble(key, 0.0);
                }
                break;
            case FLOAT:
                try {
                    Float result = Float.parseFloat(value.toString());
                    row.addFloat(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的float：" + value);
                    row.addFloat(key, 0.0f);
                }
                break;
            case INT64:
                try {
                    Long result = Long.parseLong(value.toString());
                    row.addLong(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的Long：" + value);
                    row.addLong(key, 0l);
                }
                break;
            case INT32:
                try {
                    Integer result = Integer.parseInt(value.toString());
                    row.addInt(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的Integer：" + value);
                    row.addInt(key, 0);
                }
                break;
            case INT16:
                try {
                    Short result = Short.parseShort(value.toString());
                    row.addShort(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的Short：" + value);
                    row.addShort(key, (short) 0);
                }
                break;
            case INT8:
                try {
                    Byte result = Byte.parseByte(value.toString());
                    row.addByte(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的byte：" + value);
                    row.addByte(key, (byte) 0);
                }
                break;
            case BOOL:
                try {
                    boolean result = Boolean.parseBoolean(value.toString());
                    row.addBoolean(key, result);
                } catch (NumberFormatException e) {
                    System.out.println("不是个合法的boolean：" + value);
                    row.addBoolean(key, false);
                }
                break;
            default:
                row.addString(key, value.toString());
        }
    }
}
