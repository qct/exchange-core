package exchange.core2.tests.steps;

import static exchange.core2.tests.util.ExchangeTestContainer.CHECK_SUCCESS;
import static exchange.core2.tests.util.TestConstants.SYMBOLSPEC_ETH_XBT;
import static exchange.core2.tests.util.TestConstants.SYMBOLSPEC_EUR_USD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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
import exchange.core2.core.common.api.reports.TotalSymbolReportResult;
import exchange.core2.core.common.cmd.CommandResultCode;
import exchange.core2.core.common.cmd.OrderCommand;
import exchange.core2.core.common.cmd.OrderCommandType;
import exchange.core2.core.common.config.PerformanceConfiguration;
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
        users.put("Alice", 1440001L);
        users.put("Bob", 1440002L);
        users.put("Charlie", 1440003L);

        ParameterType(
            "symbol",
            "EUR_USD|ETH_XBT",
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
        DataTableType((Map<String, String> entry) -> CoreSymbolSpecification.builder()
                .symbolId(Integer.parseInt(entry.get("symbolId")))
                .type(SymbolType.of(Integer.parseInt(entry.get("type"))))
                .baseCurrency(Integer.parseInt(entry.get("baseCurrency")))
                .quoteCurrency(Integer.parseInt(entry.get("quoteCurrency")))
                .baseScaleK(Long.parseLong(entry.get("baseScaleK")))
                .quoteScaleK(Long.parseLong(entry.get("quoteScaleK")))
                .takerFee(Long.parseLong(entry.get("takerFee")))
                .makerFee(Long.parseLong(entry.get("makerFee")))
                .marginBuy(Long.parseLong(entry.get("marginBuy")))
                .marginSell(Long.parseLong(entry.get("marginSell")))
                .build());

        Before((HookNoArgsBody) -> {
            container = ExchangeTestContainer.create(testPerformanceConfiguration);
            container.initBasicSymbols();
        });
        After((HookNoArgsBody) -> {
            if (container != null) {
                container.close();
            }
        });

        Given("^Users and their balances:$", (DataTable datatable) -> {
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
        Given("^add symbol\\(s\\) to an exchange:$", (DataTable dataTable) -> {
            List<CoreSymbolSpecification> symbolSpecs = dataTable.asList(CoreSymbolSpecification.class);
            container.addSymbols(symbolSpecs);
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
