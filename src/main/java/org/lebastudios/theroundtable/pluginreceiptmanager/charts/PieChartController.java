package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import javafx.collections.ObservableList;
import javafx.scene.CacheHint;
import javafx.scene.chart.PieChart;

public class PieChartController extends ChartController<ObservableList<PieChart.Data>, PieChart>
{
    @Override
    protected PieChart charInit()
    {
        PieChart chart = new PieChart();

        chart.setLegendVisible(false);
        chart.maxHeight(Double.MAX_VALUE);
        chart.maxWidth(Double.MAX_VALUE);

        chart.setAnimated(false);
        chart.setCache(true);
        chart.setCacheHint(CacheHint.SPEED);

        chart.setLabelLineLength(10);
        
        return chart;
    }

    @Override
    public void setData(ObservableList<PieChart.Data> data)
    {
        chart.setData(data);
    }
}
