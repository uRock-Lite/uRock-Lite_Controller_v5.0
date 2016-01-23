package com.example.urook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	//UI
	Button btnJmpStage, btnBackIndex, btnBackStage, btnBackEffect, btnView, btnBack;
    ListView lvStage; //Stage listview
    ListView lvEffect; //lvEffect listview
    
    //controller.xml
    TextView tvAttr[], tvName;
    SeekBar sbAttr[];
    LinearLayout llObject;
    
    //data
	String jsonObj; //JSON data    
	String[] StageNumber;
	String[] ListviewName;
	String[][] EffectsValue;

	int Listview_Name_length = 0, 
		Effects_value_length[], 
		Effects_value_length_max = 0, 
		StageNumberLength = 0,	//"Stage Number":2
		btn_count = 0, none = 0, effect_count[][],
		ValueBuf = 0, StageBuf = 0;
	
	OutputStream dos;
	JSONObject object;
	byte[] SendValue8byte = new byte[8];//{0x01,0xff,0xff,0xff,0xff,0xff,0xff,0xff};
	//UI

	//bluetooth
	static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";  
    Button btnSearch, btnDis, btnExit, btnSend, btnStop, btnDown;  
    TextView textview;
    ToggleButton tbtnSwitch;  
    ListView lvBTDevices;  
    ArrayAdapter<String> adtDevices;  
    List<String> lstDevices = new ArrayList<String>();  
    BluetoothAdapter btAdapt;  
    public static BluetoothSocket btSocket;  
    int click = 0, clickOn = 1, start_value = 0;
    public String msg = "";
    SppReceiver sppReceiver;
    boolean RUN_THREAD = true;
	//bluetooth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //user code
        SendByteInit();       
        BTObjectInit();
        UIObjectInit();
        //user code     
    }
    @Override  
    protected void onDestroy() {  
        this.unregisterReceiver(searchDevices);  
        super.onDestroy();  
        android.os.Process.killProcess(android.os.Process.myPid());  
    }  
    
    public void SendByteInit(){
        //hello SendValue8byte[0] = (byte) 0x01;
        //edit SendValue8byte[0] = (byte) 0x02;
        //stage SendValue8byte[1] =(byte) 0x01~0xff;
        SendValue8byte[0] = (byte) 0x01;
        SendValue8byte[1] = (byte) 0xFF;
        SendValue8byte[2] = (byte) 0xFF;
        SendValue8byte[3] = (byte) 0xFF;
        SendValue8byte[4] = (byte) 0xFF;
        SendValue8byte[5] = (byte) 0xFF;
        SendValue8byte[6] = (byte) 0xFF;
        SendValue8byte[7] = (byte) 0xFF;
    }
    public void BTObjectInit(){//bluetooth
        //Button 設置 
        btnSearch = (Button) this.findViewById(R.id.btnSearch);  
        btnSearch.setOnClickListener(new ClickEvent());  
        btnExit = (Button) this.findViewById(R.id.btnExit);  
        btnExit.setOnClickListener(new ClickEvent());  
        btnDis = (Button) this.findViewById(R.id.btnDis);  
        btnDis.setOnClickListener(new ClickEvent());  
        btnSend = (Button) this.findViewById(R.id.btnSend);  
        btnSend.setOnClickListener(new ClickEvent()); 
        btnSend.setEnabled(false);
        
        // ToogleButton設置
        tbtnSwitch = (ToggleButton) this.findViewById(R.id.tbtnSwitch);  
        tbtnSwitch.setOnClickListener(new ClickEvent());  
  
        // ListView設置   
        lvBTDevices = (ListView) this.findViewById(R.id.lvDevices);  
        adtDevices = new ArrayAdapter<String>(this,  
                android.R.layout.simple_list_item_1, lstDevices);  
        lvBTDevices.setBackgroundColor(Color.GRAY);//設定背景為黑色
        lvBTDevices.setVisibility(View.GONE);//隱藏
        lvBTDevices.setAdapter(adtDevices);  
        lvBTDevices.setOnItemClickListener(new ItemClickEvent());  
  
        //TextView 設置
        textview = (TextView)findViewById(R.id.textView1);
        textview.setText("");
        
        //初始化藍芽功能
        btAdapt = BluetoothAdapter.getDefaultAdapter();
         
        if (btAdapt.isEnabled()) {  
            tbtnSwitch.setChecked(false);  
        } else {  
            tbtnSwitch.setChecked(true);  
        }   
       
        //註冊Receiver來獲取藍芽設置相關的結果 
        IntentFilter intent = new IntentFilter();  
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver來取得搜索結果  
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);  
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);  
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);  
        registerReceiver(searchDevices, intent);  
    }
    public void UIObjectInit(){
    	btnView= (Button)findViewById(R.id.btnBT);  
    	btnView.setBackgroundResource(R.drawable.blue3);
    	
        btnJmpStage= (Button)findViewById(R.id.GetValueJmpEffects_btn);  
        btnJmpStage.setOnClickListener(new UIClickEvent());
        btnJmpStage.setEnabled(false);
        btnJmpStage.setBackgroundResource(R.drawable.rook3);
    }
      
    private boolean CreateJSON()
    {
    	Log.i("msg", jsonObj);
        try{
        	object = (JSONObject) new JSONTokener(jsonObj).nextValue();
        	String string = object.getString("Name");
        	Log.i("msg", "Name: " + string);
        	Log.i("msg", "Stage Number: " + object.getString("Stage Number"));  
        	JSONArray Effects = object.getJSONArray("Effects");
        	Log.i("msg", "Effects-length: " + Effects.length());            
        	Log.i("msg", "  Name: " + Effects.getJSONObject(0).getString("Name"));
        	Log.i("msg", "    Attr-Name: " + Effects.getJSONObject(0).getJSONArray("Attr").getJSONArray(0).getString(0));
        	Log.i("msg", "    Attr-Unit: " + Effects.getJSONObject(0).getJSONArray("Attr").getJSONArray(0).getString(1));
        	Log.i("msg", "    Attr-Value: " + Effects.getJSONObject(0).getJSONArray("Attr").getJSONArray(0).getInt(2));
        	Log.i("msg", "    Attr-Min: " + Effects.getJSONObject(0).getJSONArray("Attr").getJSONArray(0).getInt(3));
        	Log.i("msg", "    Attr-Max: " + Effects.getJSONObject(0).getJSONArray("Attr").getJSONArray(0).getInt(4));
            
        	//取得StageNumberLength
        	StageNumberLength = object.getInt("Stage Number");
        	
        	effect_count = new int[StageNumberLength][StageNumberLength];
        	for(int i = 0; i < StageNumberLength; i++){
        		effect_count[i][0] = -1;
        		effect_count[i][1] = -1;
        	}
        	//建立動態Effects陣列大小
        	Listview_Name_length = Effects.length();
        	Effects_value_length = new int[Effects.length()];
        	for(int i = 0; i < Listview_Name_length; i++){
        		Effects_value_length[i] = Effects.getJSONObject(i).getJSONArray("Attr").length();
        		Effects_value_length_max += Effects_value_length[i];
        	}
        		
        	Log.i("msg", "Effects_value_length--"+Effects_value_length_max);
        	ListviewName = new String[Listview_Name_length];
        	EffectsValue = new String[Effects_value_length_max][5];
        	tvAttr = new TextView[Effects_value_length_max];
        	sbAttr = new SeekBar[Effects_value_length_max];  
            Log.i("msg", "Length:"+EffectsValue.length);
            int arraycount = 0;
            for(int i = 0; i < ListviewName.length; i++){
            	ListviewName[i] = Effects.getJSONObject(i).getString("Name");
            	for(int x = 0; x < Effects_value_length[i]; x++){
            		for(int j = 0; j < 5; j++){
            			EffectsValue[arraycount][j] = Effects.getJSONObject(i).getJSONArray("Attr").getJSONArray(x).getString(j);
            			Log.i("msg", i+"-"+x+"-"+j+"-"+EffectsValue[arraycount][j]);
                    }
        			arraycount++;
                }
            }
            return true;
        } catch(JSONException e){
        	Log.i("msg", "Error");
        	e.printStackTrace();
            return false;
        }
    }
   
    public void JmpIndex(){//btnClick
        setContentView(R.layout.activity_main);
        clickOn = 1;
        SendByteInit();
        Disconnect();
        BTObjectInit();  
        UIObjectInit();
    }
    public void JmpStage(){//btnClick
        setContentView(R.layout.stage_listview);
        btnBackIndex= (Button)findViewById(R.id.JmpIndex_btn);
        btnBackIndex.setOnClickListener(new UIClickEvent());
    }
    public void JmpEffect(){//btnClick
        setContentView(R.layout.effects_listview);
        btnBackStage= (Button)findViewById(R.id.JmpStage_btn);
        btnBackStage.setOnClickListener(new UIClickEvent());
    }
    public void JmpController(int position){//btnClick
        setContentView(R.layout.controller);
        btnBackEffect= (Button)findViewById(R.id.previous_btn);
        btnBackEffect.setOnClickListener(new UIClickEvent());
        CreateAttrObject(position); 
    }
    public void BackEffect(){//btnClick
        setContentView(R.layout.effects_listview);
        lvEffectInit();
        btnBackStage= (Button)findViewById(R.id.JmpStage_btn);
        btnBackStage.setOnClickListener(new UIClickEvent());
    }
    class UIClickEvent implements View.OnClickListener {  
        @Override  
        public void onClick(View v) { 
            if(v == btnJmpStage){
            	if(btn_count == 0)
            	{
            		//OutputStream dos;
	            	try {
	            		dos = btSocket.getOutputStream();
	            		dos.write(SendValue8byte);
	            		//dos.write(a.getBytes());
	            		dos.flush();
	            	} catch (Exception e) {
	            	} 
            	}else{
	            	JmpStage();
	            	lvStageInit();
            	}
            }else if(v == btnBackIndex){
            	JmpIndex();
            }else if(v == btnBackStage){
            	JmpStage();
            	lvStageInit();
            }else if(v == btnBackEffect){
            	BackEffect();
                SendValue8byte[3] = (byte) 0xFF;
                SendValue8byte[4] = (byte) 0xFF;
                SendValue8byte[5] = (byte) 0xFF;
                SendValue8byte[6] = (byte) 0xFF;
                SendValue8byte[7] = (byte) 0xFF;
            }else if(v == btnBack){
            	JmpStage();
            	lvStageInit();
            }
        }
    }
    
    private void lvEffectInit() {//add ListView
        ListAdapter mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                ListviewName);
        
    	lvEffect = (ListView) findViewById(R.id.listView);      
        lvEffect.setAdapter(mAdapter);
        lvEffect.setOnItemClickListener(new UIItemClickEvent()); //click event
	}    
    private void lvStageInit() {//add ListView		
    	StageNumber = new String[StageNumberLength];
        for(int z = 0; z < StageNumberLength; z++)
        	StageNumber[z] = "Stage " + z;
        
        ListAdapter mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                StageNumber);
        
        lvStage = (ListView) findViewById(R.id.listView1);
        lvStage.setAdapter(mAdapter);
        lvStage.setOnItemClickListener(new UIItemClickEvent()); //click event
	} 
    class UIItemClickEvent implements AdapterView.OnItemClickListener {  	  
        @Override  
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {  
        	if(arg0 == lvStage){
        		SendValue8byte[1] = (byte) (arg2 + 1);//effect
        		if(effect_count[SendValue8byte[1]-1][1] == -1)
        			JmpController(arg2);
        		else
        			JmpController(effect_count[SendValue8byte[1]-1][1]);
        		if(effect_count[arg2][0] == -1)
        			effect_count[arg2][0] = arg2;     	
        	}
        	else if(arg0 == lvEffect){
        		none = 1;
        		SendValue8byte[0] = (byte) 0x02;
        		SendValue8byte[2] = (byte) (arg2 + 1);
				JmpController(arg2);
        		effect_count[SendValue8byte[1]-1][1] = arg2;
        	}
        }
    }
    
    public void CreateAttrObject(int position){
        tvName = (TextView)findViewById(R.id.textView);
        if(effect_count[SendValue8byte[1]-1][0] == -1)
        	tvName.setText("none");
        else{
	        tvName.setText(ListviewName[position]);
	        llObject = (LinearLayout)findViewById(R.id.viewObj_controller);
	        
	        int i = 0, max = Effects_value_length[position];
	        if(position>0){
	        	i = Effects_value_length[position - 1];
	        	max = Effects_value_length[position]+ i;
	        }     
	        StageBuf = position + 1;//stage
	        ValueBuf = i;//value
	        //Log.i("msg", "i: "+ i +"  max:  "+max);
	        for(; i < max; i++) {
	        	tvAttr[i] = new TextView(this);
	        	tvAttr[i].setText(EffectsValue[i][0]+" : " + Integer.valueOf(EffectsValue[i][2]) +" "+ EffectsValue[i][1]);
	        	llObject.addView(tvAttr[i]);    
		        sbAttr[i] = new SeekBar(this);
		        sbAttr[i].setProgress(Integer.valueOf(EffectsValue[i][2])-Integer.valueOf(EffectsValue[i][4]));
		        sbAttr[i].setMax(Integer.valueOf(EffectsValue[i][3])-Integer.valueOf(EffectsValue[i][4]));      
		        llObject.addView(sbAttr[i]);  
		        SendValue8byte[i - ValueBuf + 3] = (byte) Integer.valueOf(EffectsValue[i][2]).byteValue();//stage	        
		        
		        Log.i("msg", "i:"+ i +"  max:"+ max + "  count:" + i);
		        sbAttr[i].setId(i);
		        sbAttr[i].setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		        	@Override
		        	public void onStopTrackingTouch(SeekBar seekBar) {//拉動SeekBar停止時做的動作        	                               
		        		//send value to stm32
		        		Log.i("msg", "Value:"+seekBar.getProgress());
		        		SendValue8byte[0] = (byte) 0x02;
		        		SendValue8byte[2] = (byte) (StageBuf);//stage
		        		SendValue8byte[seekBar.getId() - ValueBuf + 3] = (byte) (seekBar.getProgress()
		        				+Integer.valueOf(EffectsValue[seekBar.getId()][4]));//value
		        		EffectsValue[seekBar.getId()][2] = Integer.toString(seekBar.getProgress()
		        				+Integer.valueOf(EffectsValue[seekBar.getId()][4]));
		        		//Log.i("msg", "SendValue8byte[1]: "+ seekbar_buf);
		        		//Log.i("msg", "SendValue8byte[?]: "+ (seekBar.getId() - seekbar_buffer + 2));
						try {
							dos = btSocket.getOutputStream();
							dos.write(SendValue8byte);
		                    dos.flush();
						} catch (Exception e) {
						} 
		        	}  
		        	@Override
		        	public void onStartTrackingTouch(SeekBar seekBar) {//開始拉動SeekBar時做的動作
		        	}        
		        	@Override
		        	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {//SeekBar改變時做的動作 
		        		tvAttr[seekBar.getId()].setText(EffectsValue[seekBar.getId()][0]+" : "+(Integer.toString(seekBar.getProgress()
		        				+Integer.valueOf(EffectsValue[seekBar.getId()][4]))+" "+ EffectsValue[seekBar.getId()][1]));
		        	}
		        });  
	        }
			try {
				dos = btSocket.getOutputStream();
				dos.write(SendValue8byte);
                dos.flush();
			} catch (Exception e) {
			} 
        }
    }

    //bluetooth
    private BroadcastReceiver searchDevices = new BroadcastReceiver() {    
        public void onReceive(Context context, Intent intent) {  
            String action = intent.getAction();  
            Bundle b = intent.getExtras();  
            Object[] lstName = b.keySet().toArray();  
  
            // 顯示所有收到的消息及其細節 
            for (int i = 0; i < lstName.length; i++) {  
                String keyName = lstName[i].toString();  
                Log.e(keyName, String.valueOf(b.get(keyName)));  
            }  
            BluetoothDevice device = null;  
            // 搜索設備時，取得設備的MAC地址  
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {  
                device = intent  
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);  
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {  
                    String str = "未配對|" + device.getName() + "|"  +"\n"+"|" 
                            + device.getAddress()+"|" ;  
                    if (lstDevices.indexOf(str) == -1)// 防止重放添加
                        lstDevices.add(str); // 獲取設備名稱和mac地址  
                    adtDevices.notifyDataSetChanged();  
                }  
            }else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){  
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);  
                switch (device.getBondState()) {  
                case BluetoothDevice.BOND_BONDING:  
                    Log.d("BlueToothTestActivity", "正在配對......");  
                    break;  
                case BluetoothDevice.BOND_BONDED:  
                    Log.d("BlueToothTestActivity", "完成配對");  
                    connect(device);//連接設置  
                    break;  
                case BluetoothDevice.BOND_NONE:  
                    Log.d("BlueToothTestActivity", "取消配對");  
                default:  
                    break;  
                }  
            }   
        }  
    };   

    class ItemClickEvent implements AdapterView.OnItemClickListener {  
        @Override  
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,  
                long arg3) {  
            if(btAdapt.isDiscovering())btAdapt.cancelDiscovery();  
            String str = lstDevices.get(arg2);  
            String[] values = str.split("\\|");  
            String address = values[3];  
            Log.e("address", values[3]);  
            BluetoothDevice btDev = btAdapt.getRemoteDevice(address);  
            click = 0;//判斷按搜尋的次數
            try {  
                //Boolean returnValue = false;  
                if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {  
                    //利用反射方法調用BluetoothDevice.createBond(BluetoothDevice remoteDevice);  
                    Method createBondMethod = BluetoothDevice.class  
                            .getMethod("createBond");  
                    Log.d("BlueToothTestActivity", "開始配對");  
                    //returnValue = (Boolean) createBondMethod.invoke(btDev);       
                }else if(btDev.getBondState() == BluetoothDevice.BOND_BONDED){  
                	connect(btDev);  
                }  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }   
    }  
    private void connect(BluetoothDevice btDev) {  
        UUID uuid = UUID.fromString(SPP_UUID);  
        try {  
            btSocket = btDev.createRfcommSocketToServiceRecord(uuid);  
            Log.d("BlueToothTestActivity", "開始連接...");  
            btSocket.connect();      
            Toast.makeText(MainActivity.this, "連線成功", 1000).show(); 
            lvBTDevices.setVisibility(View.GONE);//隱藏
            
            //user code
            start_value = 0;
        	btnJmpStage.setEnabled(true);
        	SendByteInit();
            btnView.setBackgroundResource(R.drawable.blue1);
            //Thread Start
        	RUN_THREAD = true;
            sppReceiver = new SppReceiver(btSocket.getInputStream());
            sppReceiver.start();
            //user code
        } catch (IOException e) {  
            e.printStackTrace();  
        	Toast.makeText(MainActivity.this, "連線失敗", 1000).show(); 
        }  
    }  
    private void Disconnect(){
        SendValue8byte[0] = (byte) 0x03;

    	try {//從新搜尋時先將藍芽斷線
            if (btSocket != null){
            	dos = btSocket.getOutputStream();
        		dos.write(SendValue8byte);
        		dos.flush();
                btSocket.close();
                RUN_THREAD = false;
                Toast.makeText(MainActivity.this, "Disconnect!", 500).show();
            }
           // sppReceiver = new SppReceiver(null);
            //sppReceiver.start();
        } catch (IOException e) {  
            e.printStackTrace();  
        }       
    }
    class ClickEvent implements View.OnClickListener {  
        @Override  
        public void onClick(View v) { 
            if (v == btnSearch){//搜尋藍芽裝置，在BroadcastReceiver顯示結果  
            	if(click == 1){//判斷按搜尋的次數
            		lvBTDevices.setVisibility((View.GONE));//隱藏
            		click =0;
            		return;
            	}else
            		click = 1;	
                if(btAdapt.getState() == BluetoothAdapter.STATE_OFF) {// 如果藍芽還沒開起  
                    Toast.makeText(MainActivity.this, "請先打開藍芽", 1000).show();  
                    return;  
                } 
                //user code
                btn_count = 0;
                btnJmpStage.setEnabled(false);
                btnJmpStage.setText(" ");
                btnView.setBackgroundResource(R.drawable.blue3);
                //user code
                
                if(btAdapt.isDiscovering())  
                    btAdapt.cancelDiscovery(); 
                if(clickOn == 0)
                	Disconnect();
                else
                	clickOn = 0;
                /*try {//從新搜尋時先將藍芽斷線
                    if (btSocket != null){
                    	dos = btSocket.getOutputStream();
                		dos.write(SendValue8byte);
                		dos.flush();
                        btSocket.close();
                        RUN_THREAD = false;
                    }
                   // sppReceiver = new SppReceiver(null);
                    //sppReceiver.start();
                } catch (IOException e) {  
                    e.printStackTrace();  
                }   */    
                lvBTDevices.setVisibility((View.VISIBLE));//顯示
                lstDevices.clear(); 
                
            	Object[] lstDevice = btAdapt.getBondedDevices().toArray();  
                for (int i = 0; i < lstDevice.length; i++) {  
                    BluetoothDevice device = (BluetoothDevice) lstDevice[i];  
                    String str = "已配對|" + device.getName() + "|" +"\n"+"|" 
                            + device.getAddress()+"|";  
                    lstDevices.add(str); // 獲取設置名稱和mac地址  
                    adtDevices.notifyDataSetChanged();  
                }  
                //setTitle("本機藍芽地址：" + btAdapt.getAddress());  
                btAdapt.startDiscovery(); 
            } 
            else if (v == tbtnSwitch) {// 藍芽功能開啟/關閉
                if (tbtnSwitch.isChecked() == false)  
                    btAdapt.enable();  
                else if (tbtnSwitch.isChecked() == true)  
                    btAdapt.disable();  
            } 
            else if (v == btnDis)// 啟動被搜尋功能   
            {  
                Intent discoverableIntent = new Intent(  
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);  
                discoverableIntent.putExtra(  
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);  
                startActivity(discoverableIntent);  
            } 
            else if (v == btnExit)//關閉程式 
            {  
                try {  
                    if (btSocket != null)  
                        btSocket.close();  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
                MainActivity.this.finish();  
            }  
            else if (v == btnSend) //Send hellp 
            {  
            	//OutputStream dos;
            	try {
            		dos = btSocket.getOutputStream();
            		dos.write(SendValue8byte);
            		//dos.write(a.getBytes());
            		dos.flush();
            	} catch (Exception e) {
            	} 
            } 

        }  
    }  
    
    class SppReceiver extends Thread {
    	InputStream input = null;
    	public SppReceiver(InputStream in){
    		input = in;
    		Log.i("tag", "SppReceiver");
    	}
    	//receiver
    	public void run(){
    		byte[] data =new byte[1024];
    		int length = 0, endIdx;
    		
    		StringBuilder curMsg = new StringBuilder();
    		String end = "\n";
    		
    		if(input == null){
    			Log.i("tag", "InputStream null");
    			return;
    		}
    		while(RUN_THREAD){
    			try{
    				while (-1 != (length = input.read(data))){ //收到/n的時候才代表收完
    				 	curMsg.append(new String(data, 0, length, Charset.forName("ISO-8859-1")));
    				 	endIdx = curMsg.indexOf(end);
    				 	if (endIdx != -1){
    				 		String fullMessage = curMsg.substring(0, endIdx + end.length());
    						curMsg.delete(0, endIdx + end.length());
    				                        // Now send fullMessage
    				                        // Send the obtained bytes to the UI Activity
    						//msg = fullMessage;//new String(data, 0, length, "ASCII");
    						msg = new String(fullMessage.getBytes(), 0, fullMessage.length(), "ASCII");
    						btHandler.sendEmptyMessage(0);
    				 	}
    				 }
    			} catch (IOException e){
    				Log.i("tag", "SppReceiver_disconnect");
    				e.printStackTrace();
    			}
    		}
    	}
    }
    Handler btHandler = new Handler() {//Thread線程發出Handler消息，通知更新UI。
    	public void handleMessage(Message m){
    		Log.i("tag", "receiver data : " + msg);
			textview.setText(msg);
			if(msg.length()>20 && start_value == 0)
			{
				jsonObj = msg;
				msg = "";
				start_value++;
				btn_count++;
				try{
					if(CreateJSON()== false){
						start_value = 0;
						btn_count = 0;
						Toast.makeText(MainActivity.this, "JSON格式錯誤!!!請重新連線!", 500).show();
						BTObjectInit();  
						UIObjectInit();
					}
					else{
						Toast.makeText(MainActivity.this, "JSON GET!!!", 1000).show(); 
						try{
							btnJmpStage.setText(object.getString("Name"));
						}catch(JSONException e){
						}
					}
				}catch(Exception e){
					btn_count = 0;
					start_value = 0;
					Toast.makeText(MainActivity.this, "JSON格式錯誤!!!請重新連線!", 500).show(); 
					BTObjectInit();  
					UIObjectInit();
				}
			}
    	}
    };
    //bluetooth
}
