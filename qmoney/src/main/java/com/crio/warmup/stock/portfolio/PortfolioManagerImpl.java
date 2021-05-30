
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.cglib.core.Local;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {
  private RestTemplate restTemplate;
  private StockQuotesService stockService;
  private ExecutorService threadPool = null;

  protected PortfolioManagerImpl(StockQuotesService stockService, RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.stockService = stockService;
  }

  protected PortfolioManagerImpl(StockQuotesService stockService) {
    this(stockService, new RestTemplate());
  }

  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws StockQuoteServiceException {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    List<Candle> quotes = new ArrayList<>();

    for (PortfolioTrade trade : portfolioTrades) {
      String symbol = trade.getSymbol();
      LocalDate startDate = trade.getPurchaseDate();
      double totalNumOfYears = (double) startDate.until(endDate, DAYS) / 365.24;
      try {
        quotes = this.stockService.getStockQuote(symbol, startDate, endDate);
      } catch (StockQuoteServiceException e) {
        throw e;
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      double buyPrice = quotes.get(0).getOpen();
      double sellPrice = quotes.get(quotes.size() - 1).getClose();

      Double totalReturns = (sellPrice - buyPrice) / buyPrice;
      Double annualizedReturn = Math.pow((1 + totalReturns), (1 / totalNumOfYears)) - 1;

      annualizedReturns.add(new AnnualizedReturn(symbol, annualizedReturn, totalReturns));
    }

    Collections.sort(annualizedReturns, this.getComparator());

    return annualizedReturns;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    List<Callable<List<Object>>> callableTasks = new ArrayList<>();

    if (threadPool == null)
      threadPool = Executors.newFixedThreadPool(numThreads);

    for (PortfolioTrade trade : portfolioTrades) {
      String symbol = trade.getSymbol();
      LocalDate startDate = trade.getPurchaseDate();

      callableTasks.add(() -> {
        List<Candle> quotes = stockService.getStockQuote(symbol, startDate, endDate);

        return Arrays.asList(quotes, symbol, startDate);
      });
    }

    List<Future<List<Object>>> futureTasks = threadPool.invokeAll(callableTasks);

    for (Future<List<Object>> task : futureTasks) {
      LocalDate startDate = LocalDate.now();
      String symbol = "";
      List<Candle> quotes = new ArrayList<>();

      try {
        quotes = (List<Candle>) task.get().get(0);
        symbol = (String) task.get().get(1);
        startDate = (LocalDate) task.get().get(2);
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException(e.getMessage());
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      double totalNumOfYears = (double) startDate.until(endDate, DAYS) / 365.24;
      double buyPrice = quotes.get(0).getOpen();
      double sellPrice = quotes.get(quotes.size() - 1).getClose();

      Double totalReturns = (sellPrice - buyPrice) / buyPrice;
      Double annualizedReturn = Math.pow((1 + totalReturns), (1 / totalNumOfYears)) - 1;

      annualizedReturns.add(new AnnualizedReturn(symbol, annualizedReturn, totalReturns));
    }

    Collections.sort(annualizedReturns, this.getComparator());

    threadPool.shutdown();
    try {
      if (!threadPool.awaitTermination(800, TimeUnit.MILLISECONDS)) {
        threadPool.shutdownNow();
      }
    } catch (InterruptedException e) {
      threadPool.shutdownNow();
    }

    return annualizedReturns;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    if (to.isBefore(from))
      throw new RuntimeException();

    String apiUrl = this.buildUri(symbol, from, to);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    String response = restTemplate.getForObject(apiUrl, String.class);

    Candle[] quotes = objectMapper.readValue(response, TiingoCandle[].class);

    List<Candle> candles = Arrays.asList(quotes);

    Comparator<Candle> comparator = Comparator.comparing(Candle::getDate);

    Collections.sort(candles, comparator);

    return candles;
  }

  public String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate = String.format("https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
        symbol, startDate.toString(), endDate.toString(), "4dd7b0eb5e86af400d5c10e372ab0a2e49ec7bfb");

    return uriTemplate;
  }
}
