
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private final String API_KEY = "VXBYLHXMONMH1YMH";
  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException {
    String url = buildUri(symbol, from, to);
    List<Candle> candles = new ArrayList<>();

    if (to.isBefore(from))
      throw new RuntimeException();

    AlphavantageDailyResponse alphavantageDailyResponse = this.restTemplate.getForObject(url,
        AlphavantageDailyResponse.class);

    Map<LocalDate, AlphavantageCandle> responseCandles = alphavantageDailyResponse.getCandles();

    for (Map.Entry<LocalDate, AlphavantageCandle> entry : responseCandles.entrySet()) {
      LocalDate keyDate = entry.getKey();
      if ((keyDate.isEqual(from) || keyDate.isEqual(to)) || (keyDate.isAfter(from) && keyDate.isBefore(to))) {
        AlphavantageCandle candle = entry.getValue();
        candle.setDate(keyDate);
        candles.add(candle);
      }
    }

    Collections.sort(candles, this.getComparator());

    return candles;
  }

  private Comparator<Candle> getComparator() {
    return Comparator.comparing(Candle::getDate);
  }

  @Override
  public String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String urlTemplate = String.format(
        "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY_ADJUSTED&symbol=%s&outputsize=full&apikey=%s",
        symbol, this.API_KEY);

    return urlTemplate;
  }
}
