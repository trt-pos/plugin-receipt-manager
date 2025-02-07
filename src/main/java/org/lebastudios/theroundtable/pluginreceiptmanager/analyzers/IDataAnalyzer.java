package org.lebastudios.theroundtable.pluginreceiptmanager.analyzers;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;
import org.lebastudios.theroundtable.pluginreceiptmanager.entities.SimpleReceipt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface IDataAnalyzer
{
    Node getDataChart(ObservableList<SimpleReceipt> simpleReceipts);
    
    static void fastIterateOverReceipts(List<SimpleReceipt> simpleReceipts, Consumer<Receipt> receiptConsumer)
    {
        Database.getInstance().connectQuery(session ->
        {
            var list = session.createQuery("from Receipt " +
                                    "where id in (:ids) " +
                                    "and id not in (select rm.superReceipt.id from ReceiptModification rm)",
                            Receipt.class)
                    .setParameterList("ids", simpleReceipts.stream().map(SimpleReceipt::getId).toList())
                    .getResultList();
            
            consumeMultiThreaded(list, receiptConsumer, 100);
        });
    }
    
    static <T> void consumeMultiThreaded(List<T> items, Consumer<T> consumer, int minItems)
    {
        int cores = Runtime.getRuntime().availableProcessors();

        if (cores > 1 && items.size() > minItems)
        {
            int size = items.size();
            int chunk = size / cores;
            
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < cores; i++)
            {
                int start = i * chunk;
                int end = (i + 1) * chunk;

                if (i == cores - 1)
                {
                    end = size;
                }

                int finalEnd = end;
                final var thread = new Thread(() ->
                {
                    for (int j = start; j < finalEnd; j++)
                    {
                        consumer.accept(items.get(j));
                    }
                });
                
                threads.add(thread);
                thread.start();
            }

            for (Thread thread : threads)
            {
                try
                {
                    thread.join();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            for (T item : items)
            {
                consumer.accept(item);
            }
        }
    }
}
