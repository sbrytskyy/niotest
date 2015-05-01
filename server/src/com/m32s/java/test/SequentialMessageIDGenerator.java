package com.m32s.java.test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generate sequential numeric
 * 
 * @author serhiy.brytskyy
 * @version 1.0
 * @since 1.0
 * 
 */
public class SequentialMessageIDGenerator {
	private AtomicInteger seq = new AtomicInteger();
	
    public String newMessageId() {
        /*
         * use database sequence convert into hex representation or if not using
         * database using random
         */
        synchronized (seq) {
        	int nextVal = seq.incrementAndGet();

            return Integer.toString(nextVal, 16);
        }
    }
}