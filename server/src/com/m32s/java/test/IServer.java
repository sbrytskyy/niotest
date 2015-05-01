package com.m32s.java.test;

import java.nio.channels.SocketChannel;

public interface IServer {

	public void send(SocketChannel socket, byte[] dataCopy);

}
