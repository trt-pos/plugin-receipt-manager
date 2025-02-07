package org.lebastudios.theroundtable.pluginreceiptmanager.analyzers;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.XYChart;
import org.lebastudios.theroundtable.pluginreceiptmanager.charts.BarChartController;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;

import java.util.HashMap;
import java.util.Map;

public class IncomeDataAnalyzer implements IDataAnalyzer
{
    @Override
    public Node getDataChart(ObservableList<SimpleReceipt> simpleReceipts)
    {
        Map<Integer, Float> incomePerHour = new HashMap<>();

        IDataAnalyzer.fastIterateOverReceipts(simpleReceipts, receipt ->
        {
            var hour = receipt.getTransaction().getDate().getHour();
            incomePerHour.put(
                    hour,
                    incomePerHour.getOrDefault(hour, 0f) + receipt.getTaxedTotal().floatValue()
            );
        });

        XYChart.Series<Object, Object> series = new XYChart.Series<>();

        for (int i = 0; i < 24; i++)
        {
            if (!incomePerHour.containsKey(i)) continue;

            series.getData().add(new XYChart.Data<>(
                    i + ":00",
                    incomePerHour.get(i)
            ));
        }

        final var barChartController = new BarChartController();
        barChartController.setVerticalLabel("€");
        barChartController.setData(series);
        
        return barChartController.getRoot();
    }
}
