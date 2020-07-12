/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package org.jeasy.batch.tutorials.advanced.jms;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.jms.Message;

import org.jeasy.batch.core.job.Job;
import org.jeasy.batch.core.job.JobBuilder;
import org.jeasy.batch.core.job.JobExecutor;
import org.jeasy.batch.core.job.JobReport;
import org.jeasy.batch.flatfile.FlatFileRecordReader;
import org.jeasy.batch.jms.JmsRecordWriter;

/**
* Main class to run the JMS message producer job. It reads tweets
 * from a flat file and sends them to a JMS queue.
 *
* @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
*/
public class JmsProducerJobLauncher {

    public static void main(String[] args) throws Exception {

        Path dataSource = Paths.get(args.length != 0 ? args[0] : "easy-batch-tutorials/src/main/resources/data/tweets.csv");

        // Build a batch job
        Job job = new JobBuilder<String, Message>()
                .reader(new FlatFileRecordReader(dataSource))
                .processor(new JmsRecordTransformer())
                .writer(new JmsRecordWriter<>(JMSUtil.getQueueConnectionFactory(), JMSUtil.getQueue()))
                .batchSize(1)
                .build();

        // Execute the job and get report
        JobExecutor jobExecutor = new JobExecutor();
        JobReport report = jobExecutor.execute(job);
        jobExecutor.shutdown();

        System.out.println(report);

    }

}
