package exchange.core2.core.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * <p>Created by qct on 2022/10/16.
 */
@Builder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public final class ApiRemoveSymbol extends ApiCommand {

    public final int symbolId;

    @Override
    public String toString() {
        return "[REMOVE_SYMBOL " + symbolId + "]";
    }
}
