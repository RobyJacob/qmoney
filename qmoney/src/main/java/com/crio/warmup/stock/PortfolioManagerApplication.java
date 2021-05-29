
package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.quotes.AlphavantageService;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
import java.util.logging.Logger;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  private static RestTemplate restTemplate = new RestTemplate();
  private static final String API_TOKEN = "4dd7b0eb5e86af400d5c10e372ab0a2e49ec7bfb";
  private static String apiUrl = "https://api.tiingo.com/tiingo/daily/{sym}/prices?startDate={startDate}&endDate={endDate}&token={token}";

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

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/Users/robyjacob/crio-projects/robyjacob1998-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@5b068087";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplication.mainReadFile(String[])";
    String lineNumberFromTestFileInStackTrace = "280:1";

    return Arrays.asList(new String[] { valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
        functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace });
  }

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

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {
    String symbol = trade.getSymbol();
    LocalDate startDate = trade.getPurchaseDate();
    double totalNumOfYears = (double) startDate.until(endDate, ChronoUnit.DAYS) / 365.24;

    Double totalReturns = (sellPrice - buyPrice) / buyPrice;
    Double annualizedReturns = Math.pow((1 + totalReturns), (1 / totalNumOfYears)) - 1;

    return new AnnualizedReturn(symbol, annualizedReturns, totalReturns);
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args) throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    PortfolioTrade[] portfolioTrades = getTrades(file);
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);

    return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  public static void main(String[] args) throws Exception {
    // Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    // ThreadContext.put("runId", UUID.randomUUID().toString());

    // printJsonObject(mainCalculateReturnsAfterRefactor(args));
    StockQuotesService service = StockQuoteServiceFactory.INSTANCE.getService(null, restTemplate);
    System.out.println(service.getStockQuote("AAPL", LocalDate.parse("2019-01-01"), LocalDate.parse("2019-01-04")));
  }
}
