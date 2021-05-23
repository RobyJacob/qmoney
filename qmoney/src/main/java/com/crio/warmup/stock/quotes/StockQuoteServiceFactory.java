
package com.crio.warmup.stock.quotes;

import org.springframework.web.client.RestTemplate;

public enum StockQuoteServiceFactory {

  INSTANCE;

  public StockQuotesService getService(String provider, RestTemplate restTemplate) {

    if (provider != null && provider.equalsIgnoreCase("tiingo")) {
      return new TiingoService(restTemplate);
    }

    return new AlphavantageService(restTemplate);
  }
}
