/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.spark.bigquery;

import com.google.cloud.bigquery.connector.common.BigQueryStorageClientFactory;
import com.google.cloud.bigquery.connector.common.ReadRowsHelper;
import com.google.cloud.bigquery.storage.v1beta1.Storage;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.sources.v2.reader.InputPartition;
import org.apache.spark.sql.sources.v2.reader.InputPartitionReader;

import java.util.Iterator;

public class BigQueryInputPartition implements InputPartition<InternalRow> {

    private final BigQueryStorageClientFactory bigQueryStorageClientFactory;
    private final String streamName;
    private final int maxReadRowsRetries;
    private final ReadRowsResponseToInternalRowIteratorConverter converter;

    public BigQueryInputPartition(
            BigQueryStorageClientFactory bigQueryStorageClientFactory,
            String streamName,
            int maxReadRowsRetries,
            ReadRowsResponseToInternalRowIteratorConverter converter) {
        this.bigQueryStorageClientFactory = bigQueryStorageClientFactory;
        this.streamName = streamName;
        this.maxReadRowsRetries = maxReadRowsRetries;
        this.converter = converter;
    }

    @Override
    public InputPartitionReader<InternalRow> createPartitionReader() {
        Storage.ReadRowsRequest.Builder readRowsRequest = Storage.ReadRowsRequest.newBuilder()
                .setReadPosition(Storage.StreamPosition.newBuilder()
                        .setStream(Storage.Stream.newBuilder()
                                .setName(streamName)));
        ReadRowsHelper readRowsHelper = new ReadRowsHelper(bigQueryStorageClientFactory, readRowsRequest, maxReadRowsRetries);
        Iterator<Storage.ReadRowsResponse> readRowsResponses = readRowsHelper.readRows();
        return new BigQueryInputPartitionReader(readRowsResponses, converter);
    }
}