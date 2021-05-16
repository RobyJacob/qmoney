
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerFactory {

  @Deprecated
  public static PortfolioManager getPortfolioManager(RestTemplate restTemplate) {

    return new PortfolioManagerImpl(restTemplate);
  }

  public static PortfolioManager getPortfolioManager(String provider, RestTemplate restTemplate) {
    StockQuotesService stockService = StockQuoteServiceFactory.INSTANCE.getService(provider, restTemplate);

    return new PortfolioManagerImpl(stockService, restTemplate);
  }

}
