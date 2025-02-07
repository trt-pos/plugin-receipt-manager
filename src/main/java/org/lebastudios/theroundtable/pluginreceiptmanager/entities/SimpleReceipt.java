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
    
    @Override
    public String toString()
    {
        StringBuffer billNumber = new StringBuffer();

        PluginCashRegisterEvents.onRequestReceiptBillNumber.invoke(this.getId(), billNumber);
        
        String receiptId = this.getId() + (billNumber.isEmpty() ? "" : (" (" + billNumber + ")"));
        
        return LangFileLoader.getTranslation("word.receipt") + " " + receiptId
                + " - " + this.getDate().toLocalDate()
                + " " + LangFileLoader.getTranslation("word.at")
                + " " + this.getDate().toLocalTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
