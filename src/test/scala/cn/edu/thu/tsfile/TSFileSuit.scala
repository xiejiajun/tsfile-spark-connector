package cn.edu.thu.tsfile

import java.io.File

import cn.edu.thu.tsfile.io.CreateTSFile
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.junit.Assert
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import cn.edu.thu.tsfile.qp.common.SQLConstant


class TSFileSuit extends FunSuite with BeforeAndAfterAll {

  private val resourcesFolder = "src/test/resources"
  private val tsfileFolder = "src/test/resources/tsfile"
  private val tsfilePath1 = "src/test/resources/tsfile/test1.tsfile"
  private val tsfilePath2 = "src/test/resources/tsfile/test2.tsfile"
  private val outputPath = "src/test/resources/output"
  private var spark: SparkSession = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val resources = new File(resourcesFolder)
    if (!resources.exists())
      resources.mkdirs()
    val tsfile_folder = new File(tsfileFolder)
    if (!tsfile_folder.exists())
      tsfile_folder.mkdirs()
    val output = new File(outputPath)
    if (output.exists())
      output.delete()
    new CreateTSFile().createTSFile1(tsfilePath1)
    new CreateTSFile().createTSFile2(tsfilePath2)
    spark = SparkSession
      .builder()
      .config("spark.master", "local")
      .appName("TSFile test")
      .getOrCreate()
  }

  override protected def afterAll(): Unit = {
    val out = new File(outputPath)
    if (out.exists())
      out.delete()
    try {
      spark.sparkContext.stop()
    } finally {
      super.afterAll()
    }
  }

  test("writer") {
    val df = spark.read.tsfile(tsfilePath1)
//    val df = spark.createDataFrame(
//      Seq(
//        ("root.car.d1", 8, 1, 9.8),
//        ("root.car.d1", 8, 2, 8.7),
//        ("root.car.d2", 7, 3, 5.5),
//        ("root.car.d2", 7, 4, 2.0))
//    ).toDF("delta_object", "timestamp", "sensor1", "sensor2")

    df.show()
    df.write.tsfile(outputPath)
  }

  test("tsfile_qp") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select s1,s2 from tsfile_table where delta_object = 'root.car.d1' and time <= 10 and (time > 5 or s1 > 10)")
    Assert.assertEquals(0, newDf.count())
  }

  test("testMultiFilesNoneExistDelta_object") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table where delta_object = 'd4'")
    Assert.assertEquals(0, newDf.count())
  }

  test("testMultiFilesWithFilterOr") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table where s1 < 2 or s2 > 60")
    Assert.assertEquals(4, newDf.count())
  }

  test("testMultiFilesWithFilterAnd") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table where s2 > 20 and s1 < 5")
    Assert.assertEquals(2, newDf.count())
  }

  test("testMultiFilesSelect*") {
    val df = spark.read.tsfile(tsfileFolder)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table")
    Assert.assertEquals(16, newDf.count())
  }

  test("testCount") {
    val df = spark.read.tsfile(tsfilePath1)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select count(*) from tsfile_table")
    Assert.assertEquals(8, newDf.head().apply(0).asInstanceOf[Long])
  }

  test("testSelect *") {
    val df = spark.read.tsfile(tsfilePath1)
    df.createOrReplaceTempView("tsfile_table")
    val newDf = spark.sql("select * from tsfile_table")
    val count = newDf.count()
    Assert.assertEquals(8, count)
  }

  test("testQueryData1") {
    val df = spark.read.tsfile(tsfilePath1)
    df.createOrReplaceTempView("tsfile_table")

    val newDf = spark.sql("select s1, s3 from tsfile_table where s1 > 4 and delta_object = 'root.car.d2'").cache()
    val count = newDf.count()
    Assert.assertEquals(4, count)
  }

  test("testQueryDataComplex2") {
    val df = spark.read.tsfile(tsfilePath1)
    df.createOrReplaceTempView("tsfile_table")

    val newDf = spark.sql("select * from tsfile_table where s1 <4 and delta_object = 'root.car.d1' or s1 > 5 and delta_object = 'root.car.d2'").cache()
    val count = newDf.count()
    Assert.assertEquals(6, count)
  }

  test("testQuerySchema") {
    val df = spark.read.format("cn.edu.thu.tsfile").load(tsfilePath1)

    val expected = StructType(Seq(
      StructField(SQLConstant.RESERVED_TIME, LongType, nullable = true),
      StructField(SQLConstant.RESERVED_DELTA_OBJECT, StringType, nullable = true),
      StructField("s3", FloatType, nullable = true),
      StructField("s4", DoubleType, nullable = true),
      StructField("s1", IntegerType, nullable = true),
      StructField("s2", LongType, nullable = true)
    ))
    Assert.assertEquals(expected, df.schema)
  }

//  test("testQuerySchema1") {
//    val df = spark.read.format("cn.edu.thu.tsfile").load("/testdata/test1.tsfile")
//    df.createOrReplaceTempView("tsfile")
//
//    println(spark.sql("select * from tsfile").schema)
//
//    spark.sql("select * from tsfile").show()
//  }

}