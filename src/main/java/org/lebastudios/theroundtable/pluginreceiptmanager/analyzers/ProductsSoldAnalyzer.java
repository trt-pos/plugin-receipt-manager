package org.lebastudios.theroundtable.pluginreceiptmanager.analyzers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.entities.Product_Receipt;
import org.lebastudios.theroundtable.pluginreceiptmanager.charts.PieChartController;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductsSoldAnalyzer implements IDataAnalyzer
{
    @Override
    public Node getDataChart(ObservableList<SimpleReceipt> simpleReceipts)
    {
        final Map<String, Float> productsSold = new HashMap<>();

        Database.getInstance().connectQuery(session ->
        {
            List<Product_Receipt> prods = session.createQuery("from Product_Receipt " +
                            "where receipt.id in (:ids) " +
                            "and receipt.id not in (select rm.superReceipt.id from ReceiptModification rm)",
                            Product_Receipt.class)
                    .setParameterList("ids", simpleReceipts.stream().map(SimpleReceipt::getId).toList())
                    .getResultList();
            
            IDataAnalyzer.consumeMultiThreaded(prods, p ->
            {
                final var product = p.getProduct();
                final var productIdentifier = product.getName() + ":&:" + product.getId();

                synchronized (productsSold)
                {
                    productsSold.put(productIdentifier,
                            productsSold.getOrDefault(productIdentifier, 0f) + p.getQuantity().floatValue()
                    );
                }
            }, 100);
        });

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        productsSold.forEach((k, v) ->
        {
            var name = k.split(":&:")[0];
            final var qty = v.intValue();
            
            name = name.substring(0, Math.min(name.length(), 10)) + " (" + qty + ")";
            pieChartData.add(new PieChart.Data(name, qty));
        });

        final var pieChartController = new PieChartController();
        pieChartController.setData(pieChartData);
        
        return pieChartController.getRoot();
    }
}
