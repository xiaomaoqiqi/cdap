/*
 * Copyright © 2014 Cask Data, Inc.
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

package co.cask.cdap.data2.dataset2.lib.table;

import co.cask.cdap.api.annotation.Beta;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.batch.RecordScanner;
import co.cask.cdap.api.data.batch.Scannables;
import co.cask.cdap.api.data.batch.Split;
import co.cask.cdap.api.data.batch.SplitReader;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.dataset.DataSetException;
import co.cask.cdap.api.dataset.DatasetOpHandler;
import co.cask.cdap.api.dataset.lib.AbstractCloseableIterator;
import co.cask.cdap.api.dataset.lib.AbstractDataset;
import co.cask.cdap.api.dataset.lib.CloseableIterator;
import co.cask.cdap.api.dataset.lib.KeyValue;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.dataset.lib.ObjectStore;
import co.cask.cdap.common.BadRequestException;
import co.cask.cdap.common.io.BinaryDecoder;
import co.cask.cdap.common.io.BinaryEncoder;
import co.cask.cdap.internal.io.ReflectionDatumReader;
import co.cask.cdap.internal.io.ReflectionDatumWriter;
import co.cask.cdap.internal.io.TypeRepresentation;
import com.google.common.base.Preconditions;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Default implementation for {@link ObjectStore}
 * @param <T> the type of objects in the store
 */
@Beta
public class ObjectStoreDataset<T> extends AbstractDataset implements ObjectStore<T>, DatasetOpHandler {
  private final KeyValueTable kvTable;
  private final TypeRepresentation typeRep;
  private final Schema schema;

  private final ReflectionDatumWriter<T> datumWriter;
  private final ReflectionDatumReader<T> datumReader;
  private static final Gson GSON = new Gson();

  public ObjectStoreDataset(String name, KeyValueTable kvTable, TypeRepresentation typeRep,
                            Schema schema, @Nullable ClassLoader classLoader) {
    super(name, kvTable);
    this.kvTable = kvTable;
    this.typeRep = typeRep;
    this.typeRep.setClassLoader(classLoader);
    this.schema = schema;
    this.datumWriter = new ReflectionDatumWriter<>(this.schema);
    this.datumReader = new ReflectionDatumReader<>(this.schema, getTypeToken());
  }

  public ObjectStoreDataset(String name, KeyValueTable kvTable,
                            TypeRepresentation typeRep, Schema schema) {
    this(name, kvTable, typeRep, schema, null);
  }

  @Override
  public void write(String key, T object) {
    kvTable.write(Bytes.toBytes(key), encode(object));
  }

  @Override
  public void write(byte[] key, T object) {
    kvTable.write(key, encode(object));
  }

  @Override
  public T read(String key) {
    return decode(kvTable.read(Bytes.toBytes(key)));
  }

  @Override
  public CloseableIterator<KeyValue<byte[], T>> scan(byte[] startRow, byte[] stopRow) {
    final CloseableIterator<KeyValue<byte[], byte[]>> keyValueIterator = kvTable.scan(startRow, stopRow);
    return new AbstractCloseableIterator<KeyValue<byte[], T>>() {
      boolean closed = false;
      @Override
      protected KeyValue<byte[], T> computeNext() {
        Preconditions.checkState(!closed);
        if (keyValueIterator.hasNext()) {
          KeyValue<byte[], byte[]> row = keyValueIterator.next();
          return new KeyValue<>(row.getKey(), decode(row.getValue()));
        }
        close();
        return null;
      }

      @Override
      public void close() {
        keyValueIterator.close();
        endOfData();
        closed = true;
      }
    };
  }

  @Override
  public T read(byte[] key) {
    byte[] read = kvTable.read(key);
    return decode(read);
  }

  @Override
  public void delete(byte[] key) {
    kvTable.delete(key);
  }

  // this function only exists to reduce the scope of the SuppressWarnings annotation to a single cast.
  @SuppressWarnings("unchecked")
  private TypeToken<T> getTypeToken() {
    return (TypeToken<T>) TypeToken.of(this.typeRep.toType());
  }

  private byte[] encode(T object) {
    // encode T using schema
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    BinaryEncoder encoder = new BinaryEncoder(bos);
    try {
      this.datumWriter.encode(object, encoder);
    } catch (IOException e) {
      // SHOULD NEVER happen
      throw new DataSetException("Failed to encode object to be written: " + e.getMessage(), e);
    }
    return bos.toByteArray();
  }

  private T decode(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    // decode T using schema
    ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    BinaryDecoder decoder = new BinaryDecoder(bis);
    try {
      return this.datumReader.read(decoder, this.schema);
    } catch (IOException e) {
      // SHOULD NEVER happen
      throw new DataSetException("Failed to decode read object: " + e.getMessage(), e);
    }
  }

  // TODO: it should implement RecordScannable, but due to classloading issues it doesn't
//  @Override
  public RecordScanner<KeyValue<byte[], T>> createSplitRecordScanner(Split split) {
    return Scannables.splitRecordScanner(createSplitReader(split), new ObjectRecordMaker());
  }

  // TODO: it should implement RecordScannable, but due to classloading issues it doesn't
//  @Override
  public Type getRecordType() {
    return typeRep.toType();
  }

  @Override
  public List<Split> getSplits() {
    return kvTable.getSplits();
  }

  public List<Split> getSplits(int numSplits, byte[] start, byte[] stop) {
    return kvTable.getSplits(numSplits, start, stop);
  }

  @Override
  public SplitReader<byte[], T> createSplitReader(Split split) {
    return new ObjectScanner(kvTable.createSplitReader(split));
  }

  @Override
  public byte[] handleOperation(String method, Reader body) throws Exception {
    switch (method) {
      case "read": {
        StringKeyObjectValue input = GSON.fromJson(body, new TypeToken<StringKeyObjectValue<T>>() {
        }.getType());
        input.validateKey();

        StringKeyObjectValue response = new StringKeyObjectValue<>(input.getKey(), read(input.getKey()));
        return Bytes.toBytes(GSON.toJson(response));
      }
      case "write": {
        StringKeyObjectValue<T> input = GSON.fromJson(body, new TypeToken<StringKeyObjectValue<T>>() {
        }.getType());
        input.validateKeyValue();

        write(input.getKey(), input.getValue());
        return new byte[0];
      }

      case "delete": {
        StringKeyObjectValue input = GSON.fromJson(body, new TypeToken<StringKeyObjectValue<T>>() {
        }.getType());
        input.validateKey();
        delete(Bytes.toBytes(input.getKey()));
        return new byte[0];
      }case "scan" : {
        StringKeyObjectValue<T> input = GSON.fromJson(body, new TypeToken<StringKeyObjectValue<T>>() {
        }.getType());
        input.validateKeyValue();

        CloseableIterator<KeyValue<byte[], T>> scan = scan(Bytes.toBytes(input.getKey()), Bytes.toBytes((String) input.getValue()));
        Bytes.toBytes(GSON.toJson(mapFromIterator(scan)));
      }
      default:
        throw new BadRequestException(String.format("Method %s is not supported", method));
    }
  }

  private Map<byte[], T> mapFromIterator(Iterator<KeyValue<byte[], T>> iterator) {
    Map<byte[], T> map = new HashMap<>();
    while (iterator.hasNext()) {
      KeyValue<byte[], T> keyValue = iterator.next();
      map.put(keyValue.getKey(), keyValue.getValue());
    }
    return map;
  }

  /**
   * {@link co.cask.cdap.api.data.batch.Scannables.RecordMaker} for {@link ObjectStoreDataset}.
   */
  public class ObjectRecordMaker implements Scannables.RecordMaker<byte[], T, KeyValue<byte[], T>> {
    @Override
    public KeyValue<byte[], T> makeRecord(byte[] key, T value) {
      return new KeyValue<>(key, value);
    }
  }

  /**
   * The split reader for objects is reading a table split using the underlying KeyValueTable's split reader.
   */
  public class ObjectScanner extends SplitReader<byte[], T> {

    // the underlying KeyValueTable's split reader
    private SplitReader<byte[], byte[]> reader;

    public ObjectScanner(SplitReader<byte[], byte[]> reader) {
      this.reader = reader;
    }

    @Override
    public void initialize(Split split) throws InterruptedException {
      this.reader.initialize(split);
    }

    @Override
    public boolean nextKeyValue() throws InterruptedException {
      return this.reader.nextKeyValue();
    }

    @Override
    public byte[] getCurrentKey() throws InterruptedException {
      return this.reader.getCurrentKey();
    }

    @Override
    public T getCurrentValue() throws InterruptedException {
      return decode(this.reader.getCurrentValue());
    }

    @Override
    public void close() {
      this.reader.close();
    }
  }

  private static class StringKeyObjectValue<T> {
    private final String key;
    private final T value;

    private StringKeyObjectValue(String key, T value) {
      this.key = key;
      this.value = value;
    }


    public String getKey() {
      return key;
    }

    public T getValue() {
      return value;
    }

    public void validateKey() {
      if(key == null) {
        throw new javax.ws.rs.BadRequestException(
          "Missing key in body. Expected format: {\"key\":\"<your-key>\"}");
      }
    }

    public void validateKeyValue() {
      if(key == null) {
        throw new javax.ws.rs.BadRequestException(
          "Missing key in body. Expected format: {\"key\":\"<your-key>\", \"value\":\"<your-value>\"}");
      }
    }
  }
}
