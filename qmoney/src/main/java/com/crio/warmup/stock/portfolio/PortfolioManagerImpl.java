
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.ArrayList;
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
}
