
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  private static RestTemplate restTemplate = new RestTemplate();
  private static final String API_TOKEN = "4dd7b0eb5e86af400d5c10e372ab0a2e49ec7bfb";
  private static String apiUrl = "https://api.tiingo.com/tiingo/daily/{sym}/prices?startDate={startDate}&endDate={endDate}&token={token}";

  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Read the json file provided in the argument[0]. The file will be available in
  // the classpath.
  // 1. Use #resolveFileFromResources to get actual file from classpath.
  // 2. Extract stock symbols from the json file with ObjectMapper provided by
  // #getObjectMapper.
  // 3. Return the list of all symbols in the same order as provided in json.

  // Note:
  // 1. There can be few unused imports, you will need to fix them to make the
  // build pass.
  // 2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    File tradeFile = resolveFileFromResources(args[0]);
    ObjectMapper mapper = getObjectMapper();
    List<String> symbols = new ArrayList<>();

    PortfolioTrade[] trades = mapper.readValue(tradeFile, PortfolioTrade[].class);

    for (PortfolioTrade trade : trades) {
      symbols.add(trade.getSymbol());
    }

    return symbols;
  }

  private static PortfolioTrade[] getTrades(String fileName) throws IOException, URISyntaxException {
    File tradeFile = resolveFileFromResources(fileName);
    ObjectMapper mapper = getObjectMapper();

    PortfolioTrade[] trades = mapper.readValue(tradeFile, PortfolioTrade[].class);

    return trades;
  }

  private static List<Candle> getQuotes(String sym, LocalDate startDate, String endDate) {
    Map<String, String> map = new HashMap<>();
    map.put("sym", sym);
    map.put("token", API_TOKEN);
    map.put("endDate", endDate);
    map.put("startDate", startDate.toString());

    if (LocalDate.parse(endDate).isBefore(startDate))
      throw new RuntimeException();

    Candle[] quotes = restTemplate.getForObject(apiUrl, TiingoCandle[].class, map);

    return Arrays.asList(quotes);
  }

  private static List<String> sortSymbols(List<Map.Entry<String, Double>> symbolClose) {
    List<String> sortedSymbols = new ArrayList<>();

    Collections.sort(symbolClose, new Comparator<Map.Entry<String, Double>>() {
      @Override
      public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
        return e1.getValue().compareTo(e2.getValue());
      }
    });

    for (Map.Entry<String, Double> entry : symbolClose) {
      sortedSymbols.add(entry.getKey());
    }

    return sortedSymbols;
  }

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  // TODO: CRIO_TASK_MODULE_REST_API
  // Find out the closing price of each stock on the end_date and return the list
  // of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  // and deserialize the results in List<Candle>

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    PortfolioTrade[] trades = getTrades(args[0]);
    List<String> symbols = new ArrayList<>();
    List<LocalDate> startDates = new ArrayList<>();
    String endDate = args[1];
    Map<String, Double> symCloseVal = new HashMap<>();

    for (PortfolioTrade trade : trades) {
      symbols.add(trade.getSymbol());
      startDates.add(trade.getPurchaseDate());
    }

    for (int i = 0; i < trades.length; i++) {
      String symbol = symbols.get(i);
      LocalDate startDate = startDates.get(i);
      List<Candle> candles = getQuotes(symbol, startDate, endDate);

      for (Candle candle : candles) {
        symCloseVal.put(symbol, candle.getClose());
      }
    }

    return sortSymbols(new ArrayList<>(symCloseVal.entrySet()));
  }

  // Note:
  // Remember to confirm that you are getting same results for annualized returns
  // as in Module 3.

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Follow the instructions provided in the task documentation and fill up the
  // correct values for
  // the variables provided. First value is provided for your reference.
  // A. Put a breakpoint on the first line inside mainReadFile() which says
  // return Collections.emptyList();
  // B. Then Debug the test #mainReadFile provided in
  // PortfoliomanagerApplicationTest.java
  // following the instructions to run the test.
  // Once you are able to run the test, perform following tasks and record the
  // output as a
  // String in the function below.
  // Use this link to see how to evaluate expressions -
  // https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  // 1. evaluate the value of "args[0]" and set the value
  // to the variable named valueOfArgument0 (This is implemented for your
  // reference.)
  // 2. In the same window, evaluate the value of expression below and set it
  // to resultOfResolveFilePathArgs0
  // expression ==> resolveFileFromResources(args[0])
  // 3. In the same window, evaluate the value of expression below and set it
  // to toStringOfObjectMapper.
  // You might see some garbage numbers in the output. Dont worry, its expected.
  // expression ==> getObjectMapper().toString()
  // 4. Now Go to the debug window and open stack trace. Put the name of the
  // function you see at
  // second place from top to variable functionNameFromTestFileInStackTrace
  // 5. In the same window, you will see the line number of the function in the
  // stack trace window.
  // assign the same to lineNumberFromTestFileInStackTrace
  // Once you are done with above, just run the corresponding test and
  // make sure its working as expected. use below command to do the same.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/Users/robyjacob/crio-projects/robyjacob1998-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@5b068087";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplication.mainReadFile(String[])";
    String lineNumberFromTestFileInStackTrace = "280:1";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Now that you have the list of PortfolioTrade and their data, calculate
  // annualized returns
  // for the stocks provided in the Json.
  // Use the function you just wrote #calculateAnnualizedReturns.
  // Return the list of AnnualizedReturns sorted by annualizedReturns in
  // descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args) throws IOException, URISyntaxException {
    PortfolioTrade[] trades = getTrades(args[0]);
    String endDate = args[1];
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();

    for (PortfolioTrade trade : trades) {
      String sym = trade.getSymbol();
      LocalDate startDate = trade.getPurchaseDate();
      List<Candle> candles = getQuotes(sym, startDate, endDate);
      double buyPrice = candles.get(0).getOpen();
      double sellPrice = candles.get(candles.size() - 1).getClose();
      AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(LocalDate.parse(endDate), trade, buyPrice,
          sellPrice);
      annualizedReturns.add(annualizedReturn);
    }

    Comparator<AnnualizedReturn> annualizedReturnComparator = Comparator
        .comparing(AnnualizedReturn::getAnnualizedReturn);

    Collections.sort(annualizedReturns, annualizedReturnComparator.reversed());

    return annualizedReturns;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Return the populated list of AnnualizedReturn for all stocks.
  // Annualized returns should be calculated in two steps:
  // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  // 1.1 Store the same as totalReturns
  // 2. Calculate extrapolated annualized returns by scaling the same in years
  // span.
  // The formula is:
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  // 2.1 Store the same as annualized_returns
  // Test the same using below specified command. The build should be successful.
  // ./gradlew test --tests
  // PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {
    String symbol = trade.getSymbol();
    LocalDate startDate = trade.getPurchaseDate();
    double totalNumOfYears = (double) startDate.until(endDate, ChronoUnit.DAYS) / 365.24;

    Double totalReturns = (sellPrice - buyPrice) / buyPrice;
    Double annualizedReturns = Math.pow((1 + totalReturns), (1 / totalNumOfYears)) - 1;

    return new AnnualizedReturn(symbol, annualizedReturns, totalReturns);
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory, create PortfolioManager using
  // PortfolioManagerFactory.
  // Refer to the code from previous modules to get the List<PortfolioTrades> and
  // endDate, and
  // call the newly implemented method in PortfolioManager to calculate the
  // annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns
  // as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    PortfolioTrade[] portfolioTrades = getTrades(file);
    // ObjectMapper objectMapper = getObjectMapper();
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}
