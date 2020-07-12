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

package org.jeasy.batch.tutorials.advanced.cbrd;

import org.jeasy.batch.core.filter.FileExtensionFilter;
import org.jeasy.batch.core.job.Job;
import org.jeasy.batch.core.job.JobBuilder;
import org.jeasy.batch.core.job.JobExecutor;
import org.jeasy.batch.core.reader.BlockingQueueRecordReader;
import org.jeasy.batch.core.reader.FileRecordReader;
import org.jeasy.batch.core.record.Record;
import org.jeasy.batch.extensions.integration.ContentBasedBlockingQueueRecordWriter;
import org.jeasy.batch.extensions.integration.Predicate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
* Main class to run the content based record dispatching tutorial.
 *
* @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
*/
public class Launcher {

    private static final int THREAD_POOL_SIZE = 3;
    private static final int QUEUE_TIMEOUT = 1000;

    public static void main(String[] args) {
        
        String path = args.length == 0 ? "." : args[0];
        Path directory = Paths.get(path);

        // Create work queues
        BlockingQueue<Record<Path>> csvQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Record<Path>> xmlQueue = new LinkedBlockingQueue<>();

        Map<Predicate<Path>, BlockingQueue<Record<Path>>> queueMap = new HashMap<>();
        queueMap.put(new CsvFilePredicate(), csvQueue);
        queueMap.put(new XmlFilePredicate(), xmlQueue);
        // Create a content based record writer to write records to work queues based on their content
        ContentBasedBlockingQueueRecordWriter<Path> contentBasedBlockingQueueRecordWriter =
                new ContentBasedBlockingQueueRecordWriter<>(queueMap);

        // Build a master job that will read files from the directory and dispatch them to worker jobs
        Job masterJob = new JobBuilder<Path, Path>()
                .named("master-job")
                .reader(new FileRecordReader(directory))
                .filter(new FileExtensionFilter(".log", ".tmp"))
                .writer(contentBasedBlockingQueueRecordWriter)
                .build();

        // Build jobs
        Job workerJob1 = buildWorkerJob("csv-worker-job", csvQueue);
        Job workerJob2 = buildWorkerJob("xml-worker-job", xmlQueue);

        // Create a Job executor with 3 worker threads
        JobExecutor jobExecutor = new JobExecutor(THREAD_POOL_SIZE);

        // Submit master and worker jobs to job executor
        jobExecutor.submitAll(masterJob, workerJob1, workerJob2);

        // Shutdown job executor
        jobExecutor.shutdown();

    }

    private static Job buildWorkerJob(String jobName, BlockingQueue<Record<Path>> workQueue) {
        return new JobBuilder<Path, Path>()
                .named(jobName)
                .reader(new BlockingQueueRecordReader<>(workQueue, QUEUE_TIMEOUT))
                .processor(new DummyFileProcessor())
                .build();
    }

}
