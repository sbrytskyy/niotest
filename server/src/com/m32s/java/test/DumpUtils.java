package com.m32s.java.test;

import java.util.Iterator;

import ie.omk.smpp.Address;
import ie.omk.smpp.message.SMPPPacket;
import ie.omk.smpp.message.tlv.TLVTable;
import ie.omk.smpp.message.tlv.Tag;

public class DumpUtils {

    public static String dumpPacket(SMPPPacket packet) {
        StringBuffer ds = new StringBuffer(80);
        ds.append("SMPP PDU (len: ");
        ds.append(Integer.toString(packet.getLength()));
        ds.append(", cmd: 0x");
        ds.append(Integer.toHexString(packet.getCommandId()));
        ds.append(", status: ");
        ds.append(Integer.toString(packet.getCommandStatus()));
        ds.append(", seq: ");
        ds.append(Integer.toString(packet.getSequenceNum()));
        ds.append(", esm_class: 0x");
        ds.append(Integer.toHexString(packet.getEsmClass()));
        ds.append(")");
        return ds.toString();
    }

    public static String dumpPacketDetailed(SMPPPacket packet) {
        StringBuffer ds = new StringBuffer(160);
        ds.append("SMPP PDU (len: ");
        ds.append(Integer.toString(packet.getLength()));
        ds.append(", cmd: 0x");
        ds.append(Integer.toHexString(packet.getCommandId()));
        ds.append(", status: ");
        ds.append(Integer.toString(packet.getCommandStatus()));
        ds.append(", seq: ");
        ds.append(Integer.toString(packet.getSequenceNum()));
        
        ds.append(", messageId: ");
        ds.append(packet.getMessageId());
        
        ds.append(", service_type: ");
        ds.append(packet.getServiceType());
        Address source = packet.getSource();
        if(source != null)
        {
            ds.append(", source_addr: ");
            ds.append(packet.getSource().toString());
        }
        Address destination = packet.getDestination();
        if(destination != null)
        {
            ds.append(", dest_addr: ");
            ds.append(packet.getDestination().toString());
        }
        ds.append(", esm_class: 0x");
        ds.append(Integer.toHexString(packet.getEsmClass()));
        ds.append(", data_coding: ");
        ds.append(packet.getDataCoding());
        ds.append(", sm_length: ");
        ds.append(packet.getMessageLen());
        ds.append(")");
        ds.append(" OptionalTags: ");
        
        TLVTable tlvTable = packet.getTLVTable();
        ds.append(tlvTable.tagSet().size() + "; ");
        
        if (tlvTable.tagSet().size() > 0) {
            Iterator<Tag> iterator = tlvTable.tagSet().iterator();
            
            while (iterator.hasNext()) {
                Tag tag = iterator.next();
//                ds.append("tag intValue: " + tag.intValue());
                ds.append("tag: 0x" + tag.toHexString());
                
                Object o = packet.getOptionalParameter(tag);
                if (o instanceof Integer) {
                    Integer i = (Integer) o;
                    ds.append(", int value: " + i);
                } else if (o instanceof String) {
                    ds.append(", str value: " + o);
                } else if (o instanceof byte[]) {
                    byte[] a = (byte[]) o;
                    String s = new String(a);
                    
                    ds.append(", byte[] value as str: " + s);
                } else {
                    ds.append(", value: " + o);
                }

                ds.append("; ");
            }
        }
        
        return ds.toString();
    }

    public static String dumpPacketMessage(SMPPPacket packet) {
        int bytesPerLine = 16;
        byte data[] = packet.getMessage();
        if(packet.getMessageLen() == 0)
        {
            data = (byte[])packet.getOptionalParameter(Tag.MESSAGE_PAYLOAD);
            if(data == null)
                data = new byte[0];
        }
        int rows = data.length / bytesPerLine;
        int rest = data.length % bytesPerLine;
        StringBuffer ds = new StringBuffer(data.length * 2 + (rows + 1) * bytesPerLine);
        for (int i = 0; i < rows; i++)
        {
            int off = i * bytesPerLine;
            for (int j = off; j < off + bytesPerLine; j++)
            {
                ds.append(HexadecimalBinaryEncoding.byteToHex(data[j]));
                ds.append(' ');
            }
            ds.append('\n');
        }
        if(rest > 0)
        {
            for (int j = rows * bytesPerLine; j < data.length; j++)
            {
                ds.append(HexadecimalBinaryEncoding.byteToHex(data[j]));
                ds.append(' ');
            }
        }
        return ds.toString();
    }

}
