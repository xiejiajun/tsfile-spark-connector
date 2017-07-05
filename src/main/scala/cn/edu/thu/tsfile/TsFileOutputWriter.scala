package cn.edu.thu.tsfile

import cn.edu.thu.tsfile.io.TsFileOutputFormat
import cn.edu.thu.tsfile.timeseries.write.record.TSRecord
import org.apache.hadoop.io.NullWritable
import org.apache.hadoop.mapreduce.{RecordWriter, TaskAttemptContext}
import org.apache.spark.sql.Row
import org.apache.spark.sql.execution.datasources.OutputWriter
import org.apache.spark.sql.types._

private[tsfile] class TsFileOutputWriter(
                                          pathStr: String,
                                          dataSchema: StructType,
                                          options: Map[String, String],
                                          context: TaskAttemptContext) extends OutputWriter{

  private val recordWriter: RecordWriter[NullWritable, TSRecord] = {
    val fileSchema = Converter.toTsFileSchema(dataSchema, options)
    new TsFileOutputFormat(fileSchema).getRecordWriter(context)
  }

  override def write(row: Row): Unit = {
    if( row != null) {
      val tsRecord = Converter.toTsRecord(row)
      recordWriter.write(NullWritable.get(), tsRecord)
    }
  }

  override def close(): Unit = {
    recordWriter.close(context)
  }
}