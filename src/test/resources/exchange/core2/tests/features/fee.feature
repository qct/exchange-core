Feature: An exchange collects fees from both makers and takers while matching orders

  Background:
    Given Add symbol(s) to an exchange:
      | name    | type | baseCurrency | quoteCurrency | baseScaleK | quoteScaleK | takerFee | makerFee | marginBuy | marginSell |
      | XBT_LTC | 0    | 3762         | 4141          | 1000000    | 10000       | 1900     | 700      | 0         | 0          |

  @CollectFee
  Scenario: place an GTC BID order require fees, then cancel it
    Given Add Users and adjust their balances:
      | user | asset | balance    |
      | Bob  | LTC   | 3420000000 |
    When A client Bob could not place an BID order 203 at 11400@30 (type: GTC, symbol: XBT_LTC, reservePrice: 11400) due to RISK_NSF
    Then A balance of a client Bob:
      | XBT | 0          |
      | LTC | 3420000000 |
    And A client Bob does not have active orders

    # add takerFee * 30 - 1
    Given 56999 LTC is added to the balance of a client Bob
    When A client Bob could not place an BID order 203 at 11400@30 (type: GTC, symbol: XBT_LTC, reservePrice: 11400) due to RISK_NSF
    Then A balance of a client Bob:
      | XBT | 0          |
      | LTC | 3420056999 |
    And A client Bob does not have active orders

    # add 1 extra
    Given 1 LTC is added to the balance of a client Bob
    When A client Bob places an BID order 203 at 11400@30 (type: GTC, symbol: XBT_LTC, reservePrice: 11400)
    Then An XBT_LTC order book is:
      | bid | price | ask |
      | 30  | 11400 |     |
    And A balance of a client Bob:
      | XBT | 0 |
      | LTC | 0 |
    And A client Bob orders:
      | id  | price | size | filled | reservePrice | side |
      | 203 | 11400 | 30   | 0      | 11400        | BID  |

    When A client Bob cancels the remaining size 30 of the order 203

    # ltcAmount + takerFee * 30
    Then A balance of a client Bob:
      | XBT | 0          |
      | LTC | 3420057000 |
    And A client Bob does not have active orders
    And Total currency balance report is:
      | currency | accountBalances | fees | adjustments | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 3420057000      | 0    | -3420057000 | 0        | 0              | 0                | 0                 |

  Scenario: a BidGtcMaker matched by an AskIocTakerPartial
    Given Add Users and adjust their balances:
      | user  | asset | balance      |
      | Alice | LTC   | 200000000000 |
      | Bob   | XBT   | 2000000000   |
    When A client Alice places an BID order 101 at 11500@1731 (type: GTC, symbol: XBT_LTC, reservePrice: 11553)

    # LTC: 14281100 = ltcAmount - (order101.reservePrice * step + takerFee) * order101.size = 200000000000 - (11553 * 10k + 1900) * 1731
    Then A balance of a client Alice:
      | XBT | 0        |
      | LTC | 14281100 |
    And A client Alice orders:
      | id  | price | size | filled | reservePrice | side |
      | 101 | 11500 | 1731 | 0      | 11553        | BID  |

    Then Total currency balance report is:
      | currency | accountBalances | fees | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 14281100        | 0    | -200000000000 | 0        | 199985718900   | 0                | 0                 |
      | XBT      | 2000000000      | 0    | -2000000000   | 0        | 0              | 0                | 0                 |

    When A client Bob places an ASK order 102 at 11493@2000 (type: IOC, symbol: XBT_LTC, reservePrice: 0)
    # LTC: 933788300 = ltcAmount - (order101.price * step + makerFee) * 1731L = 200000000000 - (11500 * 10k + 700) * 1731
    Then A balance of a client Alice:
      | XBT | 1731000000 |
      | LTC | 933788300  |
    And A client Alice does not have active orders
    # LTC: 199,061,711,100 = (order101.price * step - takerFee) * 1731 = (11500 * 10k - 1900) * 1731
    # XBT: 269000000 = btcAmount - 1731L * 1M = 2000000000 - 1731000000
    And A balance of a client Bob:
      | XBT | 269000000    |
      | LTC | 199061711100 |
    And A client Bob does not have active orders
    And Total currency balance report is:
      | currency | accountBalances | fees    | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 199995499400    | 4500600 | -200000000000 | 0        | 0              | 0                | 0                 |
      | XBT      | 2000000000      | 0       | -2000000000   | 0        | 0              | 0                | 0                 |

  Scenario: a BidGtcMakerPartial matched by an AskIocTaker
    Given Add Users and adjust their balances:
      | user  | asset | balance      |
      | Alice | LTC   | 200000000000 |
      | Bob   | XBT   | 2000000000   |
    When A client Alice places an BID order 101 at 11500@1731 (type: GTC, symbol: XBT_LTC, reservePrice: 11553)

    # LTC: 14281100 = ltcAmount - (order101.reservePrice * step + takerFee) * order101.size = 200000000000 - (11553 * 10k + 1900) * 1731
    Then A balance of a client Alice:
      | XBT | 0        |
      | LTC | 14281100 |
    And A client Alice orders:
      | id  | price | size | filled | reservePrice | side |
      | 101 | 11500 | 1731 | 0      | 11553        | BID  |

    Then Total currency balance report is:
      | currency | accountBalances | fees | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 14281100        | 0    | -200000000000 | 0        | 199985718900   | 0                | 0                 |
      | XBT      | 2000000000      | 0    | -2000000000   | 0        | 0              | 0                | 0                 |

    When A client Bob places an ASK order 102 at 11493@1000 (type: IOC, symbol: XBT_LTC, reservePrice: 0)
    # LTC: 545481100 = ltcAmount - (order101.price * step + makerFee) * 1000L - (order101.reservePrice * step + takerFee) * 731L)
                    # = 200000000000 - (11500 * 10k + 700) * 1000 - (11553 * 10k + 1900) * 731
    Then A balance of a client Alice:
      | XBT | 1000000000 |
      | LTC | 545481100  |
    And A client Alice orders:
      | id  | price | size | filled | reservePrice | side |
      | 101 | 11500 | 1731 | 1000   | 11553        | BID  |
    # LTC: 114,998,100,000 = (order101.price * step - takerFee) * 1000 = (11500 * 10k - 1900) * 1000
    # XBT: 1,000,000,000 = btcAmount - 1000L * 1M = 2000000000 - 1000000000
    And A balance of a client Bob:
      | XBT | 1000000000   |
      | LTC | 114998100000 |
    And A client Bob does not have active orders
    # ltcFees = (makerFee + takerFee) * 1000L
    And Total currency balance report is:
      | currency | accountBalances | fees    | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 115543581100    | 2600000 | -200000000000 | 0        | 84453818900    | 0                | 0                 |
      | XBT      | 2000000000      | 0       | -2000000000   | 0        | 0              | 0                | 0                 |

  Scenario: an AskGtcMaker matched by a BidIocTakerPartial
    Given Add Users and adjust their balances:
      | user  | asset | balance      |
      | Alice | XBT   | 2000000000   |
      | Bob   | LTC   | 260000000000 |
    When A client Alice places an ASK order 101 at 11500@2000 (type: GTC, symbol: XBT_LTC, reservePrice: 11500)

    Then A balance of a client Alice:
      | XBT | 0 |
      | LTC | 0 |
    And A client Alice orders:
      | id  | price | size | filled | reservePrice | side |
      | 101 | 11500 | 2000 | 0      | 11500        | ASK  |

    Then Total currency balance report is:
      | currency | accountBalances | fees | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 260000000000    | 0    | -260000000000 | 0        | 0              | 0                | 0                 |
      | XBT      | 0               | 0    | -2000000000   | 0        | 2000000000     | 0                | 0                 |

    When A client Bob places an BID order 102 at 11521@2197 (type: IOC, symbol: XBT_LTC, reservePrice: 11659)

    # LTC: 229998600000 = (order101.price * step - makerFee) * 2000L = (11500 * 10k - 700) * 2000
    Then A balance of a client Alice:
      | XBT | 0            |
      | LTC | 229998600000 |
    And A client Alice does not have active orders

    # LTC: 29,996,200,000 = ltcAmount - (order101.price * step + takerFee) * 2000L = 260000000000 - (11500 * 10k + 1900) * 2000
    And A balance of a client Bob:
      | XBT | 2000000000  |
      | LTC | 29996200000 |
    And A client Bob does not have active orders
    # ltcFees = (makerFee + takerFee) * 2000L
    And Total currency balance report is:
      | currency | accountBalances | fees    | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 259994800000    | 5200000 | -260000000000 | 0        | 0              | 0                | 0                 |
      | XBT      | 2000000000      | 0       | -2000000000   | 0        | 0              | 0                | 0                 |

  Scenario: an AskGtcMakerPartial matched by a BidIocTaker
    Given Add Users and adjust their balances:
      | user  | asset | balance      |
      | Alice | XBT   | 2000000000   |
      | Bob   | LTC   | 260000000000 |
    When A client Alice places an ASK order 101 at 11500@2000 (type: GTC, symbol: XBT_LTC, reservePrice: 11500)

    Then A balance of a client Alice:
      | XBT | 0 |
      | LTC | 0 |
    And A client Alice orders:
      | id  | price | size | filled | reservePrice | side |
      | 101 | 11500 | 2000 | 0      | 11500        | ASK  |

    Then Total currency balance report is:
      | currency | accountBalances | fees | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 260000000000    | 0    | -260000000000 | 0        | 0              | 0                | 0                 |
      | XBT      | 0               | 0    | -2000000000   | 0        | 2000000000     | 0                | 0                 |

    When A client Bob places an BID order 102 at 11521@1997 (type: IOC, symbol: XBT_LTC, reservePrice: 11659)

    # LTC: 229,653,602,100 = (order101.price * step - makerFee) * 1997 = (11500 * 10k - 700) * 1997
    Then A balance of a client Alice:
      | XBT | 0            |
      | LTC | 229653602100 |
    And A client Alice orders:
      | id  | price | size | filled | reservePrice | side |
      | 101 | 11500 | 2000 | 1997   | 11500        | ASK  |

    # LTC: 30,341,205,700 = ltcAmount - (order101.price * step + takerFee) * 1997 = 260000000000 - (11500 * 10k + 1900) * 1997
    And A balance of a client Bob:
      | XBT | 1997000000  |
      | LTC | 30341205700 |
    And A client Bob does not have active orders
    # ltcFees = (makerFee + takerFee) * 1997 = 5192200
    And Total currency balance report is:
      | currency | accountBalances | fees    | adjustments   | suspends | ordersBalances | openInterestLong | openInterestShort |
      | LTC      | 259994807800    | 5192200 | -260000000000 | 0        | 0              | 0                | 0                 |
      | XBT      | 1997000000      | 0       | -2000000000   | 0        | 3000000        | 0                | 0                 |

