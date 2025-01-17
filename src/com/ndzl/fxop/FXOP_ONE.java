package com.ndzl.fxop;

import com.mot.rfid.api3.*;

import java.io.*;

import java.util.concurrent.ConcurrentLinkedQueue;

//import static javafx.application.Platform.exit;  //for exit()

/*
TO SPEEDUP INTELLIJ BUILD TIME... SWITCH OFF ONEDRIVE :)

REMOTE DEBUG. To be run on fx jvm:
java -Xdebug -Xrunjdwp:transport=dt_socket,address=8998,server=y -Djava.library.path=/platform/lib/  -cp .:/platform/lib/Symbol.RFID.API3.jar com.ndzl.fxop.FXOP_ONE

 * */

public class FXOP_ONE {

    RFIDReader myReader = null;
    String FX_reader_serial = "";
    public Antennas antennas;
    int ANTENNA_INDEX = 2;              //<== IDX OF ONE CONNECTED ANTENNA
    String EPC_TOBE_INCREMENTED="";

    ConcurrentLinkedQueue<String> hs = new ConcurrentLinkedQueue<>();

    public FXOP_ONE(){
        Connect("169.254.10.1");  //for usb-connected host apps
        //Connect("127.0.0.1");  //for embedded apps
        //WriteTag();

        InventorySetup(); //keep this always enabled
        accessReadTID();  //E28068940000400B0C90F50C
        InventoryRun(3000);  //use many tags near the antenna to show a good output!
        //InventoryRunWithPrefilter(3000);  //use many tags near the antenna to show a good output!
        //InventoryOneTag_EPCplusplus( 500 ); //needs InventorySetup(); to be run before; tags around the antenna: the fewer, the better

        Disconnect();
    }



    public static void main(String[] args) {

        new FXOP_ONE();
    }

    void Connect(String address){
        myReader = new RFIDReader();

        myReader.setHostName(address);
        myReader.setPort(5084);

        antennas = myReader.Config.Antennas;

        try {
            myReader.connect();

            System.out.println("Reader connected!");
            System.out.println("ModelName= "+myReader.ReaderCapabilities.getModelName());
            FX_reader_serial = myReader.ReaderCapabilities.ReaderID.getID();
            System.out.println("SerialNo= "+FX_reader_serial);
            System.out.println("FirmwareVersion="+myReader.ReaderCapabilities.getFirwareVersion());
            System.out.println("Printing data to ./rfid_data.txt");

            //TO SHOW CONFIGURATION PERSISTENCE

            //setAntenna2_minPower();
            //myReader.Config.saveLlrpConfig();



            System.out.println("Transmit power index table\n"+getTransmitPowerIndexTable());
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    void Disconnect(){
        try {
            myReader.disconnect();
            System.out.println("Reader disconnected.");
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

        setAntenna2_maxPower();

        TagAccess tagAccess = new TagAccess();
        TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();

        MEMORY_BANK memBank = MEMORY_BANK.MEMORY_BANK_EPC;
        writeAccessParams.setMemoryBank(memBank);

        try {
            byte[] writeData = hexStringToByteArray("5432");
            writeAccessParams.setWriteData(writeData);
            writeAccessParams.setWriteDataLength(2);
            writeAccessParams.setByteOffset(14);
            writeAccessParams.setAccessPassword(0);

            myReader.Actions.TagAccess.writeWait("321833B2DDD9014000000106", writeAccessParams,  null);

        } catch (InvalidUsageException e) {
           e.printStackTrace();
        } catch (OperationFailureException e) {
           e.printStackTrace();
        }

    }

    void setAntenna2_minPower(){
        try {
            Antennas.AntennaRfConfig antennaRfConfig = myReader.Config.Antennas.getAntennaRfConfig(2);
            antennaRfConfig.setTransmitPowerIndex((short)0);
            myReader.Config.Antennas.setAntennaRfConfig(2,antennaRfConfig);
            System.out.println("CURRENT ANTENNA 2 POWER IDX: "+ antennaRfConfig.getTransmitPowerIndex());

        } catch (InvalidUsageException e) {
            //e.printStackTrace();
        } catch (OperationFailureException e) {
            //e.printStackTrace();
        }
    }

    void setAntenna2_averagePower(){
        try {
            Antennas.AntennaRfConfig antennaRfConfig = myReader.Config.Antennas.getAntennaRfConfig(2);
            antennaRfConfig.setTransmitPowerIndex((short)100);
            myReader.Config.Antennas.setAntennaRfConfig(2,antennaRfConfig);
            System.out.println("CURRENT ANTENNA 2 POWER IDX: "+ antennaRfConfig.getTransmitPowerIndex());

        } catch (InvalidUsageException e) {
            //e.printStackTrace();
        } catch (OperationFailureException e) {
            //e.printStackTrace();
        }
    }

    void setAntenna2_maxPower(){
        try {
            int maxidx = myReader.ReaderCapabilities.getTransmitPowerLevelValues().length-1;
            Antennas.AntennaRfConfig antennaRfConfig = myReader.Config.Antennas.getAntennaRfConfig(2);
            antennaRfConfig.setTransmitPowerIndex((short)maxidx);
            myReader.Config.Antennas.setAntennaRfConfig(2,antennaRfConfig);
            System.out.println("CURRENT ANTENNA 2 POWER IDX: "+ antennaRfConfig.getTransmitPowerIndex());

        } catch (InvalidUsageException e) {
            //e.printStackTrace();
        } catch (OperationFailureException e) {
            //e.printStackTrace();
        }
    }

    void WriteECPplusplus(String currentEPC)
    {
        System.out.println("WriteECPplusplus::BEGIN");

        setAntenna2_maxPower();

        TagAccess tagAccess = new TagAccess();
        TagAccess.WriteAccessParams writeAccessParams = tagAccess.new WriteAccessParams();

        String last4char = currentEPC.substring(currentEPC.length()-4);
        System.out.println("WriteECPplusplus::found EPC ending <..."+last4char+">");
        int incremented = 1+ Integer.parseInt(last4char, 16);

        String last4charIncremented = String.format("%04X", incremented);

        try {
            byte[] writeData = hexStringToByteArray(last4charIncremented);
            writeAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
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
        System.out.println("WriteECPplusplus::you should now have EPC <..."+last4charIncremented+">");
        System.out.println("WriteECPplusplus::END");
    }

    void accessReadTID(){
        //TID
        TagAccess tagaccess = new TagAccess();
        MEMORY_BANK memoryBank = MEMORY_BANK.MEMORY_BANK_TID;
        TagAccess.Sequence opSequence = tagaccess.new Sequence(tagaccess);
        TagAccess.Sequence.Operation op1 = opSequence.new Operation();
        op1.setAccessOperationCode(ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ);


        op1.ReadAccessParams.setMemoryBank(memoryBank);
        op1.ReadAccessParams.setByteCount(0);
        op1.ReadAccessParams.setByteOffset(0);
        op1.ReadAccessParams.setAccessPassword(0);
        try {
            myReader.Actions.TagAccess.OperationSequence.deleteAll();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        try {
            myReader.Actions.TagAccess.OperationSequence.add(op1);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        myReader.Actions.purgeTags();
        try {
            myReader.Actions.TagAccess.OperationSequence.performSequence(null, null, null);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    void readTID(String epc){

        //TID ACCESS
        String tagEPC = epc;// "E28068940000400B0C90F50C";
        TagAccess tagAccess = new TagAccess();
        TagAccess.ReadAccessParams readAccessParams = tagAccess.new ReadAccessParams();
        TagData readAccessTag = null;
        //readAccessParams.setAccessPassword(0);
        //readAccessParams.setByteCount(8);
        readAccessParams.setMemoryBank(MEMORY_BANK.MEMORY_BANK_TID);
       // readAccessParams.setByteOffset(0);
        try {
            readAccessTag = myReader.Actions.TagAccess.readWait( tagEPC , readAccessParams, null);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        System.out.println("EPC="+tagEPC+"\t\t"+readAccessTag.getMemoryBank().toString() + "=" + readAccessTag.getMemoryBankData());
        //+" TID="+readAccessTag.getMemoryBankData()

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

                EPC_TOBE_INCREMENTED = key;  //randomly chosen tag for increment demo

                String tag_antenna = ""+tag.getAntennaID();
                String tag_RSSI = ""+tag.getPeakRSSI();


                String accessResult="N/A";
                if (tag.getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ)
                    accessResult = tag.getMemoryBankData();



                String tagdata = FX_reader_serial+"-"+/*tag_time.ConvertTimetoString()+*/",EPC:"+key+",ant:"+tag_antenna+",rssi:"+tag_RSSI+" TID="+accessResult;

                hs.add(date+"-"+tagdata);
                System.out.println(tagdata);

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

            Antennas.SingulationControl singulationControl = myReader.Config.Antennas.getSingulationControl(ANTENNA_INDEX);
            /*
            if(isReadWriteOperation)
                singulationControl.setSession(SESSION.SESSION_S1);
            else
             */
                singulationControl.setSession(SESSION.SESSION_S0);

            myReader.Config.Antennas.setSingulationControl(ANTENNA_INDEX, singulationControl);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        EventPrinter ep = new EventPrinter();
        new Thread(ep).start();
    }

    void InventoryRun(long millisDuration)
    {
        //file:///C:/Program%20Files/Zebra%20FXSeries%20Host%20Java%20SDK/RFID/doc/ProgrammersGuide/Generic%20Reader%20Interface/Basic%20Operations.htm
        System.out.println("A few seconds inventory");

        isReadWriteOperation = false;

        setAntenna2_minPower();

        try {
            myReader.Actions.Inventory.perform();


        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        try
        {

            Thread.sleep(millisDuration);
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
            //exit();

        }

    }


    void InventoryRunWithPrefilter(long millisDuration)
    {
        System.out.println("InventoryRunWithPrefilter::BEGIN filter on EPC=3218..");

        isReadWriteOperation = false;

        setAntenna2_minPower();

        try{
            PreFilters filters = new PreFilters();
            PreFilters.PreFilter filter = filters.new PreFilter();
            byte[] tagMask = new byte[] { 0x00, 0x00 }; // ATTENTION: "3218".getBytes();  would be wrong!
            filter.setAntennaID((short) 2 );// Set this filter for Antenna ID 2
            filter.setTagPattern(tagMask);// Tags which starts with 0x...
            filter.setTagPatternBitCount(tagMask.length * 8);
            filter.setBitOffset(32); // skip PC bits (always it should be in bit length)
            filter.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
            filter.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_UNAWARE); // use state unaware singulation
            filter.StateUnawareAction.setStateUnawareAction(STATE_UNAWARE_ACTION.STATE_UNAWARE_ACTION_SELECT);
            myReader.Actions.PreFilters.add(filter);
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }


        try {
            myReader.Actions.Inventory.perform();


        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }

        try
        {

            Thread.sleep(millisDuration);
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
            //exit();

        }

    }

    void InventoryOneTag_EPCplusplus(int millisWait)
    {
        System.out.println("InventoryOneTag_EPCplusplus::BEGIN");

        isReadWriteOperation = true;

        setAntenna2_averagePower(); //for inventory

        try {
            myReader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
        try
        {
            Thread.sleep(millisWait);
        }
        catch(Exception /*IOException*/ioex)
        {            System.out.println("InventoryOneTag_EPCplusplus::IO Exception.Stopping inventory");       }
        finally
        {
            try {
                myReader.Actions.Inventory.stop();
            } catch (InvalidUsageException e) {
                e.printStackTrace();
            } catch (OperationFailureException ee) {
                ee.printStackTrace();
            }
            System.out.println("InventoryOneTag_EPCplusplus::END-Inventory stopped");
        }


        //inventory just stopped has selected 1 tag that will be used here:

        WriteECPplusplus(EPC_TOBE_INCREMENTED);  // EPC++


    }

    String getTransmitPowerIndexTable() throws InvalidUsageException, OperationFailureException {
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

        _ssbb.append("current power idx for Antenna2 = "+myReader.Config.Antennas.getAntennaConfig(2).getTransmitPowerIndex()+"\n");

        return _ssbb.toString();
    }

}


