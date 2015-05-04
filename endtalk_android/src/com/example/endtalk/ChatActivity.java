package com.example.endtalk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class ChatActivity extends Activity {



	//transplant
	private ChattingAdapter chatHistoryAdapter;
	private List<ChatMessage> messages = new ArrayList<ChatMessage>();
	private ListView chatHistoryLv;
	private ImageView sendImageIv;
	private ImageView captureImageIv;
	//transplant define over
	
	
	
	class user {
		int id;
		int to;
		int last_id;
		String load_online;			//yes / null
		volatile String typing;		//yes / null
		String send_content;		//即将发送的数据
	}
	user usernow = new user();
	String temp_content = null;
	
	public volatile boolean event_stop = false;			//(可行)给load_event判断是否停止继续循环，用于主动disconnect
	public volatile boolean isExit = false;
	StringBuilder totalmsg = new StringBuilder(21);
	private EditText ed_msg = null;
    private Button btn_send = null;
    private Button btn_leave = null;
    private Button btn_reconnect = null;
    private ImageButton iv_exit = null;
    private ImageButton iv_share = null;
    
    InputMethodManager imm = null;
    
    //private Button btn_exit = null;			//用上面两个ImageView代替了
    //private Button btn_share = null;
    //private TextView tv_msg = null;
    //private ScrollView sview = null;
    private TextView tv_online_num = null;
    private TextView tv_typing = null;
    //private TextView textview_selected = null;
    public static int query_time = 1000;			//ms
    //String url = "http://192.168.1.66";
    String url = "http://test.aexx.net";
    String shorturl = "http://t.cn/Rhijgrb";
    int online_num;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_activity);
		
		ShareSDK.initSDK(this);
		
		System.out.println("oncreate");
		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);  
		Timer timer = new Timer();
		timer.schedule(new TimerforLoad_online(), 0, 30000);
		
		chatHistoryLv = (ListView) findViewById(R.id.chatting_history_lv);
		chatHistoryAdapter = new ChattingAdapter(this, messages);
		chatHistoryLv.setAdapter(chatHistoryAdapter);
		
//		tv_msg = (TextView) findViewById(R.id.textview1);
//		tv_msg.getPaint().setFakeBoldText(true);								//字体加粗(含中文)
//		tv_msg.setMovementMethod(ScrollingMovementMethod.getInstance());		//有用！使光标移到最新出现的文字
		//sview = (ScrollView) findViewById(R.id.scrollview);
		tv_online_num = (TextView) findViewById(R.id.textview3);
		tv_typing = (TextView) findViewById(R.id.textview4);
		//tv_typing.setVisibility(View.INVISIBLE);
		ed_msg = (EditText) findViewById(R.id.edittext1);
		btn_send = (Button) findViewById(R.id.button1);
		btn_leave = (Button) findViewById(R.id.button2);
		btn_reconnect = (Button) findViewById(R.id.button3);
		iv_exit = (ImageButton) findViewById(R.id.ib_exit);
		iv_share = (ImageButton) findViewById(R.id.ib_share);
		//btn_exit = (Button) findViewById(R.id.button4);
		//btn_share = (Button) findViewById(R.id.buttonsdk);
		
		
		btn_send.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				if (v.getId() == btn_send.getId()) {
					if (usernow.send_content == null) {
						usernow.send_content = ed_msg.getText().toString();	//获取当前输入内容
						String sendStr;
						if (usernow.send_content != null
								&& (sendStr = usernow.send_content.trim().replaceAll("\r", "").replaceAll("\t", "")
								.replaceAll("\n", "").replaceAll("\f", "")) != "") {
							send(sendStr);
						}
						ed_msg.setText("");
					} else {												//如果用户输入太快
						temp_content = ed_msg.getText().toString();
						String sendStr;
						if (temp_content != null
								&& (sendStr = temp_content.trim().replaceAll("\r", "").replaceAll("\t", "")
								.replaceAll("\n", "").replaceAll("\f", "")) != "") {
							send(sendStr);
						}
					}

				} else if (v.getId() == sendImageIv.getId()) {
					Intent i = new Intent();
					i.setType("image/*");
					i.setAction(Intent.ACTION_GET_CONTENT);
					startActivityForResult(i, Activity.DEFAULT_KEYS_SHORTCUT);
					//close a pic
				} else if (v.getId() == captureImageIv.getId()) {
					Intent it = new Intent("android.media.action.IMAGE_CAPTURE");
					startActivityForResult(it, Activity.DEFAULT_KEYS_DIALER);
				}
			}
		});
		
		/*btn_send.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				usernow.send_content = ed_msg.getText().toString();
				send(usernow.send_content);
				ed_msg.setText("");
			}
		});*/
		btn_leave.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				System.out.println("leave button");
				if (ChatActivity.this.getCurrentFocus() !=  null ) {
	                if (ChatActivity.this.getCurrentFocus().getWindowToken() !=  null )
	                    imm.hideSoftInputFromWindow(ChatActivity.this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);                
	            }
				new Thread(new disconnect()).start();
				/*Handler handler = new Handler();
				Runnable abort = new disconnect();
				handler.post(abort);*/
				event_stop = true;
				show_notification(getResources().getString(R.string.selfdisconnect));
			}
		});
		btn_reconnect.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				btn_reconnect.setVisibility(View.GONE);
				
				messages.clear();
				chatHistoryLv.setAdapter(chatHistoryAdapter);
				
				new Thread(new beginning()).start();
				/*Handler handler = new Handler();				//from Internet
				Runnable beginthread = new beginning();
				handler.post(beginthread);*/
			}
		});
//		btn_exit.setOnClickListener(new Button.OnClickListener(){
		iv_exit.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				System.exit(0);
			}
		});
//		btn_share.setOnClickListener(new Button.OnClickListener(){
		iv_share.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showShare();
			}			
		});
		System.out.println("oncreate line 84");
		new Thread(new beginning()).start();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		System.out.println("keydown");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(!isExit) {										//(以后还会有用)连续两次返回键退出
	            isExit = true;
	            Toast.makeText(getApplicationContext(), R.string.repeatbackkey, Toast.LENGTH_SHORT).show();
	            new Handler().postDelayed(new Runnable() {
	                public void run(){
	                    isExit = false;
	                }
	            }, 2000);
	            return false;
			}
			else System.exit(0);
		}
		/*//onkeydown疑似只相应下面一排功能键
		else if (keyCode != KeyEvent.KEYCODE_SEARCH && keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_HOME) {
			usernow.typing = "yes";
			System.out.println("onkeydown-typing "+usernow.typing);
			new Handler().postDelayed(new Runnable() {
				public void run(){
					usernow.typing = null;
				}
			}, 2000);
		}*/
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if  (event.getAction() == MotionEvent.ACTION_DOWN) {
            System.out.println("down" );
            if (this.getCurrentFocus() !=  null ) {
                if (this.getCurrentFocus().getWindowToken() !=  null )
                    imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);                
            }
        }
		return super.onTouchEvent(event);
	}


	class beginning implements Runnable {				//最开始的开始

		@Override
		public void run() {
			// TODO Auto-generated method stub
			String newurl = url+"/online.php";
			
			/*btn_reconnect.setEnabled(false);			//(是否有用未知)防止被点到触发
			btn_send.setVisibility(View.VISIBLE);
			btn_leave.setVisibility(View.VISIBLE);
			ed_msg.setVisibility(View.VISIBLE);*/
			Message msg = new Message();
			msg.what = 111;
			myviewhandler.sendMessage(msg);
			StringBuilder sb;
			System.out.println(newurl);
			show_notification(getResources().getString(R.string.logining));
			List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
			sb = newnew_getResult(newurl, params);
			System.out.println(sb+" beginning");
			
			if (sb == null) {							//判断能否连接上，不行则显示reconnect
				//show_notification(getResources().getString(R.string.failconnection));
				Message msgtoast = new Message();
				msgtoast.obj = getResources().getString(R.string.failconnection);
				toasthandler.sendMessage(msgtoast);
				
				Message msgbreak = new Message();
				msgbreak.what = 000;
				myviewhandler.sendMessage(msgbreak);
			} else {			
				Message message = new Message();
	            message.obj = sb.toString();
	            mysmallhandler.sendMessage(message);
	            System.out.println("endofbeginning, startofinit");
				init_chat();
			}
		}
	}

	
	public void init_chat() {
		System.out.println(usernow.id+" init_chat1");
		if (usernow.id > 0)
			return ;
		event_stop = false;
		totalmsg.delete(0, totalmsg.length());
		usernow.id = 0;
		usernow.to = 0;
		//$get(start.php);
		String newurl = url+"/start.php";
		System.out.println(newurl+" init_chat2");
		
		StringBuilder sb;
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		sb = newnew_getResult(newurl, params);
		System.out.println(stringToAscii(sb.toString()));
		int id = Integer.parseInt(sb.toString());
		if (id > 0) {
			usernow.id = id;
			show_notification(getResources().getString(R.string.logined));
			show_notification(getResources().getString(R.string.waiting));
			
			/*ed_msg.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View arg0) {					//监听EditText
					// TODO Auto-generated method stub
					usernow.typing = "yes";
					new Handler().postDelayed(new Runnable() {
						public void run(){
							usernow.typing = null;
						}
					}, 2000);
				}
			});*/
			System.out.println("init_chat ---> load_event "+usernow.id);
			load_event(usernow.id);
			
			
			if (usernow.id > 0 && usernow.to > 0) {
				Message msg = new Message();
				msg.obj = getResources().getString(R.string.thanksforusing);
				toasthandler.sendMessage(msg);
			}
		}
		else {
			//Toast.makeText(ChatActivity.this, "ID"+id+" "+getResources().getString(R.string.failinitialize), Toast.LENGTH_LONG).show();
			Message msg = new Message();
			msg.obj = "ID"+id+" "+getResources().getString(R.string.failinitialize);
			toasthandler.sendMessage(msg);
		}
	}
	
	
	public void load_event(int id) {
		while(!event_stop) {
			if (usernow.id == 0 || usernow.id != id)
				return ;
			//$post(event.php);
			String newurl = url+"/event.php";
			System.out.println("newurl");
			StringBuilder sb;
			List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
			params.add(new BasicNameValuePair("id",Integer.toString(usernow.id)));
			params.add(new BasicNameValuePair("to",Integer.toString(usernow.to)));
			params.add(new BasicNameValuePair("last_id",Integer.toString(usernow.last_id)));			
			params.add(new BasicNameValuePair("typing",usernow.typing = getTypingStatus()));
			params.add(new BasicNameValuePair("load_online",usernow.load_online));
			if (temp_content == null) 
				params.add(new BasicNameValuePair("send_content",usernow.send_content));
			else {
				params.add(new BasicNameValuePair("send_content",temp_content));
			}
			sb = newnew_getResult(newurl, params);
			usernow.send_content = null;
			System.out.println(sb+" load_event1");						//for test
			try {
				JSONObject jsonObject = new JSONObject(sb.toString());
				/*class dta {
					String send_status;
					String send_content;
					int online;
				}
				dta datanow = new dta();
				datanow.online = jsonObject.getInt("online");
				datanow.send_status = jsonObject.getString("send_status");
				datanow.send_content = jsonObject.getString("send_content");*/
				int online = 0;
				try {online = jsonObject.getInt("online");} catch (Exception e) {e.printStackTrace();}
				JSONArray jsonArray= jsonObject.getJSONArray("events");		//获得数组
				System.out.println(jsonArray+" load_event2");
				//ArrayList<HashMap<String, String>>mArray = new ArrayList<HashMap<String,String>>();
				
				
				usernow.load_online = null;
				if(sb != null && jsonArray != null) {
					usernow.send_content = null;
					if (online > 0) {
						//textview_selected = tv_online_num;
						Message message = new Message();
			            message.obj = online;
			            mysmallhandler.sendMessage(message);
						//tv_online_num.setText(online);
					}
					//char datatemp[];
					System.out.println(jsonArray.length()+" beforeloop");
					for (int i=0; i< jsonArray.length(); i++) {
						JSONObject jsonItem = jsonArray.getJSONObject(i);
						int id_event = jsonItem.getInt("id");
						int from_event = jsonItem.getInt("from");
						int to_event = jsonItem.getInt("to");
						int time_event = jsonItem.getInt("time");
						String type_event = jsonItem.getString("type");
						String content_event = jsonItem.getString("content");
						
						
						usernow.last_id = id_event;
						if (type_event.equals("connected") && from_event > 0) {
							System.out.println("for 1");
							usernow.to = from_event;
							show_notification(getResources().getString(R.string.connected));
							usernow.load_online = "yes";
						}
						else if (type_event.equals("typing")) {
							System.out.println("for 2");
							show_typing(1, null);
							/*new Handler().postDelayed(new Runnable() {			//change to view.postdelay
								public void run(){
									show_typing(0, null);
								}
							}, 2000);*/
						}
						else if (type_event.equals("msg") && content_event != null) {
							System.out.println("for 3");
							showMessage(content_event, 0);
							show_typing(0, null);
						}
						else if (type_event.equals("disconnect")) {
							System.out.println("for 4");
							usernow.id = 0;
							usernow.to = 0;
							show_notification(getResources().getString(R.string.disconnect));
							//show_reconnect();
							Message msg = new Message();
							msg.what = 000;
							myviewhandler.sendMessage(msg);
						}
					}
				}
				//Timer timerforload = new Timer();
				//timerforload.schedule(new TimerforEvent_loop(), 0, 1000);
				Thread.sleep(query_time);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	
	private void showMessage(String sendStr, int flag) {
		System.out.println("message " + sendStr);
		if (flag == 1) {
			messages.add(new ChatMessage(ChatMessage.MESSAGE_TO, sendStr));
		}
		else if (flag == 0) {
			messages.add(new ChatMessage(ChatMessage.MESSAGE_FROM, sendStr));
		}
		//chatHistoryAdapter.notifyDataSetChanged();			//not the same Thread
		Message msg = new Message();
		msg.what = 1;
		myshowhandler.sendMessage(msg);
	}
	
	
	public void send(String s) {
		if (s == null || usernow.to == 0 || usernow.id == 0)
			return ;
		showMessage(s,1);
		usernow.typing = null;
	}
	
	
	/*public void show_msg(String s, int me) {
		System.out.println("send " + s);
		if(me == 0) {
			//totalmsg.append("\n"+getResources().getString(R.string.send_stranger)+s);
			totalmsg.append("\n陌生人："+s);
		}
		else if(me == 1) {
			//totalmsg.append("\n"+getResources().getString(R.string.send_me)+s);
			totalmsg.append("\n我："+s);
		}
		else {
			totalmsg.append("\n"+s);
		}
		System.out.println("totalmsg " + totalmsg);
		Message message = new Message();
        message.obj = totalmsg;
        mybighandler.sendMessage(message);
	}*/
	
	
	public void show_notification(String str) {
		//show_msg(str,2);
		show_typing(2, str);
	}
	
	
	public void show_typing(int flag, String str) {
		Message msg = new Message();
		if (flag == 1)
			msg.what = 11;
		else if (flag == 0)
			msg.what = 00;
		else {
			msg.what = 22;
			msg.obj = str;
		}
		mytypinghandler.sendMessage(msg);
	}
	
	
	public void show_reconnect() {
		btn_send.setVisibility(View.GONE);
		btn_leave.setVisibility(View.GONE);
		ed_msg.setVisibility(View.GONE);
		System.out.println("reconnect");
		btn_reconnect.setVisibility(View.VISIBLE);

//		btn_exit.setVisibility(View.VISIBLE);
//		btn_share.setVisibility(View.VISIBLE);
		iv_exit.setVisibility(View.VISIBLE);
		iv_share.setVisibility(View.VISIBLE);
	}
	
	
	public void reconnect() {
		init_chat();
	}
	
	
	public String getTypingStatus() {
		String str = "yes";
		//if (ed_msg.getText().toString() != null) {
		if (TextUtils.isEmpty(ed_msg.getText())) 
			return null;
		else {
			System.out.println("typingstatus "+ed_msg.getText().toString()+"...");
			return str;
		}
	}
	
	/*
	public void disconnect() {
		if (usernow.id == 0 || usernow.to == 0) return;
		String newurl = url+"/disconnect.php";
		StringBuilder sb;
		List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
		params.add(new BasicNameValuePair("id",Integer.toString(usernow.id)));
		params.add(new BasicNameValuePair("to",Integer.toString(usernow.to)));
		sb = newnew_getResult(newurl, params);
		System.out.println(sb+" disconnect");						//for test
		String feedback = sb.toString();
		if (feedback.equals("win"))
			show_reconnect();
		else {
			Toast.makeText(ChatActivity.this, feedback, Toast.LENGTH_LONG).show();
		}
	}*/
	
	
	public class disconnect implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			//if (usernow.id == 0 || usernow.to == 0) return;
			if (usernow.id == 0 || usernow.to == 0) {
				Message msg = new Message();
				msg.what = 000;
				myviewhandler.sendMessage(msg);
			}
			else {
				String newurl = url+"/disconnect.php";
				StringBuilder sb;
				List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
				params.add(new BasicNameValuePair("id",Integer.toString(usernow.id)));
				params.add(new BasicNameValuePair("to",Integer.toString(usernow.to)));
				System.out.println(Integer.toString(usernow.id)+" "+Integer.toString(usernow.to));
				sb = newnew_getResult(newurl, params);
				System.out.println(sb+" disconnect");						//for test
				usernow.id = usernow.to = 0;
				String feedback = sb.toString();
				if (feedback.equals("win")) {
					//show_reconnect();
					Message msg = new Message();
					msg.what = 000;
					myviewhandler.sendMessage(msg);
				}
				else {
					//Toast.makeText(ChatActivity.this, feedback, Toast.LENGTH_LONG).show();
					Message msg = new Message();
					msg.obj = feedback;
					toasthandler.sendMessage(msg);
				}
			}
		}
	}
	
	
	
				//http://blog.csdn.net/u012803869/article/details/25055471		函数原型在此
	private StringBuilder newnew_getResult (String validateUrl, List<BasicNameValuePair> params) {
		
		HttpClient httpClient = new DefaultHttpClient();
		
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 5000);			//设置链接超时
        
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);				//设置读取超时(增大以避免超时，原为5秒)
        
        HttpPost httpRequst = new HttpPost(validateUrl);
        
        //JSONObject jsonObject = null;
        StringBuilder builder = null;

        try {
        	httpRequst.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));		//发送请求
            
            HttpResponse response = httpClient.execute(httpRequst);					//得到响应
            int code;
            System.out.println(code = response.getStatusLine().getStatusCode());
            if(code == 200)		//返回值如果为200的话则证明成功的得到了数据
            {
            	builder = new StringBuilder();
            	BufferedReader buffer = new BufferedReader(				//将得到的数据进行解析
            			new InputStreamReader(response.getEntity().getContent()));
            	
            	for(String s =buffer.readLine(); s!= null; s = buffer.readLine())
            	{
            		builder.append(s);
	            	}
            	System.out.println(builder.toString()+" newnew_getResult");
	                      
            	return builder;
            }
        }catch (Exception e) {
        	e.printStackTrace();
        }
        return builder;
	}
	
	
	private void showShare() {        
        OnekeyShare oks = new OnekeyShare();
		oks.setNotification(R.drawable.ic_launcher, "ShareSDK demo");
		oks.setAddress("12345678901");
		oks.setTitle(getResources().getString(R.string.share_title));
		oks.setTitleUrl(url);
		oks.setText(getResources().getString(R.string.share_text));
		oks.setImagePath("/sdcard/fall_ustc.jpg");
		//oks.setImageUrl("http://img.appgo.cn/imgs/sharesdk/content/2013/07/25/1374723172663.jpg");
		oks.setImageUrl("http://test.aexx.net/images/android.jpg");
		oks.setUrl(url);
		oks.setComment("comment");
		oks.setSite("site");
		oks.setSiteUrl("http://www.baidu.com");
		oks.setLatitude(23.122619f);
		oks.setLongitude(113.372338f);
		oks.setSilent(false);
		//oks.setSilent(true);
		oks.show(this);
	}
	

	
	
	
	public class TimerforEvent_loop extends java.util.TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			System.out.println("timertask --> load_event");
			load_event(usernow.id);
		}
	}
	
	public class TimerforLoad_online extends java.util.TimerTask {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			usernow.load_online = "yes";
		}
	}

	
	Handler mysmallhandler = new Handler() {
    	public void handleMessage(Message msg){
    		System.out.println("-->"+msg.obj);
    		tv_online_num.setText(" "+msg.obj.toString());
    	}
    };
	/*Handler mybighandler = new Handler() {
		public void handleMessage(Message msg){
			System.out.println("---->"+msg.obj);
			tv_msg.setText(msg.obj.toString());
			CharSequence text = tv_msg.getText();					//保证页面滚动到最下方
			if (text instanceof Spannable) {
				Spannable spanText = (Spannable)text;
				Selection.setSelection(spanText, text.length());
			}
			//sview.scrollTo(0, tv_msg.getHeight());
			//sview.scrollTo(0, tv_msg.getBottom());
		}
	};*/
	Handler myviewhandler = new Handler() {
		public void handleMessage(Message msg){
			if (msg.what == 000) {
				show_reconnect();
			}
			else if (msg.what == 111) {
				btn_reconnect.setVisibility(View.GONE);
//				btn_exit.setVisibility(View.GONE);
//				btn_share.setVisibility(View.GONE);
				
				iv_exit.setVisibility(View.GONE);
				iv_share.setVisibility(View.GONE);
				btn_send.setVisibility(View.VISIBLE);
				btn_leave.setVisibility(View.VISIBLE);
				ed_msg.setVisibility(View.VISIBLE);
			}
		}
	};
	Handler mytypinghandler = new Handler() {
		public void handleMessage(Message msg){
			if (msg.what == 00) {
				//tv_typing.setVisibility(View.INVISIBLE);
				tv_typing.setText("");
			}
			else if (msg.what == 11) {
				//tv_typing.setVisibility(View.VISIBLE);
				tv_typing.setText(getResources().getString(R.string.othertyping));
				tv_typing.postDelayed(new Runnable() {
					public void run() {
						tv_typing.setText("");
					}
				}, 2000);
				System.out.println("typinghandler "+R.string.othertyping);
			}
			else {
				tv_typing.setText(msg.obj.toString());
				System.out.println("typinghandler "+msg.obj.toString());
			}
		}
	};
	Handler myshowhandler = new Handler() {
		public void handleMessage(Message msg){
			if (msg.what == 1)
				chatHistoryAdapter.notifyDataSetChanged();
		}
	};
	Handler toasthandler = new Handler() {
		public void handleMessage(Message msg) {
			Toast.makeText(ChatActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
		}
	};

	public static String stringToAscii(String value)
	 {
	  StringBuffer sbu = new StringBuffer();
	  char[] chars = value.toCharArray();
	  for (int i = 0; i < chars.length; i++) {
	   if(i != chars.length - 1)
	   {
	    sbu.append((int)chars[i]).append(",");
	   }
	   else {
	    sbu.append((int)chars[i]);
	   }
	  }
	  return sbu.toString();
	 }
}