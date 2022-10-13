package exchange.core2.core.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * <p>Created by qct on 2022/10/15.
 */
@Builder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class ApiAdjustSymbolFee extends ApiCommand {

    public final int symbolId;
    public final long takerFee;
    public final long makerFee;

    @Override
    public String toString() {
        return "[ADJUST_SYMBOL_FEE " + symbolId + " takerFee: " + takerFee + " makerFee: " + makerFee + "]";
    }
}
