package io.yunba.example;

import io.yunba.android.manager.YunBaManager;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.json.JSONException;

import android.app.Application;
import android.util.Log;

public class YunBaApplication extends Application {
	private final static String TAG = "YunBaApplication";
	@Override
	public void onCreate() {
		super.onCreate();
		
		YunBaManager.start(getApplicationContext());

        YunBaManager.subscribe(getApplicationContext(), new String[]{"t1"}, new IMqttActionListener() {

            @Override
            public void onSuccess(IMqttToken arg0) {
                Log.d(TAG, "Subscribe topic succeed");
            }

            @Override
            public void onFailure(IMqttToken arg0, Throwable arg1) {
                Log.d(TAG, "Subscribe topic failed") ;
            }
        });				//鍒拌繖閲屼负娣诲姞
		
		
		initConnectStatus();
		startBlackService();
	}

	private void initConnectStatus() {
		//set MainActivity title status
		SharePrefsHelper.setString(getApplicationContext(), "connect_status", "");
	}

	private void startBlackService() {
		YunBaManager.start(getApplicationContext());
		
		IMqttActionListener listener = new IMqttActionListener() {
			
			@Override
			public void onSuccess(IMqttToken asyncActionToken) {
				String topic = DemoUtil.join(asyncActionToken.getTopics(), ",");
				Log.d(TAG, "Subscribe succeed : " + topic);
//				DemoUtil.showToast( "Subscribe succeed : " + topic, getApplicationContext());
				StringBuilder showMsg = new StringBuilder();
				showMsg.append("subscribe succ锛�").append(YunBaManager.MQTT_TOPIC)
						.append(" = ").append(topic);
			}
			
			@Override
			public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
				String msg =  "Subscribe failed : " + exception.getMessage();
				Log.d(TAG, msg);
//				DemoUtil.showToast(msg, getApplicationContext());
//				
				
			}
		};
		
		//for test
		YunBaManager.subscribe(getApplicationContext(), new String[]{"t1", "t2", "t3"}, listener);
	}


	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	

}
