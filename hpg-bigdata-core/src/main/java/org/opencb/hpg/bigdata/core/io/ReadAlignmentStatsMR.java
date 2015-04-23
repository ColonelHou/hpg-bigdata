package org.opencb.hpg.bigdata.core.io;

import java.io.IOException;

import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.ga4gh.models.ReadAlignment;
import org.opencb.hpg.bigdata.core.stats.ReadAlignmentStatsWritable;
import org.opencb.hpg.bigdata.core.stats.ReadStatsWritable;

public class ReadAlignmentStatsMR {

	public static class ReadAlignmentStatsMapper extends Mapper<AvroKey<ReadAlignment>, NullWritable, LongWritable, ReadAlignmentStatsWritable> {

		@Override
		public void map(AvroKey<ReadAlignment> key, NullWritable value, Context context) throws IOException, InterruptedException {
			ReadAlignmentStatsWritable stats = new ReadAlignmentStatsWritable();
			stats.updateByReadAlignment(key.datum());
			context.write(new LongWritable(1), stats);
		}
	}

	public static class ReadAlignmentStatsReducer extends Reducer<LongWritable, ReadAlignmentStatsWritable, Text, NullWritable> {

		public void reduce(LongWritable key, Iterable<ReadAlignmentStatsWritable> values, Context context) throws IOException, InterruptedException {
			ReadAlignmentStatsWritable stats = new ReadAlignmentStatsWritable();
			for (ReadAlignmentStatsWritable value : values) {
				stats.update(value);
			}
			context.write(new Text(stats.toJSON()), NullWritable.get());
		}
	}

	public static int run(String input, String output) throws Exception {
		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf, "ReadAlignmentStatsMR");		
		job.setJarByClass(ReadAlignmentStatsMR.class);

		// input
		AvroJob.setInputKeySchema(job, ReadAlignment.getClassSchema());
		FileInputFormat.setInputPaths(job, new Path(input));
		job.setInputFormatClass(AvroKeyInputFormat.class);
				
		// output
		FileOutputFormat.setOutputPath(job, new Path(output));
		job.setOutputKeyClass(ReadStatsWritable.class);
		job.setOutputValueClass(NullWritable.class);
		
		// mapper
		job.setMapperClass(ReadAlignmentStatsMapper.class);
		job.setMapOutputKeyClass(LongWritable.class);
		job.setMapOutputValueClass(ReadAlignmentStatsWritable.class);
		
		// reducer
		job.setReducerClass(ReadAlignmentStatsReducer.class);
		job.setNumReduceTasks(1);

		return (job.waitForCompletion(true) ? 0 : 1);
	}
}
