package com.assets.derivates;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.assets.derivates.service.impl.ComputeEntranceServiceImpl;
import com.assets.derivates.service.impl.ComputeExitsServiceImpl;
import com.assets.portfolio.correlation.entities.investment.InvestmentAction;
import com.assets.portfolio.correlation.entities.investment.InvestmentActionEnum;
import com.assets.portfolio.correlation.entities.investment.InvestmentActions;
import com.assets.portfolio.correlation.entities.stock.StockList;
import com.assets.portfolio.correlation.entities.stock.StockPrice;
import com.assets.portfolio.data.loader.DataLoader;
import com.assets.portfolio.data.loader.impl.DataLoaderCsv;

public class DerivateGraph {

    private static final Logger logger = Logger.getLogger(DerivateGraph.class);

    private static final String DEFAULT_PATH = "C:\\Users\\00556998\\Downloads\\quantquote_daily_sp500_83986\\daily";
    private static final String DEFAULT_OUTFILE = "C:\\Users\\00556998\\Downloads\\quantquote_daily_sp500_83986\\meansAndDerivates\\";

    private static final String TICKER = "FSLR";
    private static final String FROM = "2010-01-01T00:00:00Z";
    private static final String TO = "2012-01-01T00:00:00Z";
    
    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static void main(String[] args) throws FileNotFoundException {
        DataLoader loader = new DataLoaderCsv(DEFAULT_PATH);
        Map<String, StockList> data = loader.loadData();
        logger.info("Data loaded");

        
        StockList entryPoints = computeEntrance(data, TICKER, Instant.parse(FROM), Instant.parse(TO));
        StockList exitPoints = computeExits(data, TICKER, entryPoints);
        
        InvestmentActions actions = computeActions(entryPoints, exitPoints, TICKER);
        
        actions.getActions().forEach(x -> System.out.println(String.format("%s - %s(%s)@%s$ at %s", x.getTicker(), x.getAction(), x.getSharesAmount(), x.getPrice().setScale(2, RoundingMode.HALF_DOWN), x.getInstant())));
        
        System.out.println("Final benefit: " + actions.getPercentBenefit().toString() + "%");
        
        StockList dateFiltteredInput = new StockList(data.get(TICKER).parallelStream().filter(p -> p.getInstant().compareTo(Instant.parse(FROM)) >= 0).sequential().collect(Collectors.toList()), TICKER);
        BigDecimal firstValue = dateFiltteredInput.getFirst().getValue();
        BigDecimal lastValue = dateFiltteredInput.getLast().getValue();
        BigDecimal benefit = lastValue.subtract(firstValue).divide(firstValue, 5, RoundingMode.HALF_DOWN).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
        System.out.println(String.format("Benefit buy beggining and sell end: %s%%", benefit.toString()));
//        printDerivateGraph(data, "INTC");
    }

    private static InvestmentActions computeActions(StockList entryPoints, StockList exitPoints, String string) {
        StockList allActions = new StockList("INTC");
        allActions.addAll(entryPoints);
        allActions.addAll(exitPoints);
        
        allActions.sort( (x, y) -> x.getInstant().compareTo(y.getInstant()) );
        
        InvestmentActions actions = new InvestmentActions();
        int currentStocksInPossession = 0;
        for(StockPrice price : allActions){
            if(entryPoints.contains(price)){
                actions.add(new InvestmentAction(price, InvestmentActionEnum.BUY, 1));
                currentStocksInPossession++;
            }else{
                actions.add(new InvestmentAction(price, InvestmentActionEnum.SELL, currentStocksInPossession));
                currentStocksInPossession = 0;
            }
        }
        return actions;
    }

    private static StockList computeExits(Map<String, StockList> data, String ticker, StockList entryPoints) {
        return new ComputeExitsServiceImpl().computeExits(entryPoints, data.get(ticker));
    }

    private static StockList computeEntrance(Map<String, StockList> data, String value, final Instant from, final Instant to) {
        StockList dateFiltteredInput = new StockList(data.get(value).parallelStream().filter(p -> (p.getInstant().compareTo(from) >= 0 && p.getInstant().compareTo(to) <= 0)).sequential().collect(Collectors.toList()), value);
        return new ComputeEntranceServiceImpl().computeEntrances(dateFiltteredInput);
    }

    private static void printDerivateGraph(final Map<String, StockList> data, String value) throws FileNotFoundException {
        StockList intc = data.get(value);
        PrintStream printer = new PrintStream(new File(DEFAULT_OUTFILE + "INTC.csv"));
        StockList[] values = new StockList[6];
        List<Instant> instants = intc.stream().map(x -> x.getInstant()).collect(Collectors.toList());

        values[0] = intc;
        values[1] = intc.getMean(7);
        values[2] = intc.getMean(30);
        values[3] = intc.getFirstDerivate();
        values[4] = values[1].getFirstDerivate();
        values[5] = values[2].getFirstDerivate();

        print("Date, Price, Mean Week, Mean Month, Derivate, Derivate Week, Derivate Month", printer, instants, values);
        printer.close();
    }

    private static void print(final String header, final PrintStream printer, List<Instant> instants, StockList[] elements) {
        printer.println(header);
        for (Instant instant : instants) {
            printer.println(String.format("%s,%s,%s,%s,%s,%s,%s", 
                    df.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()),
                    getValueString(elements[0], instant),
                    getValueString(elements[1], instant),
                    getValueString(elements[2], instant),
                    getValueString(elements[3], instant),
                    getValueString(elements[4], instant),
                    getValueString(elements[5], instant)
            ));
        }
    }
    
    private static String getValueString(StockList element, Instant instant){
        return element.getByInstant(instant) != null ? element.getByInstant(instant).getValue().setScale(2, RoundingMode.HALF_DOWN).toString() : "";
    }
}
