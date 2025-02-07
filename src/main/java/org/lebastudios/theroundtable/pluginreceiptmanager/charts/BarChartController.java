package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

import java.net.URL;

public class BarChartController extends ChartController<XYChart.Series<Object, Object>>
{
    @FXML private BarChart<Object, Object> chart;

    @Override
    protected void initialize()
    {
        chart.setAnimated(false);
        chart.setCache(true);
        chart.setCacheHint(CacheHint.SPEED);
    }

    public void setVerticalLabel(String label)
    {
        chart.getYAxis().setLabel(label);
    }
    
    @Override
    public void setData(XYChart.Series<Object, Object> data)
    {
        chart.getData().clear();
        chart.getData().add(data);
    }
    
    @Override
    public URL getFXML()
    {
        return ChartController.class.getResource("barChart.fxml");
    }
}
