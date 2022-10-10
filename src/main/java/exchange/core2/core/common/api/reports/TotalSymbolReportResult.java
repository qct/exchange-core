package exchange.core2.core.common.api.reports;

import exchange.core2.core.common.CoreSymbolSpecification;
import exchange.core2.core.utils.SerializationUtils;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

/**
 * <p>Created by qct on 2022/10/8.
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@ToString
public final class TotalSymbolReportResult implements ReportResult {

    private final IntObjectHashMap<CoreSymbolSpecification> symbolSpecs;

    private TotalSymbolReportResult(final BytesIn bytesIn) {
        this.symbolSpecs = SerializationUtils.readNullable(
                bytesIn, bytes -> SerializationUtils.readIntHashMap(bytes, CoreSymbolSpecification::new));
    }

    public static TotalSymbolReportResult createEmpty() {
        return new TotalSymbolReportResult((IntObjectHashMap<CoreSymbolSpecification>) null);
    }

    public static TotalSymbolReportResult merge(Stream<BytesIn> pieces) {
        return pieces.map(TotalSymbolReportResult::new)
                .reduce(
                        createEmpty(),
                        (a, b) -> new TotalSymbolReportResult(
                                SerializationUtils.mergeOverride(a.symbolSpecs, b.symbolSpecs)));
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {
        SerializationUtils.marshallNullable(symbolSpecs, bytes, SerializationUtils::marshallIntHashMap);
    }
}
