
import com.wefeel.LT.Decoder;
import com.wefeel.LT.EncodedFrame;
import com.wefeel.LT.Encoder;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author R Wang
 */
public class LT {
    
    static String hash(byte[] data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return "";
    }
    
    public static void main(String[] args) {
        int frameSize = 312;
        long nonce = 0;
        String filePath = "C:\\Path\\To\\Your\\File.gif";
        try {
            System.out.println("Original data is ");
            System.out.println(hash((new FileInputStream(new File(filePath))).readAllBytes()));
            System.out.println();
            InputStream is = new FileInputStream(new File(filePath));
            sequentialLossyTest(is, frameSize, nonce);
            is.close();
            is = new FileInputStream(new File(filePath));
            outOfOrderArrivalTest(is, frameSize, nonce);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sequentialLossyTest(InputStream is, int frameSize, long nonce) throws IOException {
        System.out.println();
        System.out.println("Sequential test with data loss");
        Encoder e = Encoder.get(is, frameSize, nonce);
        System.out.println("Data converted to " + e.getNPackets() + " frames of " + e.getFrameSize() + " bytes each.");
        
        //byte[][] data = e.getData(); for (byte[] d : data) System.out.println(bytesToHex(d));
        System.out.println("--------------------");
        System.out.println("Data into unlimited number of frames");
        
        double lossRate = 0.7;
        // Assuming 70% data is lost during transmission
        Decoder d = new Decoder();
        for (int i = 0; i < 99999; i++) {
            EncodedFrame next = e.next();
            if (Math.random() < lossRate) {
                d.frameReceived(next.toByteArr());
                System.out.print("O");
            } else {
                System.out.print("X");
            }
            if (i % 10 == 9) {
                System.out.println("   " + d.numFrameDecoded() + " frames decoded");
            }
            if (d.finished()) {
                for (; i % 10 != 9; i++) System.out.print("-");
                System.out.println("   Data transmission completed at " + i + " frames generated");
                // for (byte[] f : d.getDecoded()) System.out.println(bytesToHex(f));
                byte[] originalData = d.getDecoded();
                System.out.println("Hash: " + hash(originalData));
                break;
            }
        }
    }

    private static void outOfOrderArrivalTest(InputStream is, int frameSize, long nonce) throws IOException {
        System.out.println();
        System.out.println("Out of order test with data loss");
        
        Encoder e = Encoder.get(is, frameSize, nonce);
        System.out.println("Data converted to " + e.getNPackets() + " frames of " + e.getFrameSize() + " bytes each.");
        //byte[][] data = e.getData(); for (byte[] d : data) System.out.println(bytesToHex(d));
        System.out.println("--------------------");
        System.out.println("Data into unlimited number of frames");
        
        double lossRate = 0.7;
        // Assuming 70% data is lost during transmission
        Decoder d = new Decoder();
        EncodedFrame[] buf = new EncodedFrame[50];
        Random r = new Random();
        for (int i = 0; i < 50; i++) {
            buf[i] = e.next();
        }
        for (int i = 0; i < 99999; i++) {
            EncodedFrame next = e.next();
            if (Math.random() < lossRate) {
                int j = r.nextInt(50);
                d.frameReceived(buf[j].toByteArr());
                buf[j] = next;
                System.out.print("O");
            } else {
                System.out.print("X");
            }
            if (i % 10 == 9) {
                System.out.println("   " + d.numFrameDecoded() + " frames decoded");
            }
            if (d.finished()) {
                for (; i % 10 != 9; i++) System.out.print("-");
                System.out.println("   Data transmission completed at " + i + " frames generated");
                // for (byte[] f : d.getDecoded()) System.out.println(bytesToHex(f));
                byte[] originalData = d.getDecoded();
                System.out.println("Hash: " + hash(originalData));
                break;
            }
        }
        
    }
}
