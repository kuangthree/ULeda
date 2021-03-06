package ecnu.uleda.view_controller;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.lbssearch.object.Location;
import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ecnu.uleda.R;
import ecnu.uleda.function_module.UserOperatorController;
import ecnu.uleda.tool.UPublicTool;
import ecnu.uleda.exception.UServerAccessException;
import ecnu.uleda.model.UTask;
import ecnu.uleda.function_module.UTaskManager;
import ecnu.uleda.function_module.Navigation;
import ecnu.uleda.view_controller.task.activity.TaskDetailsActivity;

public class UMainFragment extends Fragment implements View.OnTouchListener,View.OnClickListener{
    //主界面的Fragment
    private Button[] mButtons;
    private FloatingActionButton mFab;
    private int mCurrent;
    private MapView mMapView;
    private TencentMap mTencentMap;
    private ArrayList<Marker> mMarkers=new ArrayList<>();
    private UTaskManager mUTaskManager = UTaskManager.getInstance();
    private TencentLocationManager mLocationManager=null;
    private Navigation mNavigation=null;
    private LatLng mCurrentDestination=null;
    private LatLng mCenter=new LatLng(31.2284994411d,121.4063922732d);
    //下弹出动画
    private View mPanelView;
    private View mCloseButton;
    private View mBlank;
    private View mTask;
    private View mProject;
    private View mActivity;

    private Animation mButtonInAnimation;
    private Animation mButtonOutAnimation;
    private Animation mButtonScaleLargeAnimation;
    private Animation mButtonScaleSmallAnimation;
    private Animation mCloseRotateAnimation;
    private void initView(View v)
    {
        mPanelView = v.findViewById(R.id.panel);
        mCloseButton = v.findViewById(R.id.close);

        mBlank = v.findViewById(R.id.blank);

        mActivity =  v.findViewById(R.id.activity);
        mProject =  v.findViewById(R.id.project);
        mTask = v.findViewById(R.id.task);

        mBlank.setOnClickListener(this);
        mCloseButton.setOnClickListener(this);

        mActivity.setOnTouchListener(this);
        mProject.setOnTouchListener(this);
        mTask.setOnTouchListener(this);
    }
    private void initAnimation() {

        mButtonInAnimation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.button_in);
        mButtonOutAnimation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.button_out);
        mButtonScaleLargeAnimation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.button_scale_small);
        mButtonScaleSmallAnimation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.button_scale_large);
        mCloseRotateAnimation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.close_rotate);

        mButtonOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // 6个按钮的退出动画执行完毕后，将面板隐藏
                mPanelView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
    private void openPanelView() {
        hideFab();
        AlphaAnimation animation = new AlphaAnimation(0, 1);
        animation.setDuration(100);
        mPanelView.setVisibility(View.VISIBLE);
        mPanelView.startAnimation(animation);
        mProject.startAnimation(mButtonInAnimation);
        mTask.startAnimation(mButtonInAnimation);
        mActivity.startAnimation(mButtonInAnimation);

        mCloseButton.startAnimation(mCloseRotateAnimation);
    }
    private void closePanelView() {
        showFab();
        // 给6个按钮添加退出动画
        mProject.startAnimation(mButtonOutAnimation);
        mTask.startAnimation(mButtonOutAnimation);
        mActivity.startAnimation(mButtonOutAnimation);
        Animation animation = new AlphaAnimation(1, 0);
        animation.setDuration(100);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPanelView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mPanelView.startAnimation(animation);
    }

    @Override
    public boolean onTouch(final View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 手指按下，按钮执行放大动画
                v.startAnimation(mButtonScaleLargeAnimation);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 手指移开，按钮执行缩小动画
                v.startAnimation(mButtonScaleSmallAnimation);
                gotoPost(v.getId());
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 缩小动画执行完毕后，将按钮的动画清除。这里的150毫秒是缩小动画的执行时间。
                        v.clearAnimation();
                    }
                }, 150);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.float_button:
                // 添加按钮
                openPanelView();

                break;

            case R.id.close:// 关闭按钮
                closePanelView();

                break;
            case R.id.blank:
                closePanelView();
                break;

        }
    }

    private void gotoPost(int id) {
        switch (id) {
            case R.id.task:
                quicklyClosePanel();
                TaskPostActivity.startActivity(getActivity(), TaskPostActivity.TYPE_TASK);
                break;
            case R.id.project:
                quicklyClosePanel();
                TaskPostActivity.startActivity(getActivity(), TaskPostActivity.TYPE_PROJECT);
                break;
            case R.id.activity:
                quicklyClosePanel();
                TaskPostActivity.startActivity(getActivity(), TaskPostActivity.TYPE_ACTIVITY);
                break;
        }
    }

    private void quicklyClosePanel() {
        mFab.setScaleX(1);
        mFab.setScaleY(1);
        closePanelView();

    }

    private Handler mHandler=new Handler(){
      @Override
      public void handleMessage(Message msg){
          if(msg.what==0){
              switch (mCurrent){
                  case 0:
                      putMarkers(mUTaskManager.getTasksInMap(UTaskManager.RECOMMEND));
                      break;
                  case 1:
                      putMarkers(mUTaskManager.getTasksInMap(UTaskManager.HELP_EACH_OTHER));
                      break;
                  case 2:
                      putMarkers(mUTaskManager.getTasksInMap(UTaskManager.U_ACTIVITY));
                      break;
                  case 3:
                      putMarkers(mUTaskManager.getTasksInMap(UTaskManager.FOLLOW));
                      break;
                  default:
                      break;
              }
          }else if(msg.what==1){
              Toast.makeText(UMainFragment.this.getActivity(),"错误："+msg.obj,Toast.LENGTH_SHORT).show();
          }
      }
    };
    @Override
    public void onCreate(Bundle savedInstanceState){
             super.onCreate(savedInstanceState);
        mLocationManager = TencentLocationManager.getInstance(this.getActivity());

    }
    private class ULocationListener implements TencentLocationListener{
        @Override
        public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
            mLocationManager.removeUpdates(this);
            mCenter=new LatLng(tencentLocation.getLatitude(),
                    tencentLocation.getLongitude());
            mTencentMap.setCenter(mCenter);
            mUTaskManager.setLocation(mCenter.getLatitude()+","+mCenter.getLongitude());
        }

        @Override
        public void onStatusUpdate(String s, int i, String s1) {
            //Toast.makeText(getActivity(),"请检查GPS状态",Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v=inflater.inflate(R.layout.u_main_fragment,parent,false);
        init(v);
        initView(v);
        initAnimation();



        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mLocationManager=TencentLocationManager.getInstance(this.getContext());
        //每15秒定位一次
        mLocationManager.requestLocationUpdates(TencentLocationRequest.create()
                .setInterval(15000)
                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA),new ULocationListener());
        mMapView.onCreate(savedInstanceState);
        //测试代码
        mTencentMap.setCenter(mCenter);
        //测试代码
        mTencentMap.setZoom(18);
        mNavigation=Navigation.getInstance(getContext(),mTencentMap);
        new RefreshMapMarkerThread().start();
    }

    @Override
    public void onResume(){
        mMapView.onResume();
        if(mLocationManager!=null) {
            mLocationManager.resumeLocationUpdates();
        }
        super.onResume();
    }

    @Override
    public void onStop(){
        mMapView.onStop();
        if(mLocationManager!=null) {
            mLocationManager.pauseLocationUpdates();
        }
        super.onStop();
    }
    @Override
    public void onPause(){
        mMapView.onPause();
        if(mLocationManager!=null) {
            mLocationManager.pauseLocationUpdates();
        }
        super.onPause();
    }
    @Override
    public void onDestroy(){
        mMapView.onDestroy();
        if(mLocationManager!=null) {
            mLocationManager.pauseLocationUpdates();
        }
        super.onDestroy();
    }

    public void pauseLocationUpdates() {
        if (mLocationManager != null) {
            mLocationManager.pauseLocationUpdates();
        }
    }

    public void resumeLocationUpdates() {
        if (mLocationManager != null) {
            mLocationManager.resumeLocationUpdates();
        }
    }
    public MapView getMapView(){
        return mMapView;
    }
    private void init(View v){
        mFab=(FloatingActionButton)v.findViewById(R.id.float_button);
        mFab.setAlpha(0.7f);
        mFab.setOnClickListener(this);
        mButtons=new Button[5];
        mButtons[0]=(Button)v.findViewById(R.id.recommended_bt);
        mButtons[1]=(Button)v.findViewById(R.id.help_each_other_bt);
        mButtons[2]=(Button)v.findViewById(R.id.activity_bt);
        mButtons[3]=(Button)v.findViewById(R.id.follow_bt);
        mButtons[4]=(Button)v.findViewById(R.id.navigation_bt);

        mButtons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrent(0);
                onPressRecommendButton();
            }
        });
        mButtons[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrent(1);
                onPressHelpEachOtherButton();
            }
        });
        mButtons[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrent(2);
                onPressActivityButton();
            }
        });
        mButtons[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrent(3);
                onPressFollowButton();
            }
        });
        mButtons[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrent(4);
                onPressNavigationButton();
            }
        });
        setCurrent(0);
        mMapView=(MapView)v.findViewById(R.id.map_view);
        mTencentMap=mMapView.getMap();
        //设置弹出窗口的视图
        mTencentMap.setInfoWindowAdapter(new TencentMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                if(mNavigation!=null && mNavigation.isWorking()){
                    return null;
                }
                View v=View.inflate(UMainFragment.this.getActivity(),R.layout.u_marker_info_window,null);
                @SuppressWarnings("unchecked")
                ArrayList<UTask> tasks=(ArrayList<UTask>)marker.getTag();
                ListView listView=(ListView) v.findViewById(R.id.marker_info_list_view);
                listView.setAdapter(new MarkerListAdapter(getContext(),tasks));
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        UTask task=(UTask)adapterView.getItemAtPosition(i);
                        Intent intent = new Intent(UMainFragment.this.getActivity(),TaskDetailsActivity.class);
                        intent.putExtra("UTask",task);
                        startActivity(intent);
                    }
                });
                final LatLng position=marker.getPosition();
                Button button=(Button)v.findViewById(R.id.marker_info_navigation_bt);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCurrentDestination=position;
                        setCurrent(4);
                        onPressNavigationButton();
                    }
                });
                return v;
            }

            @Override
            public void onInfoWindowDettached(Marker marker, View view) {
                //无需回收view
            }
        });
        mTencentMap.setOnMarkerClickListener(new TencentMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker argo) {
                argo.showInfoWindow();
                for(Marker i:mMarkers)
                {
                    if(!i.equals(argo))
                    {
                       i.hideInfoWindow();
                    }
                }

                return false;
            }
        });
        mTencentMap.setOnMapClickListener(new TencentMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                for(Marker i:mMarkers)
                {
                   i.hideInfoWindow();
                }
            }
        });
    }
    private static class MarkerListAdapter extends ArrayAdapter<UTask>{
        MarkerListAdapter(Context context, List<UTask> tasks){
            super(context,R.layout.marker_list_item,tasks);
        }

        @Override
        @NonNull
        public View getView(int position,View convertView,@NonNull ViewGroup parent){
            UTask task=getItem(position);
            if(convertView==null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.marker_list_item, parent, false);
            }
            View v=convertView;
            if(task!=null) {
                TextView tv=(TextView) v.findViewById(R.id.marker_item_text_title);
                tv.setText(UPublicTool.forShort(task.getTitle(), 5));
                tv=(TextView)v.findViewById(R.id.marker_item_text_price);
                tv.setText(String.format(Locale.ENGLISH,"¥%.2f",task.getPrice()));
            }
            return v;
        }
    }
    public void setCurrent(int p){
        mCurrent=p;
        setButtonsPressed(p);
    }
    private void setButtonsPressed(int i){
        for(int j=0;j<5;++j){
            if(j==i){
                mButtons[j].setBackgroundResource(R.drawable.bt_shape);
            }else{
                mButtons[j].setBackgroundResource(R.drawable.bt_shape_unpressed);
            }
        }
    }
    private void showMarkers(){
        if(mMarkers!=null){
            for(Marker m :mMarkers){
                if(m!=null){
                    m.setVisible(true);
                }
            }
        }
    }
    private void putMarkers(ArrayList<UTask> tasks){
        hideMarkers();
        mMarkers=new ArrayList<>();
        HashMap<LatLng,ArrayList<UTask>> map=new HashMap<>();
        for(UTask task:tasks){
            if(task.getToWhere().length()>0){
                LatLng latLng = task.getPosition();
                if(map.containsKey(latLng)){
                    map.get(latLng).add(task);
                }else{
                    ArrayList<UTask> a=new ArrayList<>();
                    a.add(task);
                    map.put(latLng,a);
                }
            }
        }
        Set<LatLng> set=map.keySet();
        for(LatLng latLng:set){
            Marker marker = mTencentMap.addMarker(new MarkerOptions()
            .position(latLng)
            .draggable(false)
            .icon(BitmapDescriptorFactory.defaultMarker())
            .anchor(0.5f,0.5f)
            .tag(map.get(latLng)));
            mMarkers.add(marker);
        }
        showMarkers();
    }
    private void hideMarkers(){
        if(mMarkers!=null){
            for(Marker m:mMarkers){
                if(m!=null){
                    m.setVisible(false);
                }
            }
        }
    }
    private class RefreshMapMarkerThread extends Thread{
        @Override
        public void run(){
            try {
                mUTaskManager.waitRefreshTasksInMap();
                Message message=new Message();
                message.what=0;
                mHandler.sendMessage(message);
            }catch (UServerAccessException e){
                e.printStackTrace();
                Message message=new Message();
                message.obj=e.getMessage();
                message.what=1;
                mHandler.sendMessage(message);
            }
        }
    }
    private void onPressRecommendButton(){
        mNavigation.endNavigation();
        new RefreshMapMarkerThread().start();
    }
    private void onPressHelpEachOtherButton(){
        mNavigation.endNavigation();
        new RefreshMapMarkerThread().start();
    }
    private void onPressActivityButton(){
        mNavigation.endNavigation();
        new RefreshMapMarkerThread().start();
    }
    private void onPressFollowButton(){
        mNavigation.endNavigation();
        new RefreshMapMarkerThread().start();
    }
    private void onPressNavigationButton(){
        hideMarkers();
        if(mCurrentDestination==null){
            Toast.makeText(getActivity(),"请先选择任务！",Toast.LENGTH_SHORT).show();
            setCurrent(0);
            onPressRecommendButton();
            return;
        }
        mLocationManager.requestLocationUpdates(TencentLocationRequest.create()
                .setInterval(15000)
                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA),
                new ULocationListener(){
                    @Override
                    public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
                        super.onLocationChanged(tencentLocation,i,s);
//                        mNavigation.startNavigation(
//                                new Location((float) mCenter.getLatitude(),(float)mCenter.getLongitude()),
//                                new Location((float)mCurrentDestination.getLatitude(),
//                                        (float)mCurrentDestination.getLongitude())
//                        );
                    }
                });
        mNavigation.startNavigation(
                new Location((float) mCenter.getLatitude(),(float)mCenter.getLongitude()),
                new Location((float)mCurrentDestination.getLatitude(),
                        (float)mCurrentDestination.getLongitude())
        );
    }

    private void hideFab() {
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(mFab, "scaleX", 1, 0)
                .setDuration(100);
        animatorX.setInterpolator(new AccelerateInterpolator());
        ObjectAnimator animatorY = ObjectAnimator.ofFloat(mFab, "scaleY", 1, 0)
                .setDuration(100);
        animatorY.setInterpolator(new AccelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animatorX, animatorY);
        set.start();
    }

    private void showFab() {
        ObjectAnimator animatorX = ObjectAnimator.ofFloat(mFab, "scaleX", 0, 1)
            .setDuration(100);
        animatorX.setInterpolator(new AccelerateInterpolator());
    ObjectAnimator animatorY = ObjectAnimator.ofFloat(mFab, "scaleY", 0, 1)
            .setDuration(100);
        animatorY.setInterpolator(new AccelerateInterpolator());
    AnimatorSet set = new AnimatorSet();
        set.playTogether(animatorX, animatorY);
        set.start();
}
}
