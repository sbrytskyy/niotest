package com.m32s.java.test;

import ie.omk.smpp.BadCommandIDException;
import ie.omk.smpp.message.BindTransmitter;
import ie.omk.smpp.message.BindTransmitterResp;
import ie.omk.smpp.message.GenericNack;
import ie.omk.smpp.message.SMPPPacket;
import ie.omk.smpp.message.SubmitSM;
import ie.omk.smpp.message.SubmitSMResp;
import ie.omk.smpp.message.Unbind;
import ie.omk.smpp.message.UnbindResp;
import ie.omk.smpp.message.tlv.Tag;
import ie.omk.smpp.util.PacketFactory;
import ie.omk.smpp.util.PacketStatus;
import ie.omk.smpp.util.SMPPIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

public class SmppProcessor implements Runnable {
	
	private static Logger logger = Logger.getLogger(SmppProcessor.class);

	private SocketChannel socket;
	private IServer server;

	private static ConcurrentHashMap<SocketChannel, Boolean> socketState = new ConcurrentHashMap<SocketChannel, Boolean>();
	private static SequentialMessageIDGenerator idGenerator = new SequentialMessageIDGenerator();

	private static AtomicLong counterIn = new AtomicLong();
	
	private byte[] data;

	public SmppProcessor(SocketChannel socket, IServer server, byte[] data) {
		
		super();
		this.socket = socket;
		this.server = server;
		this.data = data;
	}

	public void run() {
		logger.debug("Incoming data length: " + data.length);
		
		int offset = 0;
		
		while (offset < data.length) {
			int psize = SMPPIO.bytesToInt(data, offset, ISmppConstants.SIZE_OF_INT);
			logger.debug("Packet size: " + psize);
			
			if (psize < ISmppConstants.HEADER_SIZE || psize > ISmppConstants.MAX_PACKET_SIZE) {
				sendPacket(new GenericNack(), true);
				logger.error("Invalid packet size: " + psize);
			} else {
				try {
					int id = SMPPIO.bytesToInt(data, offset + 4, ISmppConstants.SIZE_OF_INT);
	
					SMPPPacket packet = PacketFactory.newInstance(id);
					if (packet != null) {
						packet.readFrom(data, offset);
						offset += packet.getLength();
						logger.debug("Incoming data offset: " + offset);
						
						processInboundPacket(packet);
					}
				} catch (BadCommandIDException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	private void processInboundPacket(SMPPPacket packet) {
		logger.debug("[SMPP packet-in] " + DumpUtils.dumpPacketDetailed(packet));

		Boolean b = socketState.get(socket);
		boolean isBound = (b != null) ? b : false;
		
		switch (packet.getCommandId()) {
		case SMPPPacket.BIND_TRANSMITTER:
			logger.debug("BIND_TRANSMITTER");
			
			BindTransmitterResp bindTrResp = new BindTransmitterResp(
					(BindTransmitter) packet);
			if (isBound) {
				bindTrResp.setCommandStatus(PacketStatus.ALREADY_BOUND);
			} else {
				bindTrResp.setCommandStatus(PacketStatus.OK);
				isBound = (bindTrResp.getCommandStatus() == PacketStatus.OK);
				if (isBound) {
					bindTrResp.setOptionalParameter(Tag.SC_INTERFACE_VERSION,
							packet.getVersion().getVersionID());
				}
				socketState.put(socket, isBound);
			}

			sendPacket(bindTrResp, true);
			break;
		case SMPPPacket.UNBIND:
			logger.debug("UNBIND");
			
			UnbindResp unbindResp = new UnbindResp((Unbind) packet);
			if (!isBound) {
				unbindResp.setCommandStatus(PacketStatus.INVALID_BIND_STATUS);
			} else {
				isBound = false;
			}
			sendPacket(unbindResp, false);
			break;
		case SMPPPacket.SUBMIT_SM:
			logger.debug("SUBMIT_SM");
			
			final SubmitSM submitSM = (SubmitSM) packet;
            SubmitSMResp submitSMResp = new SubmitSMResp(submitSM);
            String id = idGenerator.newMessageId();
			submitSMResp.setMessageId(id);
			if (isBound) {
                submitSMResp.setCommandStatus(PacketStatus.OK);
			} else {
				// connection not bound, send error 
				submitSMResp.setCommandStatus(PacketStatus.INVALID_BIND_STATUS);
			}
            sendPacket(submitSMResp);
            
            long l = counterIn.incrementAndGet();
            if (l % 500 == 0) {
            	logger.info("l=" + l + ". Message processed...");
            }
            
            // TODO emulate processor activity
            //StringEncrypter.test();
			break;
		}
	}

	private void sendPacket(SMPPPacket packet) {
		sendPacket(packet, true);
	}
	
	private void sendPacket(SMPPPacket packet, boolean withOptional) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			packet.writeTo(bout, withOptional);

			server.send(socket, bout.toByteArray());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}


	public static void invalidateSocket(SocketChannel socket) {
		logger.debug("[invalidateSocket] socket: " + socket.socket().toString());
		socketState.remove(socket);
	}
}
