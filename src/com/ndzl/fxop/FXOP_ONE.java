package com.ndzl.fxop;

import com.mot.rfid.api3.*;

import java.io.*;
import java.util.HashSet;

public class FXOP_ONE {

    RFIDReader myReader = null;
    String FX_reader_serial = "";
    public Antennas antennas;

    HashSet<String> hs = new HashSet<>();

    public FXOP_ONE(){
        Connect();
        //WriteTag();

        InventorySetup();
        InventoryRun();

        Disconnect();
    }



    public static void main(String[] args) {

        new FXOP_ONE();
    }

    void Connect(){
        myReader = new RFIDReader();

        myReader.setHostName("192.168.1.10");
        myReader.setPort(5084);

        antennas = myReader.Config.Antennas;

        try {
            myReader.connect();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        //dopo connect aggiungere event handlers eventuali
        System.out.println("Reader connected!");
        System.out.println("ModelName= "+myReader.ReaderCapabilities.getModelName());
        FX_reader_serial = myReader.ReaderCapabilities.ReaderID.getID();
        System.out.println("SerialNo= "+FX_reader_serial);
        System.out.println("FirwareVersion="+myReader.ReaderCapabilities.getFirwareVersion());

    }

    void Disconnect(){
        try {
            myReader.disconnect();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    void WriteTag(){

        try {
            Antennas.Config antennaConfig = myReader.Config.Antennas.getAntennaConfig(2);

            System.out.println("CURRENT ANTENNA 2 POWER: "+ antennaConfig.getTransmitPowerIndex());

            antennaConfig.setTransmitPowerIndex((short)2);



        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }


        TagAccess tagAccess = new TagAccess();
        TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();

        MEMORY_BANK memBank = MEMORY_BANK.MEMORY_BANK_EPC;
        writeAccessParams.setMemoryBank(memBank);

        try {
            byte[] writeData = hexStringToByteArray("5555");
            writeAccessParams.setWriteData(writeData);
            writeAccessParams.setWriteDataLength(2);
            writeAccessParams.setByteOffset(14);
            writeAccessParams.setAccessPassword(0);

            myReader.Actions.TagAccess.writeWait("313830303030303130304444", writeAccessParams,  null);

        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

    }


    PrintWriter pw;
    public class EventsHandler implements RfidEventsListener
    {
        public EventsHandler()
        {
            try {
                pw = new PrintWriter("./rfid_data.txt");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        TagData[]  myTags = null;
        public void eventReadNotify(RfidReadEvents rre) {

            myTags = myReader.Actions.getReadTags(5);
            for (int index = 0; index < myTags.length; index++)
            {
                TagData tag = myTags[index];
                String key = tag.getTagID();
                String tag_antenna = ""+tag.getAntennaID();
                String tag_PC = ""+tag.getPC();
                String tag_RSSI = ""+tag.getPeakRSSI();
                String tag_membankdata = tag.getMemoryBankData();

                String tagdata = FX_reader_serial+"-"+index+/*tag_time.ConvertTimetoString()+*/",tid:"+key+",ant:"+tag_antenna+",rssi:"+tag_RSSI+",pc:"+tag_PC ;
                //String tagdata = FX_reader_serial+"-epc:"+key;

                //System.out.println("<"+tagdata+">");
                pw.println(tagdata);
                hs.add(tagdata);
            }

        }

    public void eventStatusNotify(RfidStatusEvents rse)
    {

    }

    }
    private EventsHandler eventsHandler = new EventsHandler();
    void InventorySetup(){
        myReader.Events.setInventoryStartEvent(true);
        myReader.Events.setInventoryStopEvent(true);
        myReader.Events.setTagReadEvent(true);
        myReader.Events.setAttachTagDataWithReadEvent(false);
        myReader.Events.addEventsListener(eventsHandler);

    }

    void InventoryRun()
    {
        //file:///C:/Program%20Files/Zebra%20FXSeries%20Host%20Java%20SDK/RFID/doc/ProgrammersGuide/Generic%20Reader%20Interface/Basic%20Operations.htm
        System.out.println("Press Enter to stop inventory");
        try {
            myReader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        try
        {  BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
           br.readLine();
           br.close();
           pw.flush();
           pw.close();
        }
        catch(Exception /*IOException*/ioex)
        {            System.out.println("IO Exception.Stopping inventory");       }
        finally
        {

            try {
                myReader.Actions.Inventory.stop();
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException e) {
                e.printStackTrace();
            }


        }

    }
}


