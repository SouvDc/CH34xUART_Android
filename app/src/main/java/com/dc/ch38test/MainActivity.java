package com.dc.ch38test;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cnbot.ch34x.CH34xUARTDriver;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;

import static com.dc.ch38test.ByteUtils.toHexString;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private EditText readText,writeText;
    private TextView txtContent;
    private Spinner baudSpinner,stopSpinner,dataSpinner,paritySpinner,flowSpinner;
    private Button writeButton, configButton, openButton, clearButton, btnShow;

    private boolean isOpen;
    public int baudRate;
    public byte baudRate_byte;
    public byte stopBit;
    public byte dataBit;
    public byte parity;
    public byte flowControl;
    public int totalrecv;
    public byte[] writeBuffer;
    public byte[] readBuffer;
    private int retval;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            readText.append((String) msg.obj);        }
    };

    private CH34xUARTDriver ch34xUARTDriver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        initDevice();
        initCH34();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(ch34xUARTDriver != null){
            ch34xUARTDriver.CloseDevice();
        }
        unregisterReceiver(mUsbDeviceReceiver);
    }

    private void initData() {
        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        isOpen = false;
        configButton.setEnabled(false);
        writeButton.setEnabled(false);
    }

    private void initCH34() {
        if (!ch34xUARTDriver.UsbFeatureSupported())
            // 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("提示")
                    .setMessage("您的手机不支持USB HOST，请更换其他手机再试！")
                    .setPositiveButton("确认",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    private void initDevice() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbDeviceReceiver, filter);
        ch34xUARTDriver = new CH34xUARTDriver(
                (UsbManager) getSystemService(Context.USB_SERVICE), this);
    }

    private void initUI() {
        readText = (EditText) findViewById(R.id.ReadValues);
        writeText = (EditText) findViewById(R.id.WriteValues);
        configButton = (Button) findViewById(R.id.configButton);
        writeButton = (Button) findViewById(R.id.WriteButton);
        openButton = (Button) findViewById(R.id.open_device);

        btnShow = (Button) findViewById(R.id.btnShow);
        clearButton = (Button) findViewById(R.id.clearButton);
        txtContent = (TextView) findViewById(R.id.txtContent);
        baudSpinner = (Spinner) findViewById(R.id.baudRateValue);
        ArrayAdapter<CharSequence> baudAdapter = ArrayAdapter
                .createFromResource(this, R.array.baud_rate,
                        R.layout.my_spinner_textview);
        baudAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        baudSpinner.setAdapter(baudAdapter);
        baudSpinner.setGravity(0x10);
        baudSpinner.setSelection(9);
        /* by default it is 9600 */
        baudRate = 115200;

        /* stop bits */
        stopSpinner = (Spinner) findViewById(R.id.stopBitValue);
        ArrayAdapter<CharSequence> stopAdapter = ArrayAdapter
                .createFromResource(this, R.array.stop_bits,
                        R.layout.my_spinner_textview);
        stopAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        stopSpinner.setAdapter(stopAdapter);
        stopSpinner.setGravity(0x01);
        /* default is stop bit 1 */
        stopBit = 1;

        /* data bits */
        dataSpinner = (Spinner) findViewById(R.id.dataBitValue);
        ArrayAdapter<CharSequence> dataAdapter = ArrayAdapter
                .createFromResource(this, R.array.data_bits,
                        R.layout.my_spinner_textview);
        dataAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        dataSpinner.setAdapter(dataAdapter);
        dataSpinner.setGravity(0x11);
        dataSpinner.setSelection(3);
        /* default data bit is 8 bit */
        dataBit = 8;

        /* parity */
        paritySpinner = (Spinner) findViewById(R.id.parityValue);
        ArrayAdapter<CharSequence> parityAdapter = ArrayAdapter
                .createFromResource(this, R.array.parity,
                        R.layout.my_spinner_textview);
        parityAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        paritySpinner.setAdapter(parityAdapter);
        paritySpinner.setGravity(0x11);
        /* default is none */
        parity = 0;

        /* flow control */
        flowSpinner = (Spinner) findViewById(R.id.flowControlValue);
        ArrayAdapter<CharSequence> flowAdapter = ArrayAdapter
                .createFromResource(this, R.array.flow_control,
                        R.layout.my_spinner_textview);
        flowAdapter.setDropDownViewResource(R.layout.my_spinner_textview);
        flowSpinner.setAdapter(flowAdapter);
        flowSpinner.setGravity(0x11);
        /* default flow control is is none */
        flowControl = 0;

        /* set the adapter listeners for baud */
        baudSpinner.setOnItemSelectedListener(new MyOnBaudSelectedListener());
        /* set the adapter listeners for stop bits */
        stopSpinner.setOnItemSelectedListener(new MyOnStopSelectedListener());
        /* set the adapter listeners for data bits */
        dataSpinner.setOnItemSelectedListener(new MyOnDataSelectedListener());
        /* set the adapter listeners for parity */
        paritySpinner
                .setOnItemSelectedListener(new MyOnParitySelectedListener());
        /* set the adapter listeners for flow control */
        flowSpinner.setOnItemSelectedListener(new MyOnFlowSelectedListener());


        btnShow.setOnClickListener(this);
        openButton.setOnClickListener(this);
        writeButton.setOnClickListener(this);
        configButton.setOnClickListener(this);
        clearButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.open_device:
                openDevice();
                break;
            case R.id.configButton:
                configDevice();
                break;
            case R.id.clearButton:
                totalrecv = 0;
                readText.setText("");
                break;
            case R.id.WriteButton:
                writeData();
                break;
            case R.id.btnShow:
                txtContent.setText(readText.getText().toString());
                break;
            default:break;
        }
    }

    /**
     * 写入byte[]数据
     */
    private void writeData() {
        byte[] to_send = ByteUtils.toByteArray(writeText.getText().toString());
        byte[] to_send2 = ByteUtils.toByteArray2(writeText.getText().toString());
        txtContent.setText(new String(to_send)+"---"+new String(to_send2));
        //写数据，第一个参数为需要发送的字节数组，第二个参数为需要发送的字节长度，返回实际发送的字节长度
        int retval = 0;
        try {
            retval = ch34xUARTDriver.WriteData(to_send2, to_send2.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (retval < 0) {
            Toast.makeText(MainActivity.this, "写失败!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 配置设备参数
     */
    private void configDevice() {
        //配置串口波特率，函数说明可参照编程手册
        if (ch34xUARTDriver.SetConfig(baudRate, dataBit, stopBit, parity,
                flowControl)) {
            Toast.makeText(MainActivity.this, "串口设置成功!",
                    Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(MainActivity.this, "串口设置失败!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开设备
     */
    private void openDevice() {
        if (!isOpen) {
            // EnumerateDevice方法用于枚举CH34X设备以及打开相关设备
            UsbDevice usbDevice = ch34xUARTDriver.EnumerateDevice();
            if (usbDevice == null) {
                Toast.makeText(MainActivity.this, "打开设备失败!",
                        Toast.LENGTH_SHORT).show();
                ch34xUARTDriver.CloseDevice();
                return;
            }
            // 打开并连接USB
            ch34xUARTDriver.OpenDevice(usbDevice);
            if (ch34xUARTDriver.isConnected()) {
                if (!ch34xUARTDriver.UartInit()) {
                    //对串口设备进行初始化操作
                    Toast.makeText(MainActivity.this, "设备初始化失败!",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(MainActivity.this, "打开设备成功!",
                        Toast.LENGTH_SHORT).show();
                isOpen = true;
                openButton.setText("Close");
                configButton.setEnabled(true);
                writeButton.setEnabled(true);

                new readThread().start();//开启读线程读取串口接收的数据

            }
        } else {
            openButton.setText("Open");
            configButton.setEnabled(false);
            writeButton.setEnabled(false);
            isOpen = false;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            ch34xUARTDriver.CloseDevice();
            totalrecv = 0;
        }
    }

    public class MyOnBaudSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            baudRate = Integer.parseInt(parent.getItemAtPosition(position)
                    .toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    public class MyOnStopSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            stopBit = (byte) Integer.parseInt(parent
                    .getItemAtPosition(position).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    }

    public class MyOnDataSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            dataBit = (byte) Integer.parseInt(parent
                    .getItemAtPosition(position).toString());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    }

    public class MyOnParitySelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            String parityString = new String(parent.getItemAtPosition(position)
                    .toString());
            if (parityString.compareTo("None") == 0) {
                parity = 0;
            }

            if (parityString.compareTo("Odd") == 0) {
                parity = 1;
            }

            if (parityString.compareTo("Even") == 0) {
                parity = 2;
            }

            if (parityString.compareTo("Mark") == 0) {
                parity = 3;
            }

            if (parityString.compareTo("Space") == 0) {
                parity = 4;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    }

    public class MyOnFlowSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            String flowString = new String(parent.getItemAtPosition(position)
                    .toString());
            if (flowString.compareTo("None") == 0) {
                flowControl = 0;
            }

            if (flowString.compareTo("CTS/RTS") == 0) {
                flowControl = 1;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }

    }


    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Toast.makeText(MainActivity.this, action, Toast.LENGTH_LONG).show();
            Log.e(TAG, "action:" + action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice deviceFound = (UsbDevice) intent
                        .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Toast.makeText(
                        MainActivity.this,
                        "设备加入: \n"
                                + deviceFound.toString(), Toast.LENGTH_LONG)
                        .show();
                openDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent
                        .getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this,
                        "设备断开: \n" + device.toString(),
                        Toast.LENGTH_LONG).show();
                isOpen = false;
                ch34xUARTDriver.CloseDevice();
            }
        }

    };

    private class readThread extends Thread {

        @Override
        public void run() {
            byte[] buffer = new byte[4096];

            while (true) {

                Message msg = Message.obtain();
                if (!isOpen) {
                    break;
                }
                int length = ch34xUARTDriver.ReadData(buffer, 4096);
                if (length > 0) {
                    String recv = toHexString(buffer, length);		//以16进制输出
                    msg.obj = recv;
                    handler.sendMessage(msg);
                }
            }
        }
    }



}
