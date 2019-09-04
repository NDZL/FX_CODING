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
        //InventoryRun();
        //InventoryOneTag();

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

        System.out.println("Transmit power index table\n"+getTransmitPowerIndexTable());
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

        setAntenna2_minPower();

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

    void setAntenna2_minPower(){
        try {
            Antennas.Config antennaConfig = myReader.Config.Antennas.getAntennaConfig(2);
            antennaConfig.setTransmitPowerIndex((short)2);
            System.out.println("CURRENT ANTENNA 2 POWER: "+ antennaConfig.getTransmitPowerIndex());

        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    void WriteECPplusplus(String currentEPC)
    {

        setAntenna2_minPower();

        TagAccess tagAccess = new TagAccess();
        TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();

        MEMORY_BANK memBank = MEMORY_BANK.MEMORY_BANK_EPC;
        writeAccessParams.setMemoryBank(memBank);

        String last4char = currentEPC.substring(currentEPC.length()-4);
        System.out.println("found EPC ending <..."+last4char+">");
        int incremented = 1+ Integer.parseInt(last4char, 16);

        String last4charIncremented = String.format("%04X", incremented);

        try {
            byte[] writeData = hexStringToByteArray(last4charIncremented);
            writeAccessParams.setWriteData(writeData);
            writeAccessParams.setWriteDataLength(2);
            writeAccessParams.setByteOffset(14);
            writeAccessParams.setAccessPassword(0);

            myReader.Actions.TagAccess.writeWait(currentEPC, writeAccessParams,  null);

        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        System.out.println("you should now have EPC <..."+last4charIncremented+">");

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
    boolean isReadWriteOperation = false;
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
                String tag_RSSI = ""+tag.getPeakRSSI();
                String tagdata = FX_reader_serial+"-"+/*tag_time.ConvertTimetoString()+*/",tid:"+key+",ant:"+tag_antenna+",rssi:"+tag_RSSI;

                hs.add(date+"-"+tagdata);
                System.out.println(tagdata);

                if(isReadWriteOperation){
                    isReadWriteOperation=false;

                    WriteECPplusplus(key);  //innesca scrittura di EPC++

                }
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

        isReadWriteOperation = false;

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

    void InventoryOneTag()
    {
        System.out.println("Lettura di un unico tag");

        isReadWriteOperation = true;

        try {
            myReader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        try
        {
            Thread.sleep(200);
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
        }
    }

    String getTransmitPowerIndexTable(){
        int [] powerLevels = myReader.ReaderCapabilities.getTransmitPowerLevelValues();
        //return String.valueOf(powerLevels[]);
        StringBuilder _ssbb=new StringBuilder();
        /*
        for(int i=0; i<powerLevels.length; i++){
            _ssbb.append(i+"="+powerLevels[i]+"\n");
        }
        */
        _ssbb.append("min index 0 = "+(1.0d*powerLevels[0])/100.0+"dBm\n");
        _ssbb.append("max index "+(powerLevels.length-1)+  " = "+1.0*powerLevels[powerLevels.length-1]/100.0+"dBm\n");
        return _ssbb.toString();
    }

}


