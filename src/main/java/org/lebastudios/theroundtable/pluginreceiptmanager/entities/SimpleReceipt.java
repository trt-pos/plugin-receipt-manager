package org.lebastudios.theroundtable.pluginreceiptmanager.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.locale.LangFileLoader;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;
import org.lebastudios.theroundtable.plugincashregister.entities.Receipt;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SimpleReceipt
{
    private int id;
    private LocalDateTime date = LocalDateTime.now();
    private Receipt.Status status;
    
    private String billNumber = null;

    public SimpleReceipt(int id, LocalDateTime date, Receipt.Status status)
    {
        this.id = id;
        this.date = date;
        this.status = status;
    }

    @Override
    public String toString()
    {
        if (billNumber == null) 
        {
            StringBuffer billNumberSb = new StringBuffer();

            PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(this.getId(), billNumberSb);

            billNumber = billNumberSb.isEmpty() ? "" : (" (" + billNumberSb + ")");
        }
        
        
        return LangFileLoader.getTranslation("word.receipt") + " " + this.getId() + billNumber
                + " - " + this.getDate().toLocalDate()
                + " " + LangFileLoader.getTranslation("word.at")
                + " " + this.getDate().toLocalTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
