package exchange.core2.core.common.api.reports;

import exchange.core2.core.common.CoreSymbolSpecification;
import exchange.core2.core.processors.MatchingEngineRouter;
import exchange.core2.core.processors.RiskEngine;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

/**
 * <p>Created by qct on 2022/10/8.
 */
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public final class TotalSymbolReportQuery implements ReportQuery<TotalSymbolReportResult>{

    public TotalSymbolReportQuery(BytesIn bytesIn) {
        // do nothing
    }

    @Override
    public int getReportTypeCode() {
        return ReportType.TOTAL_SYMBOL_REPORT.getCode();
    }

    @Override
    public TotalSymbolReportResult createResult(Stream<BytesIn> sections) {
        return TotalSymbolReportResult.merge(sections);
    }

    @Override
    public Optional<TotalSymbolReportResult> process(MatchingEngineRouter matchingEngine) {
        return Optional.empty();
    }

    @Override
    public Optional<TotalSymbolReportResult> process(RiskEngine riskEngine) {
        IntObjectHashMap<CoreSymbolSpecification> symbolSpecs =
                riskEngine.getSymbolSpecificationProvider().getSymbolSpecs();
        return Optional.of(new TotalSymbolReportResult(symbolSpecs));
    }

    @Override
    public void writeMarshallable(BytesOut bytes) {
        // do nothing
    }
}
