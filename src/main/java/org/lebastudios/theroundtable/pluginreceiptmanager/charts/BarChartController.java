package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.lebastudios.theroundtable.locale.Translator;

public class BarChartController extends ChartController<XYChart.Series<String, Number>, BarChart<String, Number>>
{
    @Override
    protected BarChart<String, Number> charInit()
    {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(Translator.getInstance().t("rm:word.hours"));
        xAxis.setSide(Side.BOTTOM);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setSide(Side.LEFT);
        yAxis.setTickMarkVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);

        chart.setLegendVisible(false);
        chart.maxWidth(Double.MAX_VALUE);
        chart.maxHeight(Double.MAX_VALUE);

        chart.setAnimated(false);
        chart.setCache(true);
        chart.setCacheHint(CacheHint.SPEED);
        
        return chart;
    }

    public void setVerticalLabel(String label)
    {
        chart.getYAxis().setLabel(label);
    }
    
    @Override
    public void setData(XYChart.Series<String, Number> data)
    {
        chart.getData().clear();
        chart.getData().add(data);
    }
}
