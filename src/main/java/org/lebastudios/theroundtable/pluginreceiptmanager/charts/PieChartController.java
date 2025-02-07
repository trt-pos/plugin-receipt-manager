package org.lebastudios.theroundtable.pluginreceiptmanager.charts;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.chart.PieChart;

import java.net.URL;

public class PieChartController extends ChartController<ObservableList<PieChart.Data>>
{
    @FXML private PieChart chart;

    @Override
    protected void initialize()
    {
        chart.setAnimated(false);
        chart.setCache(true);
        chart.setCacheHint(CacheHint.SPEED);
        
        chart.setLabelLineLength(10);
    }

    @Override
    public void setData(ObservableList<PieChart.Data> data)
    {
        chart.setData(data);
    }

    @Override
    public URL getFXML()
    {
        return ChartController.class.getResource("pieChart.fxml");
    }
}
