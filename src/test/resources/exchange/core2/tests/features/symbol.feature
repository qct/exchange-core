Feature: An exchange can add symbols, manage symbols

  @SymbolReport
  Scenario: and symbol, query symbol, manipulate symbol
    Then An exchange symbols are:
      | symbolId | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | 5991     | 1    | 978          | 840           | 1          | 1           | 0        | 0        | 2200      | 3210       |
      | 9269     | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |

    Given add symbol(s) to an exchange:
      | symbolId | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | 9340     | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |
    Then An exchange symbols are:
      | symbolId | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | 5991     | 1    | 978          | 840           | 1          | 1           | 0        | 0        | 2200      | 3210       |
      | 9269     | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |
      | 9340     | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |