/*
 * Copyright 2019 Maksim Zheravin
 *
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
package exchange.core2.core.common.api.binary;

import exchange.core2.core.utils.SerializationUtils;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import org.agrona.collections.IntArrayList;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public final class BatchRemoveSymbolCommand implements BinaryDataCommand {

    private final IntArrayList symbolIds;

    public BatchRemoveSymbolCommand(final Integer symbolId) {
        symbolIds = new IntArrayList(new int[] {symbolId}, 1, -1);
    }

    public BatchRemoveSymbolCommand(final Collection<Integer> collection) {
        symbolIds = new IntArrayList(collection.stream().mapToInt(i -> i).toArray(), collection.size(), -1);
    }

    public BatchRemoveSymbolCommand(final BytesIn bytes) {
        int[] intArray = SerializationUtils.readIntArray(bytes);
        symbolIds = new IntArrayList(intArray, intArray.length, -1);
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {
        SerializationUtils.marshallIntArray(symbolIds.toIntArray(), bytes);
    }

    @Override
    public int getBinaryCommandTypeCode() {
        return BinaryCommandType.REMOVE_SYMBOLS.getCode();
    }
}
