package com.ndzl.fxop;

import com.mot.rfid.api3.*;

public class FXOP_ONE {

    RFIDReader myReader = null;
    public Antennas antennas;

    public FXOP_ONE(){
        Connect();
        WriteTag();
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
}
