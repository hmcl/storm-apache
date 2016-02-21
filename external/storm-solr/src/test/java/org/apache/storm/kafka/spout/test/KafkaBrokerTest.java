/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.storm.kafka.spout.test;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple KafkaBrokerTest.
 */
public class KafkaBrokerTest {
    @Test
    public void test_spoutFunctionality_expectedBehavior() throws Exception {
    }

    public static void main(String[] args) {
        new KafkaBrokerTest().main();
    }

    @Test
    public void testIdx() throws Exception {
        List<Integer> li = new ArrayList<>();
        li.add(0,0);
        li.add(1,1);
        li.add(0,2);
        Integer val = li.get(li.size() - 1);
        System.out.println("val = " + val);
    }

    @Test
    public void testTreeSetConcurrent() throws Exception{
        TreeSet<Integer> tsi = new TreeSet<>();
        for (int i = 0; i < 10000; i++) {
            tsi.add(i);
        }

        Thread t1 = new Thread(new TreeSetPrint(tsi));
        Thread t2 = new Thread(new TreeSetAdd(tsi));
        t1.start();
        t2.start();
    }

    private class TreeSetPrint implements Runnable {

        TreeSet<Integer> tsi;
        public TreeSetPrint(TreeSet<Integer> tsi) {
            this.tsi = tsi;
        }

        @Override
        public void run() {
            for (Integer i : tsi) {
                System.out.println("integer = " + i);
            }
        }

    }
    private class TreeSetAdd implements Runnable {

        TreeSet<Integer> tsi;
        public TreeSetAdd(TreeSet<Integer> tsi) {
            this.tsi = tsi;
        }

        @Override
        public void run() {
            int i = new Random().nextInt(100_000);
            System.out.println("Adding i = " + i);
            tsi.add(i);
        }

    }

    @Test
    public void testTimerThread() throws Exception {
        createCommitOffsetsTask();
        stopOnInput();
    }

    private static void stopOnInput() {
        try {
            System.out.println("PRESSE ENTER TO STOP");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ScheduledExecutorService commitOffsetsTask;

    private void createCommitOffsetsTask() {
        commitOffsetsTask = Executors.newSingleThreadScheduledExecutor(commitOffsetsThreadFactory());
        commitOffsetsTask.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("Timer Triggered");
            }
        }, 100, 2000, TimeUnit.MILLISECONDS);
    }

    private ThreadFactory commitOffsetsThreadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "kafka-spout-commit-offsets-thread");
            }
        };
    }

    public static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    public static final String AUTO_COMMIT_INTERVAL_MS = "auto.commit.interval.ms";
    public static final String SESSION_TIMEOUT_MS = "session.timeout.ms";
    public static final String KEY_DESERIALIZER = "key.deserializer";
    public static final String VALUE_DESERIALIZER = "value.deserializer";

    @Test
    public void main() {
        Properties props = getProperties();
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(props);
//        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test"));
        consumer.seekToBeginning(new TopicPartition("test", 0));
        int i = 0;
        while (true) {
            ConsumerRecords<byte[], byte[]> records = consumer.poll(10000);
//            consumer.seek(new TopicPartition("test", 0), 10);
//            ConsumerRecords<String, String> records = consumer.poll(10000);
            /*System.err.println("i = " + i);
//            consumer.seek(new TopicPartition("test", 0), i++);
            if (i == 1) {
                records = consumer.poll(10000);
                consumer.commitAsync();
            }*/
            for (ConsumerRecord<byte[], byte[]> record : records) {
//            for (ConsumerRecord<String, String> record : records) {
//                System.err.println("Inside Loop");
                System.err.printf("offset = %d, key = %s, value = %s\n", record.offset(), record.key(), record.value());
            }
            consumer.commitSync(new HashMap<TopicPartition, OffsetAndMetadata>(){
                {put(new TopicPartition("test", 0), new OffsetAndMetadata(16, "hmcl_meta"));}
            });
//            System.err.println();
        }
//        System.err.println("Exit");
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "127.0.0.1:9092");
//        props.put("bootstrap.servers", "localhost:9923");
        props.put("group.id", "test-group-1");
//        props.put("group.id", "test-consumer-group");
        props.put("enable.auto.commit", "false");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
//        props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
//        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }
}