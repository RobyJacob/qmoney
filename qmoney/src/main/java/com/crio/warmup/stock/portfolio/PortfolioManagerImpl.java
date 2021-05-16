
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
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
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {
  private RestTemplate restTemplate;
  private StockQuotesService stockService;

  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  protected PortfolioManagerImpl(StockQuotesService stockService, RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.stockService = stockService;
  }

  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    List<Candle> quotes = new ArrayList<>();

    for (PortfolioTrade trade : portfolioTrades) {
      String symbol = trade.getSymbol();
      LocalDate startDate = trade.getPurchaseDate();
      double totalNumOfYears = (double) startDate.until(endDate, DAYS) / 365.24;
      try {
        quotes = this.stockService.getStockQuote(symbol, startDate, endDate);
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
