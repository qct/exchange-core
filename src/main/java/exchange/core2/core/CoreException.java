package exchange.core2.core;

import exchange.core2.core.common.cmd.CommandResultCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>Created by qct on 2022/10/15.
 */
@AllArgsConstructor
@Getter
public class CoreException extends RuntimeException {

    private final CommandResultCode resultCode;
}
