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

import com.assets.derivates.service.ComputeEntranceService;
import com.assets.derivates.service.ComputeExitsService;
import com.assets.derivates.service.impl.ComputeBasicEntranceServiceImpl;
import com.assets.derivates.service.impl.ComputeExitsServicePercentAndDynamicStopLoss;
import com.assets.portfolio.correlation.entities.investment.InvestmentAction;
import com.assets.portfolio.correlation.entities.investment.InvestmentActionEnum;
import com.assets.portfolio.correlation.entities.investment.InvestmentActions;
import com.assets.portfolio.correlation.entities.stock.StockList;
import com.assets.portfolio.data.loader.DataLoader;
import com.assets.portfolio.data.loader.impl.DataLoaderCsv;
import com.assets.trades.service.impl.BuyFixedAmountOfMoney;

public class DerivateGraph {

    private static final Logger logger = Logger.getLogger(DerivateGraph.class);

    private static final String DEFAULT_PATH = "C:\\Users\\00556998\\Downloads\\quantquote_daily_sp500_83986\\daily";
    private static final String DEFAULT_OUTFILE = "C:\\Users\\00556998\\Downloads\\quantquote_daily_sp500_83986\\meansAndDerivates\\";

    private static final String[] TICKERS = {"XRX"};
    private static final String FROM = "2010-01-01T00:00:00Z";
    private static final String TO = "2011-06-01T00:00:00Z";
    
    private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static void main(String[] args) throws FileNotFoundException {
        
        DataLoader loader = new DataLoaderCsv(DEFAULT_PATH);
        Map<String, StockList> data = loader.loadData();
        logger.info("Data loaded");

        for(String ticker : TICKERS){
            StockList inputData = new StockList(data.get(ticker).parallelStream().filter(p -> (p.getInstant().compareTo(Instant.parse(FROM)) >= 0 && p.getInstant().compareTo(Instant.parse(TO)) <= 0)).sequential().collect(Collectors.toList()), ticker);
            computeTicker(inputData);
        }

    }

    private static void computeTicker(StockList inputData){
        ComputeEntranceService entryService = new ComputeBasicEntranceServiceImpl();
        ComputeExitsService exitService = new ComputeExitsServicePercentAndDynamicStopLoss();
        
        StockList entryPoints = entryService.computeEntrances(inputData);
        StockList exitPoints = exitService.computeExits(entryPoints, inputData);
        
        InvestmentActions actions = new InvestmentActions(entryPoints, exitPoints, new BuyFixedAmountOfMoney());
        
        printResults(actions, inputData);
        printGraph(actions, inputData, new File(DEFAULT_OUTFILE + inputData.getTicker() +".csv"));
    }
    
    private static void printResults(InvestmentActions actions, StockList inputData) {
        actions.getActions().forEach(x -> System.out.println(String.format("%s - %s(%s)@%s$ at %s", x.getTicker(), x.getAction(), x.getSharesAmount(), x.getPrice().setScale(2, RoundingMode.HALF_DOWN), x.getInstant())));
        BigDecimal firstValue = inputData.getFirst().getValue();
        BigDecimal lastValue = inputData.getLast().getValue();
        BigDecimal benefit = lastValue.subtract(firstValue).divide(firstValue, 5, RoundingMode.HALF_DOWN).multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_DOWN);
        
        System.out.println(String.format("Algorithm result: %s%% against B&S: %s%%", actions.getPercentBenefitSellingWith(inputData.getLast()).toString(), benefit.toString()));
    }
    
    private static void printGraph(InvestmentActions actions, StockList inputData, File outfile) {
        try{
            PrintStream printer = new PrintStream(outfile);
            StockList[] values = new StockList[7];
            List<Instant> instants = inputData.stream().map(x -> x.getInstant()).collect(Collectors.toList());
            values[0] = inputData;
            values[1] = inputData.getMeanWeek();
            values[2] = inputData.getMeanMonth();
            values[3] = inputData.getFirstDerivate();
            values[4] = values[1].getFirstDerivate();
            values[5] = values[2].getFirstDerivate();
            values[6] = inputData.getFirstDerivate().getFirstDerivate();
            print("Date, Price, Mean Week, Mean Month, Derivate, Derivate Week, Derivate Month, Action, ActPrice, 2ndDerivateDaily", printer, instants, values, actions);
        }catch(FileNotFoundException fnfe){
            fnfe.printStackTrace();
        }
    }

    private static void print(final String header, final PrintStream printer, List<Instant> instants, StockList[] elements, InvestmentActions actions) {
        printer.println(header);
        for (Instant instant : instants) {
            InvestmentAction action = actions.getAction(instant);
            printer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", 
                    df.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()),
                    getValueString(elements[0], instant),
                    getValueString(elements[1], instant),
                    getValueString(elements[2], instant),
                    getValueString(elements[3], instant),
                    getValueString(elements[4], instant),
                    getValueString(elements[5], instant),
                    action != null ? action.getAction().equals(InvestmentActionEnum.SELL) ? -1 : 1 : 0,
                    action != null ? action.getPrice().setScale(2, RoundingMode.HALF_DOWN).toString() : "",
                    getValueString(elements[6], instant)
            ));
        }
    }
    
    private static String getValueString(StockList element, Instant instant){
        return element.getByInstant(instant) != null ? element.getByInstant(instant).getValue().setScale(4, RoundingMode.HALF_DOWN).toString() : "";
    }
}
