
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;
  private final String API_TOKEN = "4dd7b0eb5e86af400d5c10e372ab0a2e49ec7bfb";

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException, JsonProcessingException, RuntimeException {
    if (to.isBefore(from))
      throw new RuntimeException();

    String apiUrl = this.buildUri(symbol, from, to);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    String response;
    Candle[] quotes = null;

    try {
      response = this.restTemplate.getForObject(apiUrl, String.class);
      quotes = objectMapper.readValue(response, TiingoCandle[].class);
    } catch (NullPointerException e) {
      throw new StockQuoteServiceException("Error occured while requesting Tiingo API", e.getCause());
    }

    List<Candle> candles = Arrays.asList(quotes);

    Collections.sort(candles, getComparator());

    return candles;
  }

  private Comparator<Candle> getComparator() {
    return Comparator.comparing(Candle::getDate);
  }

  @Override
  public String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uriTemplate = String.format("https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
        symbol, startDate.toString(), endDate.toString(), this.API_TOKEN);

    return uriTemplate;
  }

  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  // 1. Update the method signature to match the signature change in the
  // interface.
  // Start throwing new StockQuoteServiceException when you get some invalid
  // response from
  // Tiingo, or if Tiingo returns empty results for whatever reason, or you
  // encounter
  // a runtime exception during Json parsing.
  // 2. Make sure that the exception propagates all the way from
  // PortfolioManager#calculateAnnualisedReturns so that the external user's of
  // our API
  // are able to explicitly handle this exception upfront.

  // CHECKSTYLE:OFF

}
