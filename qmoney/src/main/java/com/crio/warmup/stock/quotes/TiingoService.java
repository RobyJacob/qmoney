
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
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
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    if (to.isBefore(from))
      throw new RuntimeException();

    String apiUrl = this.buildUri(symbol, from, to);

    System.out.println(apiUrl);

    Candle[] quotes = this.restTemplate.getForObject(apiUrl, TiingoCandle[].class);

    List<Candle> candles = Arrays.asList(quotes);

    Collections.sort(candles, this.getComparator());

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

}
