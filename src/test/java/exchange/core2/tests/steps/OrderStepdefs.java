package exchange.core2.tests.steps;

import static exchange.core2.tests.util.ExchangeTestContainer.CHECK_SUCCESS;
import static exchange.core2.tests.util.TestConstants.SYMBOLSPECFEE_XBT_LTC;
import static exchange.core2.tests.util.TestConstants.SYMBOLSPEC_ETH_XBT;
import static exchange.core2.tests.util.TestConstants.SYMBOLSPEC_EUR_USD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import exchange.core2.core.common.CoreSymbolSpecification;
import exchange.core2.core.common.MatcherEventType;
import exchange.core2.core.common.MatcherTradeEvent;
import exchange.core2.core.common.Order;
import exchange.core2.core.common.OrderAction;
import exchange.core2.core.common.OrderType;
import exchange.core2.core.common.SymbolType;
import exchange.core2.core.common.UserStatus;
import exchange.core2.core.common.api.ApiAddUser;
import exchange.core2.core.common.api.ApiAdjustUserBalance;
import exchange.core2.core.common.api.ApiCancelOrder;
import exchange.core2.core.common.api.ApiCommand;
import exchange.core2.core.common.api.ApiMoveOrder;
import exchange.core2.core.common.api.ApiPlaceOrder;
import exchange.core2.core.common.api.reports.SingleUserReportResult;
import exchange.core2.core.common.api.reports.SingleUserReportResult.QueryExecutionStatus;
import exchange.core2.core.common.api.reports.TotalCurrencyBalanceReportResult;
import exchange.core2.core.common.api.reports.TotalSymbolReportResult;
import exchange.core2.core.common.cmd.CommandResultCode;
import exchange.core2.core.common.cmd.OrderCommand;
import exchange.core2.core.common.cmd.OrderCommandType;
import exchange.core2.core.common.config.PerformanceConfiguration;
import exchange.core2.core.utils.SerializationUtils;
import exchange.core2.tests.util.ExchangeTestContainer;
import exchange.core2.tests.util.L2MarketDataHelper;
import exchange.core2.tests.util.TestConstants;
import io.cucumber.datatable.DataTable;
import io.cucumber.java8.En;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.collections.impl.map.mutable.primitive.IntLongHashMap;

@Slf4j
public class OrderStepdefs implements En {

    public static PerformanceConfiguration testPerformanceConfiguration = null;

    private ExchangeTestContainer container = null;

    private List<MatcherTradeEvent> matcherEvents;
    private final Map<Long, ApiPlaceOrder> orders = new HashMap<>();

    final Map<String, CoreSymbolSpecification> symbolSpecificationMap = new HashMap<>();
    final Map<String, Long> users = new HashMap<>();

    private final AtomicLong uniqueIdCounterLong = new AtomicLong();

    public OrderStepdefs() {
        symbolSpecificationMap.put("EUR_USD", SYMBOLSPEC_EUR_USD);
        symbolSpecificationMap.put("ETH_XBT", SYMBOLSPEC_ETH_XBT);
        symbolSpecificationMap.put("XBT_LTC", SYMBOLSPECFEE_XBT_LTC);
        users.put("Alice", 1440001L);
        users.put("Bob", 1440002L);
        users.put("Charlie", 1440003L);

        ParameterType(
            "symbol",
            "EUR_USD|ETH_XBT|XBT_LTC",
            symbolSpecificationMap::get
        );
        ParameterType("user",
            "Alice|Bob|Charlie",
            users::get);

        DataTableType((DataTable table) -> {
            DataTable subTable = table.rows(0);
            //skip a header if it presents
            if (table.row(0).get(0) != null && table.row(0).get(0).trim().equals("bid")) {
                subTable = table.rows(1);
            }
            //format | bid | price | ask |
            final L2MarketDataHelper l2helper = new L2MarketDataHelper();
            for (int i = 0; i < subTable.height(); i++) {
                List<String> row = subTable.row(i);
                int price = Integer.parseInt(row.get(1));

                String bid = row.get(0);
                if (bid != null && bid.length() > 0) {
                    l2helper.addBid(price, Integer.parseInt(bid));
                } else {
                    l2helper.addAsk(price, Integer.parseInt(row.get(2)));
                }
            }
            return l2helper;
        });
        DataTableType((Map<String, String> entry) -> {
            String typeStr = entry.get("type");
            String baseCurrencyStr = entry.get("baseCurrency");
            String quoteCurrencyStr = entry.get("quoteCurrency");
            String baseScaleKStr = entry.get("baseScaleK");
            String quoteScaleKStr = entry.get("quoteScaleK");
            String takerFeeStr = entry.get("takerFee");
            String makerFeeStr = entry.get("makerFee");
            String marginBuyStr = entry.get("marginBuy");
            String marginSellStr = entry.get("marginSell");
            SymbolType type = Strings.isNullOrEmpty(typeStr)
                    ? SymbolType.CURRENCY_EXCHANGE_PAIR
                    : SymbolType.of(Integer.parseInt(typeStr));
            int baseCurrency = Strings.isNullOrEmpty(baseCurrencyStr) ? 0 : Integer.parseInt(baseCurrencyStr);
            int quoteCurrency = Strings.isNullOrEmpty(quoteCurrencyStr) ? 0 : Integer.parseInt(quoteCurrencyStr);
            long baseScaleK = Strings.isNullOrEmpty(baseScaleKStr) ? 0 : Long.parseLong(baseScaleKStr);
            long quoteScaleK = Strings.isNullOrEmpty(quoteScaleKStr) ? 0 : Long.parseLong(quoteScaleKStr);
            long takerFee = Strings.isNullOrEmpty(takerFeeStr) ? 0 : Long.parseLong(takerFeeStr);
            long makerFee = Strings.isNullOrEmpty(makerFeeStr) ? 0 : Long.parseLong(makerFeeStr);
            long marginBuy = Strings.isNullOrEmpty(marginBuyStr) ? 0 : Long.parseLong(marginBuyStr);
            long marginSell = Strings.isNullOrEmpty(marginSellStr) ? 0 : Long.parseLong(marginSellStr);
            return CoreSymbolSpecification.builder()
                    .symbolId(symbolSpecificationMap.get(entry.get("name")).symbolId)
                    .type(type)
                    .baseCurrency(baseCurrency)
                    .quoteCurrency(quoteCurrency)
                    .baseScaleK(baseScaleK)
                    .quoteScaleK(quoteScaleK)
                    .takerFee(takerFee)
                    .makerFee(makerFee)
                    .marginBuy(marginBuy)
                    .marginSell(marginSell)
                    .build();
        });
        DataTableType((DataTable table) -> table.cells().stream()
                .skip(1)
                .map(row -> {
                    int currency = TestConstants.getCurrency(row.get(0)); // "currency"
                    return new TotalCurrencyBalanceReportResult(
                            Long.parseLong(row.get(1)) == 0 // "accountBalances"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(1))),
                            Long.parseLong(row.get(2)) == 0 // "fees"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(2))),
                            Long.parseLong(row.get(3)) == 0 // "adjustments"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(3))),
                            Long.parseLong(row.get(4)) == 0 // "suspends"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(4))),
                            Long.parseLong(row.get(5)) == 0 // "ordersBalances"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(5))),
                            Long.parseLong(row.get(6)) == 0 // "openInterestLong"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(6))),
                            Long.parseLong(row.get(7)) == 0 // "openInterestShort"
                                    ? new IntLongHashMap()
                                    : IntLongHashMap.newWithKeysValues(currency, Long.parseLong(row.get(7))));
                })
                .reduce(
                        TotalCurrencyBalanceReportResult.createEmpty(),
                        (a, b) -> new TotalCurrencyBalanceReportResult(
                                SerializationUtils.mergeSum(a.getAccountBalances(), b.getAccountBalances()),
                                SerializationUtils.mergeSum(a.getFees(), b.getFees()),
                                SerializationUtils.mergeSum(a.getAdjustments(), b.getAdjustments()),
                                SerializationUtils.mergeSum(a.getSuspends(), b.getSuspends()),
                                SerializationUtils.mergeSum(a.getOrdersBalances(), b.getOrdersBalances()),
                                SerializationUtils.mergeSum(a.getOpenInterestLong(), b.getOpenInterestLong()),
                                SerializationUtils.mergeSum(a.getOpenInterestShort(), b.getOpenInterestShort()))));

        Before((HookNoArgsBody) -> container = ExchangeTestContainer.create(testPerformanceConfiguration));
        After((HookNoArgsBody) -> {
            if (container != null) {
                container.close();
            }
        });

        Given("^Add Users and adjust their balances:$", (DataTable datatable) -> {
            Map<Long, Map<Integer, Long>> userBalances = Maps.newHashMap();
            datatable.cells().stream().skip(1)
                .forEach(row -> {
                    Long uid = users.get(row.get(0));
                    Map<Integer, Long> balances = userBalances.computeIfAbsent(uid, aLong -> Maps.newHashMap());
                    balances.put(TestConstants.getCurrency(row.get(1)), Long.parseLong(row.get(2)));
                    userBalances.putIfAbsent(uid, balances);
                });
            final List<ApiCommand> cmds = new ArrayList<>();
            userBalances.forEach((uid, value1) -> {
                cmds.add(ApiAddUser.builder().uid(uid).build());
                value1.forEach((key, value) -> cmds.add(
                    ApiAdjustUserBalance.builder()
                        .uid(uid)
                        .transactionId(uniqueIdCounterLong.incrementAndGet())
                        .currency(key)
                        .amount(value).build()));
            });
            container.getApi().submitCommandsSync(cmds);
        });

        When("A client {user} places an {word} order {long} at {long}@{long} \\(type: {word}, symbol: {symbol})",
            (Long clientId, String side, Long orderId, Long price, Long size, String orderType, CoreSymbolSpecification symbol) -> {
                aClientPassAnOrder(clientId, side, orderId, price, size, orderType, symbol, 0,
                    CommandResultCode.SUCCESS);
            });

        When(
            "A client {user} places an {word} order {long} at {long}@{long} \\(type: {word}, symbol: {symbol}, reservePrice: {long})",
            (Long clientId, String side, Long orderId, Long price, Long size, String orderType, CoreSymbolSpecification symbol, Long reservePrice) -> {
                aClientPassAnOrder(clientId, side, orderId, price, size, orderType, symbol, reservePrice,
                    CommandResultCode.SUCCESS);
            });
        Then("The order {long} is partially matched. LastPx: {long}, LastQty: {long}",
            (Long orderId, Long lastPx, Long lastQty) -> {
                theOrderIsMatched(orderId, lastPx, lastQty, false, null);
            });
        Then("The order {long} is fully matched. LastPx: {long}, LastQty: {long}, bidderHoldPrice: {long}",
            (Long orderId, Long lastPx, Long lastQty, Long bidderHoldPrice) -> {
                theOrderIsMatched(orderId, lastPx, lastQty, true, bidderHoldPrice);
            });
        And("No trade events", () -> {
            assertEquals(0, matcherEvents.size());
        });

        When("A client {user} moves a price to {long} of the order {long}",
            (Long clientId, Long newPrice, Long orderId) -> {
                moveOrder(clientId, newPrice, orderId, CommandResultCode.SUCCESS);
            });

        When("A client {user} could not move a price to {long} of the order {long} due to {word}",
            (Long clientId, Long newPrice, Long orderId, String resultCode) -> {
                moveOrder(clientId, newPrice, orderId, CommandResultCode.valueOf(resultCode));
            });

        Then("The order {long} is fully matched. LastPx: {long}, LastQty: {long}",
            (Long orderId, Long lastPx, Long lastQty) -> {
                theOrderIsMatched(orderId, lastPx, lastQty, true, null);
            });

        Then("An {symbol} order book is:",
            (CoreSymbolSpecification symbol, L2MarketDataHelper orderBook) -> {
                assertEquals(orderBook.build(), container.requestCurrentOrderBook(symbol.symbolId));
            });

        When(
            "A client {user} could not place an {word} order {long} at {long}@{long} \\(type: {word}, symbol: {symbol}, reservePrice: {long}) due to {word}",
            (Long clientId, String side, Long orderId, Long price, Long size,
                String orderType, CoreSymbolSpecification symbol, Long reservePrice, String resultCode) -> {
                aClientPassAnOrder(clientId, side, orderId, price, size, orderType, symbol, reservePrice,
                    CommandResultCode.valueOf(resultCode));
            });

        And("A balance of a client {user}:",
            (Long clientId, DataTable table) -> {
                List<List<String>> balance = table.asLists();
                SingleUserReportResult profile = container.getUserProfile(clientId);
                for (List<String> record : balance) {
                    assertThat("Unexpected balance of: " + record.get(0),
                        profile.getAccounts().get(TestConstants.getCurrency(record.get(0))),
                        is(Long.parseLong(record.get(1))));
                }
            });

        And("A client {user} orders:", (Long clientId, DataTable table) -> {
            List<List<String>> lists = table.asLists();
            //| id | price | size | filled | reservePrice | side |

            SingleUserReportResult profile = container.getUserProfile(clientId);

            //skip a header if it presents
            Map<String, Integer> fieldNameByIndex = new HashMap<>();

            //read a header
            int i = 0;
            for (String field : lists.get(0)) {
                fieldNameByIndex.put(field, i++);
            }

            //remove header
            lists = lists.subList(1, lists.size());

            Map<Long, Order> orders = profile.fetchIndexedOrders();

            for (List<String> record : lists) {
                long orderId = Long.parseLong(record.get(fieldNameByIndex.get("id")));
                Order order = orders.get(orderId);
                assertNotNull(order);

                checkField(fieldNameByIndex, record, "price", order.getPrice());
                checkField(fieldNameByIndex, record, "size", order.getSize());
                checkField(fieldNameByIndex, record, "filled", order.getFilled());
                checkField(fieldNameByIndex, record, "reservePrice", order.getReserveBidPrice());

                if (fieldNameByIndex.containsKey("side")) {
                    OrderAction action = OrderAction.valueOf(record.get(fieldNameByIndex.get("side")));
                    assertEquals(action, order.getAction(), "Unexpected action");
                }

            }
        });

        And("A client {user} does not have active orders", (Long clientId) -> {
            SingleUserReportResult profile = container.getUserProfile(clientId);
            assertEquals(0, profile.fetchIndexedOrders().size());
        });

        Given("{long} {word} is added to the balance of a client {user}",
            (Long ammount, String currency, Long clientId) -> {

                // add 1 szabo more
                container.submitCommandSync(ApiAdjustUserBalance.builder()
                    .uid(clientId)
                    .currency(TestConstants.getCurrency(currency))
                    .amount(ammount).transactionId(uniqueIdCounterLong.incrementAndGet()).build(), CHECK_SUCCESS);
            });

        When("A client {user} cancels the remaining size {long} of the order {long}",
            (Long clientId, Long size, Long orderId) -> {
                ApiPlaceOrder initialOrder = orders.get(orderId);

                ApiCancelOrder order = ApiCancelOrder.builder().orderId(orderId).uid(clientId)
                    .symbol(initialOrder.symbol)
                    .build();

                container.getApi().submitCommandAsyncFullResponse(order).thenAccept(
                    cmd -> {
                        assertThat(cmd.resultCode, is(CommandResultCode.SUCCESS));
                        assertThat(cmd.command, is(OrderCommandType.CANCEL_ORDER));
                        assertThat(cmd.orderId, is(orderId));
                        assertThat(cmd.uid, is(clientId));
                        assertThat(cmd.symbol, is(initialOrder.symbol));
                        assertThat(cmd.action, is(initialOrder.action));

                        final MatcherTradeEvent evt = cmd.matcherEvent;
                        assertNotNull(evt);
                        assertThat(evt.eventType, is(MatcherEventType.REDUCE));
                        assertThat(evt.size, is(size));
                    }).join();
            });

        When("Suspend {user} who has balances will return {string}", (Long clientId, String resultCode) -> {
            CompletableFuture<OrderCommand> future = new CompletableFuture<>();
            container.getApi().suspendUser(clientId, future::complete);
            log.debug("Suspend user result: {}", future.get());
            assertSame(future.get().resultCode, CommandResultCode.valueOf(resultCode));
        });
        When("Suspend {user} who has no balances", (Long clientId) -> {
            container.getApi().suspendUser(clientId, CHECK_SUCCESS);
        });
        Then(
                "Status of {user} is {string}",
                (Long clientId, String status) ->
                        assertSame(container.getUserProfile(clientId).getUserStatus(), UserStatus.valueOf(status)));
        Then(
                "Query {user} will return {string}",
                (Long clientId, String status) -> assertSame(
                        container.getUserProfile(clientId).getQueryExecutionStatus(),
                        QueryExecutionStatus.valueOf(status)));

        Then("An exchange symbols are:", (DataTable dataTable) -> {
            List<CoreSymbolSpecification> symbolSpecs = dataTable.asList(CoreSymbolSpecification.class);
            TotalSymbolReportResult result = container.totalSymbolReport();
            assertThat(
                    symbolSpecs,
                    containsInAnyOrder(result.getSymbolSpecs().values().toArray(new CoreSymbolSpecification[0])));
        });
        Given("^Add symbol\\(s\\) to an exchange:$", (DataTable dataTable) -> {
            List<CoreSymbolSpecification> symbolSpecs = dataTable.asList(CoreSymbolSpecification.class);
            container.addSymbols(symbolSpecs);
        });
        And("^Total currency balance report is:$", (TotalCurrencyBalanceReportResult report) -> {
            TotalCurrencyBalanceReportResult result = container.totalBalanceReport().filterZero();
            assertEquals(report.isGlobalBalancesAllZero(), report.isGlobalBalancesAllZero());
            assertEquals(report, result);
        });
        Given(
                "Adjust symbol {symbol} taker fee to {long}, maker fee to {long}",
                (CoreSymbolSpecification symbol, Long takerFee, Long makerFee) ->
                        container.adjustFee(CoreSymbolSpecification.builder()
                                .symbolId(symbol.symbolId)
                                .type(symbol.type)
                                .takerFee(takerFee)
                                .makerFee(makerFee)
                                .build()));
        Given("^Bulk adjust symbols fees:$", (DataTable datatable) -> {
            List<CoreSymbolSpecification> symbols = datatable.asList(CoreSymbolSpecification.class);
            container.adjustFee(symbols);
        });
    }

    private void aClientPassAnOrder(long clientId, String side, long orderId, long price, long size, String orderType,
        CoreSymbolSpecification symbol, long reservePrice, CommandResultCode resultCode) {

        ApiPlaceOrder.ApiPlaceOrderBuilder builder = ApiPlaceOrder.builder().uid(clientId).orderId(orderId).price(price)
            .size(size)
            .action(OrderAction.valueOf(side)).orderType(OrderType.valueOf(orderType))
            .symbol(symbol.symbolId);

        if (reservePrice > 0) {
            builder.reservePrice(reservePrice);
        }

        final ApiPlaceOrder order = builder.build();

        orders.put(orderId, order);

        log.debug("PLACE : {}", order);
        container.getApi().submitCommandAsyncFullResponse(order).thenAccept(cmd -> {
            assertThat(cmd.orderId, is(orderId));
            assertThat(cmd.resultCode, is(resultCode));
            assertThat(cmd.uid, is(clientId));
            assertThat(cmd.price, is(price));
            assertThat(cmd.size, is(size));
            assertThat(cmd.action, is(OrderAction.valueOf(side)));
            assertThat(cmd.orderType, is(OrderType.valueOf(orderType)));
            assertThat(cmd.symbol, is(symbol.symbolId));

            OrderStepdefs.this.matcherEvents = cmd.extractEvents();
        }).join();
    }

    private void theOrderIsMatched(long orderId, long lastPx, long lastQty, boolean completed, Long bidderHoldPrice) {
        assertThat(matcherEvents.size(), is(1));

        MatcherTradeEvent evt = matcherEvents.get(0);
        assertThat(evt.matchedOrderId, is(orderId));
        assertThat(evt.matchedOrderUid, is(orders.get(orderId).uid));
        assertThat(evt.matchedOrderCompleted, is(completed));
        assertThat(evt.eventType, is(MatcherEventType.TRADE));
        assertThat(evt.size, is(lastQty));
        assertThat(evt.price, is(lastPx));
        if (bidderHoldPrice != null) {
            assertThat(evt.bidderHoldPrice, is(bidderHoldPrice));
        }
    }

    private void moveOrder(long clientId, long newPrice, long orderId, CommandResultCode resultCode2) {
        ApiPlaceOrder initialOrder = orders.get(orderId);

        final ApiMoveOrder moveOrder = ApiMoveOrder.builder().symbol(initialOrder.symbol).uid(clientId).orderId(orderId)
            .newPrice(newPrice).build();
        log.debug("MOVE : {}", moveOrder);
        container.submitCommandSync(moveOrder, cmd -> {
            assertThat(cmd.resultCode, is(resultCode2));
            assertThat(cmd.orderId, is(orderId));
            assertThat(cmd.uid, is(clientId));

            matcherEvents = cmd.extractEvents();
        });
    }

    private void checkField(Map<String, Integer> fieldNameByIndex, List<String> record, String field, long expected) {
        if (fieldNameByIndex.containsKey(field)) {
            long actual = Long.parseLong(record.get(fieldNameByIndex.get(field)));
            assertEquals(actual, expected, "Unexpected value for " + field);
        }
    }
}
