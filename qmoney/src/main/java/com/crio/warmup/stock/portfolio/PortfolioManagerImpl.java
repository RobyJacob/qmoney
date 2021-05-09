
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {
  private final String API_TOKEN = "4dd7b0eb5e86af400d5c10e372ab0a2e49ec7bfb";
  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from
  // main anymore.
  // Copy your code from Module#3
  // PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the
  // method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required
  // further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command
  // below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    List<Candle> quotes = new ArrayList<>();

    for (PortfolioTrade trade : portfolioTrades) {
      String symbol = trade.getSymbol();
      LocalDate startDate = trade.getPurchaseDate();
      double totalNumOfYears = (double) startDate.until(endDate, DAYS) / 365.24;
      try {
        quotes = this.getStockQuote(symbol, startDate, endDate);
      } catch (JsonProcessingException ex) {
        System.out.println("Exception while processing Json");
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

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    Map<String, String> map = new HashMap<>();
    map.put("sym", symbol);
    map.put("token", this.API_TOKEN);
    map.put("endDate", to.toString());
    map.put("startDate", from.toString());

    if (to.isBefore(from))
      throw new RuntimeException();

    String apiUrl = this.buildUri(symbol, from, to);

    Candle[] quotes = this.restTemplate.getForObject(apiUrl, TiingoCandle[].class, map);

    return Arrays.asList(quotes);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate = String.format(
        "https:api.tiingo.com/tiingo/daily/%s/prices?" + "startDate=%s&endDate=%s&token=%s", symbol,
        startDate.toString(), endDate.toString(), this.API_TOKEN);

    return uriTemplate;
  }
}
