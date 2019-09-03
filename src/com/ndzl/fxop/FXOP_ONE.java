package com.ndzl.fxop;

import com.mot.rfid.api3.*;

import java.io.*;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import static javafx.application.Platform.exit;

public class FXOP_ONE {

    RFIDReader myReader = null;
    String FX_reader_serial = "";
    public Antennas antennas;

    ConcurrentLinkedQueue<String> hs = new ConcurrentLinkedQueue<>();

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
        System.out.println("FirmwareVersion="+myReader.ReaderCapabilities.getFirwareVersion());
        System.out.println("Printing data in ./rfid_data.txt");

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


    class EventPrinter implements Runnable {

        @Override
        public void run() {
            while(true){
                if(!hs.isEmpty()){
                    String _tbprinted = hs.poll();
                    pw.println(_tbprinted);
                    //pw.println(hs.size());
                    pw.flush();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
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

            myTags = myReader.Actions.getReadTags(1);

            java.util.Date date=new java.util.Date(System.currentTimeMillis());
            for (int index = 0; index < myTags.length; index++)
            {
                TagData tag = myTags[index];
                String key = tag.getTagID();
                String tag_antenna = ""+tag.getAntennaID();
//                String tag_PC = ""+tag.getPC();
                String tag_RSSI = ""+tag.getPeakRSSI();
//                String tag_membankdata = tag.getMemoryBankData();

                String tagdata = FX_reader_serial+"-"+/*tag_time.ConvertTimetoString()+*/",tid:"+key+",ant:"+tag_antenna+",rssi:"+tag_RSSI;
                //String tagdata = FX_reader_serial+"-epc:"+key;

                hs.add(date+"-"+tagdata);
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


        try {
            Antennas.Config antennaConfig = myReader.Config.Antennas.getAntennaConfig(2);
            antennaConfig.setTransmitPowerIndex((short)290);

            Antennas.SingulationControl singulationControl = myReader.Config.Antennas.getSingulationControl(2);
            singulationControl.setSession(SESSION.SESSION_S0);

            myReader.Config.Antennas.setSingulationControl(2, singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        EventPrinter ep = new EventPrinter();
        new Thread(ep).start();
    }

    void InventoryRun()
    {
        //file:///C:/Program%20Files/Zebra%20FXSeries%20Host%20Java%20SDK/RFID/doc/ProgrammersGuide/Generic%20Reader%20Interface/Basic%20Operations.htm
        System.out.println("A few seconds inventory");
        try {
            myReader.Actions.Inventory.perform();


        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        try
        {

            Thread.sleep(6000);
        }
        catch(Exception /*IOException*/ioex)
        {            System.out.println("IO Exception.Stopping inventory");       }
        finally
        {

            try {
                myReader.Actions.Inventory.stop();
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException ee) {
                ee.printStackTrace();
            }
            System.out.println("Inventory stopped");
            exit();

        }

    }
}


