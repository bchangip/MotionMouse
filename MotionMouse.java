/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package motionmouse;

import com.sun.glass.events.KeyEvent;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;

/**
 *
 * @author Bryan Chan
 */
public class MotionMouse {
    
    //Server constants
    private static final int PORT_NUMBER = 44444;
    private static final int MOUSE_COORDINATES_CODE = 0;
    private static final int COMMAND_CODE = 1;
    private static final int BLOCK_SIZE = 70; //Grid where the mouse can move

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws AWTException {
        ServerSocket serverSocket;
        String[] inputData;
        Double micLevel;
        int clickCounter = 0;
        int coordinatesCounter = 0;
        
        //Initializing stabilization vector
        Vector<Double[]> lastCoordinates = new Vector<Double[]>();
        Iterator lastCoordinatesIterator;
        Double[] tempCoordinate = new Double[3];
        tempCoordinate[0] = 0.0;
        tempCoordinate[1] = 0.0;
        tempCoordinate[2] = 0.0;
        for(int i=0; i<30; i++){
            lastCoordinates.add(tempCoordinate);
        }
        
        Double[] stabilizedCoordinates = new Double[3];
        
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        
        try{
            System.out.println("Server starting at port: "+PORT_NUMBER);
            
            serverSocket = new ServerSocket(PORT_NUMBER);
            
            //Client connecting
            System.out.println("Waiting for a client to connect");
            Socket socket = serverSocket.accept();
            System.out.println("Client connected succesfully");
            
//            Sending message to client
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//            bw.write("This is a message sent from the server");
//            bw.newLine();
//            bw.flush();
            
            //Receiving message from client
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            String data;

            //Loop for reading the data in the socket
            while(true){
                data = br.readLine();
                if(data!=null){
                    inputData = data.split(";");
                    
                    //Data in the socket can be a command obtained from speech recognition or mouse coordinates from the sensor used on the device
                    if(Integer.parseInt(inputData[0])==COMMAND_CODE){
                        System.out.println("Got command: "+inputData[1]);
                        switch(inputData[1]){
                            //Macros based on Robot class asigned to a variety of voice commands
                            case "click":
                                System.out.println("Performing click");
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                break;
                            case "clip":
                                System.out.println("Performing click");
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                break;
                            case "presionar":
                                System.out.println("Performing click");
                                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                                break;
                            case "next window":
                                System.out.println("Next window");
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_RIGHT);
                                robot.keyRelease(KeyEvent.VK_RIGHT);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                break;
                            case "pantalla siguiente":
                                System.out.println("Next window");
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_RIGHT);
                                robot.keyRelease(KeyEvent.VK_RIGHT);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                break;
                            case "previous window":
                                System.out.println("Previous window");
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_LEFT);
                                robot.keyRelease(KeyEvent.VK_LEFT);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                break;
                            case "pantalla anterior":
                                System.out.println("Previous window");
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_LEFT);
                                robot.keyRelease(KeyEvent.VK_LEFT);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                break;
                            case "expose":
                                System.out.println("expose");
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_UP);
                                robot.keyRelease(KeyEvent.VK_UP);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                break;
                            case "exponer":
                                System.out.println("expose");
                                robot.keyPress(KeyEvent.VK_CONTROL);
                                robot.keyPress(KeyEvent.VK_UP);
                                robot.keyRelease(KeyEvent.VK_UP);
                                robot.keyRelease(KeyEvent.VK_CONTROL);
                                break;
                            case "close window":
                                System.out.println("close window");
                                robot.keyPress(KeyEvent.MODIFIER_COMMAND);
                                robot.keyPress(KeyEvent.VK_Q);
                                robot.keyRelease(KeyEvent.VK_Q);
                                robot.keyRelease(KeyEvent.MODIFIER_COMMAND);
                                break;
                            case "cerrar ventana":
                                System.out.println("close window");
                                robot.keyPress(KeyEvent.MODIFIER_COMMAND);
                                robot.keyPress(KeyEvent.VK_Q);
                                robot.keyRelease(KeyEvent.VK_Q);
                                robot.keyRelease(KeyEvent.MODIFIER_COMMAND);
                                break;
                        }
                    } else if (Integer.parseInt(inputData[0])==MOUSE_COORDINATES_CODE){
//                        System.out.println("Got mouse coordinates");
//                        System.out.println("Message sent from the client: "+data);
//                        System.out.println("X: "+inputData[1]+" Y: "+inputData[2]+" Z: "+inputData[3]);
                        lastCoordinates.remove(0);
                        tempCoordinate[0] = Double.parseDouble(inputData[1]);
                        tempCoordinate[1] = Double.parseDouble(inputData[2]);
                        tempCoordinate[2] = Double.parseDouble(inputData[3]);
                        lastCoordinates.add(tempCoordinate);

                        lastCoordinatesIterator = lastCoordinates.iterator();

                        stabilizedCoordinates[0] = 0.0;
                        stabilizedCoordinates[1] = 0.0;
                        stabilizedCoordinates[2] = 0.0;
                        coordinatesCounter = 0;

                        //Coordinates are stabilized using a mean of the last 30 received coordinates
                        while(lastCoordinatesIterator.hasNext()){
                            tempCoordinate = (Double[])lastCoordinatesIterator.next();
                            stabilizedCoordinates[0] += tempCoordinate[0];
                            stabilizedCoordinates[1] += tempCoordinate[1];
                            stabilizedCoordinates[2] += tempCoordinate[2];
                            coordinatesCounter++;
                        }

                        stabilizedCoordinates[0] = stabilizedCoordinates[0]/coordinatesCounter;
                        stabilizedCoordinates[1] = stabilizedCoordinates[1]/coordinatesCounter;
                        stabilizedCoordinates[2] = stabilizedCoordinates[2]/coordinatesCounter;
                        
                        //Accelerometer version
                        robot.mouseMove((int)((-stabilizedCoordinates[0]*100+(1440/2))/BLOCK_SIZE)*BLOCK_SIZE, (int)((stabilizedCoordinates[2]*100+(900/2))/BLOCK_SIZE)*BLOCK_SIZE);
                    
                    
                        //Gyroscope version
                        //robot.mouseMove((int)((-stabilizedCoordinates[0]*300+(1440/2))/BLOCK_SIZE)*BLOCK_SIZE, (int)((stabilizedCoordinates[2]*150+(900/2))/BLOCK_SIZE)*BLOCK_SIZE);
                    }
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}