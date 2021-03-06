/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.cdap.etl.spark.batch;

import com.google.common.base.Optional;
import io.cdap.cdap.api.data.DatasetContext;
import io.cdap.cdap.api.spark.JavaSparkExecutionContext;
import io.cdap.cdap.etl.spark.Compat;
import io.cdap.cdap.etl.spark.SparkCollection;
import io.cdap.cdap.etl.spark.SparkPairCollection;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.SQLContext;
import scala.Tuple2;

/**
 * Implementation of {@link SparkCollection} that is backed by a JavaPairRDD.
 *
 * @param <K> type of key in the collection
 * @param <V> type of value in the collection
 */
public class PairRDDCollection<K, V> implements SparkPairCollection<K, V> {
  private final JavaSparkExecutionContext sec;
  private final JavaSparkContext jsc;
  private final SQLContext sqlContext;
  private final DatasetContext datasetContext;
  private final SparkBatchSinkFactory sinkFactory;
  private final JavaPairRDD<K, V> pairRDD;

  public PairRDDCollection(JavaSparkExecutionContext sec, JavaSparkContext jsc, SQLContext sqlContext,
                           DatasetContext datasetContext, SparkBatchSinkFactory sinkFactory,
                           JavaPairRDD<K, V> pairRDD) {
    this.sec = sec;
    this.jsc = jsc;
    this.sqlContext = sqlContext;
    this.datasetContext = datasetContext;
    this.sinkFactory = sinkFactory;
    this.pairRDD = pairRDD;
  }

  @SuppressWarnings("unchecked")
  @Override
  public JavaPairRDD<K, V> getUnderlying() {
    return pairRDD;
  }

  @Override
  public <T> SparkCollection<T> flatMap(FlatMapFunction<Tuple2<K, V>, T> function) {
    return new RDDCollection<>(sec, jsc, sqlContext, datasetContext, sinkFactory, pairRDD.flatMap(function));
  }

  @Override
  public <T> SparkPairCollection<K, T> mapValues(Function<V, T> function) {
    return wrap(pairRDD.mapValues(function));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> SparkPairCollection<K, Tuple2<V, T>> join(SparkPairCollection<K, T> other) {
    return wrap(pairRDD.join((JavaPairRDD<K, T>) other.getUnderlying()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> SparkPairCollection<K, Tuple2<V, T>> join(SparkPairCollection<K, T> other, int numPartitions) {
    return wrap(pairRDD.join((JavaPairRDD<K, T>) other.getUnderlying(), numPartitions));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> SparkPairCollection<K, Tuple2<V, Optional<T>>> leftOuterJoin(SparkPairCollection<K, T> other) {
    return wrap(Compat.leftOuterJoin(pairRDD, (JavaPairRDD<K, T>) other.getUnderlying()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> SparkPairCollection<K, Tuple2<V, Optional<T>>> leftOuterJoin(SparkPairCollection<K, T> other,
                                                                          int numPartitions) {
    return wrap(Compat.leftOuterJoin(pairRDD, (JavaPairRDD<K, T>) other.getUnderlying(), numPartitions));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> SparkPairCollection<K, Tuple2<Optional<V>, Optional<T>>> fullOuterJoin(SparkPairCollection<K, T> other) {
    return wrap(Compat.fullOuterJoin(pairRDD, (JavaPairRDD<K, T>) other.getUnderlying()));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> SparkPairCollection<K, Tuple2<Optional<V>, Optional<T>>> fullOuterJoin(SparkPairCollection<K, T> other,
                                                                                    int numPartitions) {
    return wrap(Compat.fullOuterJoin(pairRDD, (JavaPairRDD<K, T>) other.getUnderlying(), numPartitions));
  }

  private <X, Y> SparkPairCollection<X, Y> wrap(JavaPairRDD<X, Y> javaPairRDD) {
    return new PairRDDCollection<>(sec, jsc, sqlContext, datasetContext, sinkFactory, javaPairRDD);
  }
}
