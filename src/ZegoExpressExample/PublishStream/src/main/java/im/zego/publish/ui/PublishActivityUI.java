package im.zego.publish.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import org.json.JSONObject;

import im.zego.publish.R;
import im.zego.publish.databinding.ActivityPublishBinding;
import im.zego.publish.databinding.PublishInputStreamIdLayoutBinding;

import java.util.Date;

import im.zego.common.GetAppIDConfig;
import im.zego.common.entity.SDKConfigInfo;
import im.zego.common.entity.StreamQuality;
import im.zego.common.ui.BaseActivity;
import im.zego.common.util.AppLogger;
import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPublishChannel;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoStreamQualityLevel;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoUser;

public class PublishActivityUI extends BaseActivity {


    public static ZegoViewMode viewMode = ZegoViewMode.ASPECT_FILL;
    private ActivityPublishBinding binding;
    private PublishInputStreamIdLayoutBinding layoutBinding;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();
    private ZegoExpressEngine engine;
    private String streamID;
    private String userID;
    private String userName;
    private String roomID;
    private String mStreamID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_publish);

        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);
        binding.swMic.setChecked(true);
        binding.swCamera.setChecked(true);
        binding.swFrontCamera.setChecked(true);
        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(R.string.tx_start_publish));

        AppLogger.getInstance().i("createEngine");
        // 初始化SDK
        engine = ZegoExpressEngine.createEngine(GetAppIDConfig.appID, GetAppIDConfig.appSign, true, ZegoScenario.GENERAL, getApplication(), null);

        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
        userID = "userid-" + randomSuffix;
        userName = "username-" + randomSuffix;

        // 调用sdk 开始预览接口 设置view 启用预览
        ZegoCanvas zegoCanvas = new ZegoCanvas(binding.preview);
        engine.startPreview(zegoCanvas);

        engine.setEventHandler(new IZegoEventHandler() {

            @Override
            public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
                // 推流状态更新，errorCode 非0 则说明推流失败
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/308.html</a>

                if (errorCode == 0) {
                    binding.title.setTitleName(getString(R.string.tx_publish_success));
                    AppLogger.getInstance().i("推流成功, streamID : %s", streamID);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {
                    binding.title.setTitleName(getString(R.string.tx_publish_fail));
                    AppLogger.getInstance().i("推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onPublisherQualityUpdate(String streamID, im.zego.zegoexpress.entity.ZegoPublishStreamQuality quality) {
                /**
                 * 推流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} 修改回调频率
                 */
                streamQuality.setFps(String.format("帧率: %f", quality.videoSendFPS));
                streamQuality.setBitrate(String.format("码率: %f kbs", quality.videoKBPS));
                streamQuality.setHardwareEncode(String.format("硬编: %b", quality.isHardwareEncode));
                streamQuality.setNetworkQuality(String.format("网络质量: %s", getQuality(quality.level)));
            }

            @Override
            public void onPublisherVideoSizeChanged(int width, int height, ZegoPublishChannel channel) {
                // 当采集时分辨率有变化时，sdk会回调该方法
                streamQuality.setResolution(String.format("分辨率: %dX%d", width, height));
            }

            @Override
            public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {

            }

        });


        // 监听摄像头与麦克风开关
        binding.swCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableCamera(isChecked);
                    engine.enableCamera(isChecked);
                }
            }
        });

        binding.swMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMic(isChecked);
                    engine.enableAudioCaptureDevice(isChecked);
                }
            }
        });

        binding.swFrontCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableFrontCamera(isChecked);
                    engine.useFrontCamera(isChecked);
                }
            }
        });


    }

    private String getQuality(ZegoStreamQualityLevel level) {
        switch (level){
            case EXCELLENT:
                return "极好";
            case GOOD:
                return "优";
            case MEDIUM:
                return "中";
            case BAD:
                return "差";
            case DIE:
                return "极差";
        }

        return "无数据";
    }


    @Override
    protected void onResume() {
        //  某些华为手机上，应用在后台超过2分钟左右，华为系统会把摄像头资源给释放掉，并且可能会断开你应用的网络连接,
        //  需要做前台服务来避免这种情况https://blog.csdn.net/Crazy9599/article/details/89842280
        //  所以当前先关闭再重新启用摄像头来规避该问题
        if (binding.swCamera.isChecked()) {
            engine.enableCamera(false);
            engine.enableCamera(true);
        }
        if (binding.swMic.isChecked()) {
            engine.enableAudioCaptureDevice(false);
            engine.enableAudioCaptureDevice(true);
        }
        super.onResume();
    }

    public void goSetting(View view) {
        PublishSettingActivityUI.actionStart(this);
    }

    @Override
    protected void onDestroy() {
        // 停止所有的推流和拉流后，才能执行 logoutRoom
        engine.stopPreview();
        engine.stopPublishingStream();
        // 当用户退出界面时退出登录房间
        engine.logoutRoom(roomID);
        super.onDestroy();
    }

    /**
     * button 点击事件
     * 开始推流
     */
    public void onStart(View view) {
        mStreamID = layoutBinding.edStreamId.getText().toString();
        roomID = layoutBinding.edRoomId.getText().toString();
        streamQuality.setRoomID(String.format("RoomID : %s", roomID));
        ZegoUser user = new ZegoUser(userID, userName);
        engine.loginRoom(roomID, user, null);

        streamID = layoutBinding.edStreamId.getText().toString();
        // 隐藏输入StreamID布局
        hideInputStreamIDLayout();

        streamQuality.setStreamID(String.format("StreamID : %s", streamID));
        // 开始推流
        engine.startPublishingStream(streamID);

    }

    private void hideInputStreamIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
        hideKeyboard(PublishActivityUI.this, layoutBinding.edStreamId);
    }

    private void showInputStreamIDLayout() {
        // 显示InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }

    //隐藏虚拟键盘
    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

        }
    }


    public static void actionStart(Activity activity) {
        Intent intent = new Intent(activity, PublishActivityUI.class);
        activity.startActivity(intent);
    }


}
