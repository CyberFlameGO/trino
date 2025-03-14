/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.parquet.reader;

import io.trino.parquet.PrimitiveField;
import io.trino.spi.type.DecimalType;
import org.apache.parquet.bytes.HeapByteBufferAllocator;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.values.ValuesWriter;
import org.apache.parquet.column.values.plain.FixedLenByteArrayPlainValuesWriter;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Types;
import org.openjdk.jmh.annotations.Param;

import static io.trino.parquet.reader.TestData.longToBytes;
import static io.trino.parquet.reader.TestData.unscaledRandomShortDecimalSupplier;
import static java.lang.Math.toIntExact;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY;

public class BenchmarkShortDecimalColumnReader
        extends AbstractColumnReaderBenchmark<long[]>
{
    @Param({
            "1", "2", "3", "4", "5", "6", "7", "8",
    })
    public int byteArrayLength;

    @Override
    protected PrimitiveField createPrimitiveField()
    {
        int precision = maxPrecision(byteArrayLength);
        PrimitiveType parquetType = Types.optional(FIXED_LEN_BYTE_ARRAY)
                .length(byteArrayLength)
                .as(LogicalTypeAnnotation.decimalType(0, precision))
                .named("name");
        return new PrimitiveField(
                DecimalType.createDecimalType(precision),
                true,
                new ColumnDescriptor(new String[] {"test"}, parquetType, 0, 0),
                0);
    }

    @Override
    protected ValuesWriter createValuesWriter(int bufferSize)
    {
        return new FixedLenByteArrayPlainValuesWriter(byteArrayLength, bufferSize, bufferSize, HeapByteBufferAllocator.getInstance());
    }

    @Override
    protected long[] generateDataBatch(int size)
    {
        int precision = ((DecimalType) field.getType()).getPrecision();
        return unscaledRandomShortDecimalSupplier(byteArrayLength * Byte.SIZE, precision).apply(size);
    }

    @Override
    protected void writeValue(ValuesWriter writer, long[] batch, int index)
    {
        Binary binary = Binary.fromConstantByteArray(longToBytes(batch[index], byteArrayLength));
        writer.writeBytes(binary);
    }

    private static int maxPrecision(int numBytes)
    {
        return toIntExact(
                // convert double to long
                Math.round(
                        // number of base-10 digits
                        Math.floor(Math.log10(
                                Math.pow(2, 8 * numBytes - 1) - 1))));  // max value stored in numBytes
    }

    public static void main(String[] args)
            throws Exception
    {
        run(BenchmarkShortDecimalColumnReader.class);
    }
}
