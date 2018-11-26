package com.example.non_admin.picar;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
//https://github.com/Nilhcem/usbfun-androidthings/blob/master/mobile/src/main/java/com/nilhcem/usbfun/mobile/MainActivity.java

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;

import static android.support.v4.content.ContextCompat.getSystemService;
//import UsbSerial;

/**
 * see the following 3 links:
 *   - https://github.com/Nilhcem/usbfun-androidthings/blob/master/mobile/src/main/java/com/nilhcem/usbfun/mobile/MainActivity.java)
 *   - http://nilhcem.com/android-things/usb-communications
 *   - https://github.com/felHR85/UsbSerial
 * Classes that inherit this can write to and read from the serial port.
 * They MUST override the void receive(String) method that's called when they get input.
 * Devices must be serial devices (not parallel)
 * 
 * Code for the connection and reading and writing over RXTX is taken from the 
 * <a href="http://rxtx.qbang.org/wiki/index.php/Examples">examples on the RXTX wiki</a>.
 * 
 * More useful links:
 * 
 * - <a href="http://users.frii.com/jarvi/rxtx/doc/index.html>gnu.io docs</a>
 * - <a href="https://docs.oracle.com/cd/E17802_01/products/products/javacomm/reference/api/index.html">
 * javax.comm docs</a>
 * - <a href-"http://rxtx.qbang.org/wiki/index.php/Download">
 * download link for the RXTXcomm.jar I'm using</a>
 * - <a href="http://rxtx.qbang.org/wiki/index.php/Using_RXTX_In_Eclipse">
 * Partially complete installation in eclipse instructions</a>
 * 
 * To fully install on eclipse in linux, run `sudo apt-get install librxtx-java`
 * then, make `/usr/share/java:/usr/lib/jni` the native library location of the RXTXcomm.jar
 * when you add it to the build path
 * 
 *
 * @author pi
 *
 */
public class Device{
    private static UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE;
    public static HashSet<Device> devSet;
    public static HashMap<String, Device> devName;

    private static final String TAG = "Device";
	String name;
	private HashMap<String, ArduinoAPI> APIs;
	// make a singleton device manager
	private UsbDevice mDevice;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialDevice;
    private String buffer = "";

    //These store the devices


    private UsbSerialInterface.UsbReadCallback callback;

	/**
	 * constructor
	 * @param mDevice
	 */
	public Device(UsbDevice mDevice){
		// add to devSet and devName in here
		this.mDevice = mDevice;

        callback = new UsbSerialInterface.UsbReadCallback() {
            /**
             * This message is called when a device sends a message to the pi
             *
             * If an API gets a message and can process it, it will return true.
             * @param data - the message received.
             * TODO: get device name and APIs from the device
             */
            @Override
            public void onReceivedData(byte[] data) {
                try {

                    String dataUtf8 = new String(data, "UTF-8");
                    Log.i(TAG, "Data received: " + dataUtf8);
/*
                buffer += dataUtf8;

                int index;
                while ((index = buffer.indexOf('\n')) != -1) {
                    String dataStr = buffer.substring(0, index + 1).trim();
                    buffer = buffer.length() == index ? "" : buffer.substring(index + 1);
                    Log.d(TAG, "data received");
                }
*/
                    //call addAPIs here
                    for(ArduinoAPI api : APIs.values()){
                        if(api.receive(dataUtf8))
                            break;//the callback was for the api
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "Error receiving USB data", e);
                }
            }

        };
		try{
			this.connect();
            this.serialDevice.read(callback);//adding a callback to the connection
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void setUsbManager(UsbManager usb){
        usbManager = usb;
    }

	@Override
	public boolean equals(Object other){
		if(other instanceof Device)
			return this.equals((Device) other);
		else if (other instanceof UsbDevice)
		    return this.equals((UsbDevice) other);
		else
			return super.equals(other);
	}

	public boolean equals(UsbDevice other){
		return this.mDevice.equals(other);
	}

	public boolean equals(Device other){
		return this.mDevice.equals(other.getDevice());
	}


	@Override
	public int hashCode() {
		return this.mDevice.hashCode();
	}

	public UsbDevice getDevice(){
		return this.mDevice;
	}

	
	/**
	 * This adds an API the connected arduino can use. For example the motor API can control motors
	 * @param name A unique identifier for the API name.
	 * @param api The actual api. Note,  You COULD have API objects of the same type. For example if you have two motor controllers connected,
	 * You'd need an API for each because APIs also store state information.
	 *
	 *  TODO: delete this?
	 */
	public void addAPI(String name, ArduinoAPI api){
		APIs.put(name, api);
	}

	
	/**
	 * This sends data to the device
	 * @param message the data to be sent
	 */
	protected void send(String message){
	    this.serialDevice.write(message.getBytes());
	}
	
	/**
	 * gets the name of the device
	 * @return name the name of the port the device is connected to. (ex: /dev/ttyUSB1)
	 */
	public String getName(){
		return this.name;
	}
	
	/**
	 * Sets the name of the device. The name is important for connecting.
	 * @param name the name of the port the device is connected to. (ex: /dev/ttyUSB1)
	 */
	void setName(String name){
		this.name = name;
	}
	
	/**
	 * connects to a physical device and sets up the SerialReader reader and SerialWriter writer 
	 * member variables. These are 
	 *
	 */
	protected void connect (){
	    //connect(57600,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
		this.connect (115200, UsbSerialInterface.DATA_BITS_8,
				UsbSerialInterface.STOP_BITS_1, UsbSerialInterface.PARITY_NONE);
	}
	
	/** see connect(), This does the same thing as it, but it lets you set the serial communication
	 * paramaters yourself instead of assuming defaults.
	 * 
	 * see <a href = "https://stackoverflow.com/questions/391127/meaning-of-serial-port-parameters-in-java#391751">
	 * This stackoverflow post on these parameters </a>
	 * 
	 * @param speed the baud rate
	 * @param bits of data that are transferred at a time. This is typically 8 since most machines have 8-bit bytes these days.
	 * @param stop_bits defines # of trailing bits added to mark the end of the word.
	 * @param parity defines how error checking is done
	 * @throws Exception see connect()
	 * maybe add flow control to this???
	 */
   protected void connect (int speed, int bits, int stop_bits, int parity)
    {
		Log.i(TAG, "Ready to open USB device connection");
		connection = usbManager.openDevice(this.mDevice);
		serialDevice = UsbSerialDevice.createUsbSerialDevice(this.mDevice, connection);
		if (serialDevice != null) {
			if (serialDevice.open()) {
				serialDevice.setBaudRate(speed);
				serialDevice.setDataBits(bits);
				serialDevice.setStopBits(stop_bits);
				serialDevice.setParity(parity);
				serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
				Log.i(TAG, "Serial connection opened");
			} else {
				Log.w(TAG, "Cannot open serial connection");
			}
		} else {
			Log.w(TAG, "Could not create Usb Serial Device");
		}
    }


   public void killConnection(){
	   //this.serialPort.removeEventListener();
	   //this.serialPort.close();
   }
}
