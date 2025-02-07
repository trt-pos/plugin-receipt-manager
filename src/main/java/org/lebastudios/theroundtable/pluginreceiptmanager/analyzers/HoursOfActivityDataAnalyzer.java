package org.lebastudios.theroundtable.pluginreceiptmanager.analyzers;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import org.lebastudios.theroundtable.pluginreceiptmanager.charts.BarChartController;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HoursOfActivityDataAnalyzer implements IDataAnalyzer
{
    @Override
    public Node getDataChart(ObservableList<SimpleReceipt> simpleReceipts)
    {
        final Map<Integer, Integer> receiptsPerHour = new HashMap<>();
        AtomicInteger totalReceipts = new AtomicInteger();

        IDataAnalyzer.fastIterateOverReceipts(simpleReceipts, receipt ->
        {
            var hour = receipt.getTransaction().getDate().getHour();
            receiptsPerHour.put(hour, receiptsPerHour.getOrDefault(hour, 0) + 1);
            totalReceipts.getAndIncrement();
        });

        XYChart.Series<Object, Object> series = new XYChart.Series<>();

        for (int i = 0; i < 24; i++)
        {
            if (!receiptsPerHour.containsKey(i)) continue;

            series.getData().add(new XYChart.Data<>(
                    i + ":00",
                    new BigDecimal(receiptsPerHour.get(i)).divide(new BigDecimal(totalReceipts.get()), 2,
                            RoundingMode.HALF_UP)
            ));
        }

        final var barChartController = new BarChartController();
        barChartController.setData(series);
        barChartController.setVerticalLabel("%");
        
        return barChartController.getRoot();
    }
}
