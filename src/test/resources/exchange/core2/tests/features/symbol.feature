Feature: An exchange can add symbols, manage symbols

  Background:
    Given Add symbol(s) to an exchange:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 0        | 0        | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |

  @SymbolReport
  Scenario: and symbol, query symbol, manipulate symbol
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 0        | 0        | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |

    When Add symbol(s) to an exchange:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 0        | 0        | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |

    When Remove a symbol EUR_USD from an exchange:
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |

    When Could not remove a symbol EUR_USD from an exchange due to SYMBOL_MGMT_SYMBOL_NOT_EXISTS:
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |

    When Bulk remove symbols:
      | ETH_XBT |
      | XBT_LTC |
    Then An exchange does not have symbols

  Scenario: adjust a single symbol's fee
    Given Adjust a single symbol EUR_USD taker fee to 500, maker fee to 200
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 500      | 200      | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |

    When Could not adjust a single symbol XBT_LTC taker fee to 300, maker fee to 300 due to SYMBOL_MGMT_SYMBOL_NOT_EXISTS
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 500      | 200      | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |

  Scenario: bulk adjust symbol's fee
    Given Adjust symbol EUR_USD taker fee to 500, maker fee to 200
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 500      | 200      | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |
    Given Add symbol(s) to an exchange:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |

    Given Adjust symbol XBT_LTC taker fee to 2000, maker fee to 800
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 500      | 200      | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 0        | 0        | 0         | 0          |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 2000     | 800      | 0         | 0          |

    Given Bulk adjust symbols fees:
      | name    | takerFee | makerFee |
      | EUR_USD | 300      | 200      |
      | ETH_XBT | 400      | 300      |
      | XBT_LTC | 500      | 400      |
    Then An exchange symbols are:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | EUR_USD | 1    | 978          | 840           | 1          | 1           | 300      | 200      | 2200      | 3210       |
      | ETH_XBT | 0    | 3928         | 3762          | 100000     | 10          | 400      | 300      | 0         | 0          |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 500      | 400      | 0         | 0          |
