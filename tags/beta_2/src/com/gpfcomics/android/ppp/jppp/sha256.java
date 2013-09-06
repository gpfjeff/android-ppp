/**
 * @(#)SHA256.java
 *
 * Slight modifications by Jeffrey T. Darlington in support of PPP for Android to
 * comment out a number of methods and variables that are never used, thus taking up
 * wasted memory and causing warning messages to pop up in Eclipse.
 *
 * @author 
 * @version 1.00 2008/2/11
 */
package com.gpfcomics.android.ppp.jppp;
import java.math.BigInteger;
public class sha256{

   static long[] K = {	0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L,
							0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L,
							0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L,
							0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L,
							0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL,
							0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL,
							0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L,
							0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L,
							0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L,
							0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L,
							0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L,
							0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L,
							0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L,
							0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L,
							0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L,
							0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L };
						
   long[] H = { 0x6a09e667L, 0xbb67ae85L, 0x3c6ef372L, 0xa54ff53aL,
   				0x510e527fL, 0x9b05688cL, 0x1f83d9abL, 0x5be0cd19L };
   				
   static long mask = 0xffffffffL;

   private boolean flag = false; 
   private int lastBlock=0;
	 
	public sha256(){
	}
	
	/*
	private long min(long x, long y){
		return (x<y) ? x : y;
	}
	*/
	
	private long ROTRN(long x, long n){
		return ((x>>n)|(x<<(32-n)));
	}
	
	/*
	private long ROTLN(long x, long n){
		return ((x<<n)|(x>>(32-n)));
	}
	*/
	
	private long SHRN(long x, long n){
		return (x>>n);
	}
	
	private long Ch(long x, long y, long z){
		return ((x & y)^(~x & z));
	}
	
	private long Maj(long x, long y, long z){
		return  ((x & y)^(x & z)^(y & z));
	}
	
	private long BigSigma0(long x){
		return ROTRN(x, 2)^ROTRN(x, 13)^ROTRN(x, 22);
	}
	
	private long BigSigma1(long x){
		return ROTRN(x, 6)^ROTRN(x, 11)^ROTRN(x, 25);
	}
	
	private long SmallSigma0(long x){
		return ROTRN(x, 7)^ROTRN(x, 18)^SHRN(x, 3);
	}
	
	private long SmallSigma1(long x){
		return ROTRN(x, 17)^ROTRN(x, 19)^SHRN(x, 10);
	}
	
	private int blockNumber(byte[] message){
		lastBlock = message.length%64;
		
		if(lastBlock>=56){
			flag = true;
			return message.length/64 + 2;
		}
		else 
			return message.length/64 + 1;
	}
	
	 void compress(byte[] buf){
		long[] A = new long[8];
		long[] W = new long[80];
		long modulo =(long) Math.pow(2,32);
		//BigInteger bi = new BigInteger(buf);
		byte[] temp = new byte[4];
		
		for (int i = 0; i < 16; i++){
		   W[i] = 0;
		   
		   for (int j = 0; j < 4; j++){
		   	temp[j] = buf[i*4 + j];
		   }
		   
		   BigInteger tempB = new BigInteger(temp);
		   W[i] = tempB.longValue();

		}
	   
		for (int t = 16; t < 80; t++){
			W[t] = SmallSigma1(W[t-2]) + W[t-7] + SmallSigma0(W[t-15]) + W[t-16];
			W[t] = W[t] % modulo;
		}
		
		for (int i = 0; i < 8; i++) 
			A[i] = H[i];
		
		for (int i = 0; i < 64; i++){ 
				   
		   long T1 = A[7] + BigSigma1(A[4]) + Ch(A[4],A[5],A[6]) + K[i] + W[i];
		   T1 = T1 % modulo;
		  
		   long T2 = BigSigma0(A[0]) + Maj(A[0],A[1],A[2]);
		   T2 = T2 % modulo;
		  
		   A[7] = A[6];
		   A[6] = A[5];
		   A[5] = A[4];
		   A[4] = (A[3] + T1)%modulo;
		   A[3] = A[2];
		   A[2] = A[1];
		   A[1] = A[0];
		   A[0] = (T1 + T2)%modulo;

		 }  
		   
		for (int i = 0; i < 8; i++){
		   H[i] += A[i];
		   H[i] = H[i] % modulo;

		}
   }
	
	public String hash(String s){
		byte[] message = s.getBytes ();
		long count = 0;
	  	byte[] buffer = new byte[64];
	  	//int blocklen = 0;
	  	String str="", hashVal="";
	 	try{
	  		int noOfBlocks = blockNumber(message);
			for(int counter=0; counter<noOfBlocks; counter++){
				if(counter<noOfBlocks-2)
					for (int temp=0; temp<64; temp++){
						buffer[temp] = message[counter*64 + temp];
						count++;
					}
				else
					if((flag==false)||((flag==true)&&(counter==noOfBlocks-2)))
		 			for (int temp=0; temp<lastBlock; temp++){
		    				buffer[temp] = message[counter*64 + temp];
							count++;
						}
					
				
		 	//BigInteger bi = new BigInteger(buffer);
			
				if(((flag==false)&&(counter == noOfBlocks-1))||((flag==true)&&(counter==noOfBlocks-2))){
		   			buffer[lastBlock] = (byte)0x80; 
		   			if(flag==true){
		   				for (int i = lastBlock+1; i < 64; i++) {
		   					buffer[i] = 0;
		   					count++;
		   					compress(buffer);
		   					counter++;
		   				}
		   				
		   				for (int i = 0; i < 64; i++) {
		   					buffer[i] = 0;
		   					count++;
		   				}
		   			}
		   			if((flag==false)&&(counter==noOfBlocks-1)){
		 			count *=8;
		    			for (int i = 63; i >= 56; i--){
							buffer[i] = (byte)(count & 0xff);
			   	 			count >>= 8;
			   			}
			   		}
			   		else if((flag==true)&&(counter==noOfBlocks-1)){
			   			for (int i = 0; i < 64; i++) {
		   					buffer[i] = 0;
		   					count++;
		   				}
		   				count *=8;
		    			for (int i = 63; i >= 56; i--){
							buffer[i] = (byte)(count & 0xff);
			   	 			count >>= 8;
			   			}
			   		}
				}
				//BigInteger kk = new BigInteger(buffer);
			    compress(buffer);
	   		}
	 	}
	 	catch(Exception e){ 
	 		System.err.println(e); 
	 		System.exit(1); 
	 	} 
	 	   
	 	for (int i = 0; i < 8; i++){
	 		str = Long.toHexString(H[i]);
			int strlen = str.length();
			for (int j = strlen; j < 8; j++) 
				str = "0" + str;
			hashVal += str;
	 	}
	 	return hashVal;
   }
	
	public void clearVals(){
		flag = false; 
   		lastBlock=0;
   		H[0] = 0x6a09e667L;
   		H[1] = 0xbb67ae85L;
   		H[2] = 0x3c6ef372L;
   		H[3] = 0xa54ff53aL;
   		H[4] = 0x510e527fL;
   		H[5] = 0x9b05688cL;
   		H[6] = 0x1f83d9abL;
   		H[7] = 0x5be0cd19L;
	}

}