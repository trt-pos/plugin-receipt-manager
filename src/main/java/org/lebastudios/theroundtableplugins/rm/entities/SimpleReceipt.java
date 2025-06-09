package org.lebastudios.theroundtableplugins.rm.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.locale.Translator;
import org.lebastudios.theroundtableplugins.cr.PluginCashRegisterEvents;
import org.lebastudios.theroundtableplugins.cr.entities.Receipt;

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
        
        
        return Translator.getInstance().t("rm:word.receipt") + " " + this.getId() + billNumber
                + " - " + this.getDate().toLocalDate()
                + " " + Translator.getInstance().t("rm:word.at")
                + " " + this.getDate().toLocalTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
