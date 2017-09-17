package ecnu.uleda.view_controller.task.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.tencent.mapsdk.raster.model.BitmapDescriptorFactory;
import com.tencent.mapsdk.raster.model.LatLng;
import com.tencent.mapsdk.raster.model.Marker;
import com.tencent.mapsdk.raster.model.MarkerOptions;
import com.tencent.tencentmap.mapsdk.map.MapView;
import com.tencent.tencentmap.mapsdk.map.TencentMap;


import net.phalapi.sdk.PhalApiClientResponse;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ecnu.uleda.R;
import ecnu.uleda.function_module.UTaskManager;
import ecnu.uleda.model.UserInfo;
import ecnu.uleda.tool.RecyclerViewTouchListener;
import ecnu.uleda.tool.UPublicTool;
import ecnu.uleda.exception.UServerAccessException;
import ecnu.uleda.model.UTask;
import ecnu.uleda.function_module.UserOperatorController;
import ecnu.uleda.function_module.ServerAccessApi;
import ecnu.uleda.view_controller.SingleUserInfoActivity;
import ecnu.uleda.view_controller.TaskEditActivity;
import ecnu.uleda.view_controller.task.adapter.TakersAdapter;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.xiaopan.sketch.SketchImageView;
import me.xiaopan.sketch.request.DisplayOptions;
import me.xiaopan.sketch.shaper.CircleImageShaper;

public class TaskDetailsActivity extends BaseDetailsActivity {

    public static final String EXTRA_UTASK = "UTask";
    private static final int MSG_REFRESH_SUCCESS = 0;
    private static final int MSG_REFRESH_FAIL = 1;
    private static final int MSG_COMMENT_GET = 2;
    private static final int MSG_TAKE_SUCCESS = 3;
    private static final int MSG_COMMENT_SUCCESS = 4;
    private static final int MSG_COMMENT_FAILED = 5;
    private static final int MSG_TAKERS_GET = 6;
    private static final int MSG_TASK_SUCCESS = 7;
    private static final int MSG_CANCEL_TAKE = 8;
    private static final int MENU_ITEM_DELETE = 100;
    public static final String TAG = "TaskDetailsActivity";
    private UTask mTask;
    private TencentMap mTencentMap;
    private CompositeDisposable mDisposables = new CompositeDisposable();

    @BindView(R.id.head_line_layout)
    Toolbar mToolbar;
    @BindView(R.id.task_tool_title)
    TextView mHeadlineLayout;
    @BindView(R.id.task_map_view)
    MapView mMapView;
    @BindView(R.id.comment_bt)
    Button mButtonLeft;
    @BindView(R.id.right_button)
    Button mButtonRight;
    @BindView(R.id.task_title)
    TextView mTaskTitle;
    @BindView(R.id.task_location)
    TextView mTaskLocation;
    @BindView(R.id.task_details_reward)
    TextView mTaskReward;
    @BindView(R.id.task_detail_publisher_name)
    TextView mTaskPublisher;
    @BindView(R.id.task_detail_circle_image)
    SketchImageView mTaskAvatar;
    @BindView(R.id.task_detail_info)
    TextView mTaskDetailInfo;
    @BindView(R.id.task_detail_state)
    TextView mTaskTimeLimit;
    @BindView(R.id.task_detail_stars)
    TextView mTaskPublisherStars;
    @BindView(R.id.task_takers_list)
    RecyclerView mTaskTakersList;
    @BindView(R.id.task_detail_list_view)
    LinearLayout mDetailContainer;
    @BindView(R.id.details_scroll)
    ScrollView mScrollView;
    @BindView(R.id.task_takers_none)
    TextView mTakersNone;

    private ProgressDialog mProgress;

    private ExecutorService mThreadPool;
    private LayoutInflater mInflater;
    private List<UserInfo> mTakers = new ArrayList<>();
    private TakersAdapter mTakersAdapter;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_REFRESH_SUCCESS) {
                mTask = (UTask) msg.obj;
                listViewInit();
                mapInit();
                final UserOperatorController uoc = UserOperatorController.getInstance();
                if (mTask.getAuthorID() == Integer.parseInt(uoc.getId())) {
                    mButtonRight.setText("编辑任务");
                    mButtonRight.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(TaskDetailsActivity.this, TaskEditActivity.class);
                            intent.putExtra("Task", mTask);
                            startActivityForResult(intent, 1);
                        }
                    });
                } else if (mTask.getStatus() != 0) {
                    mButtonRight.setEnabled(false);
                    mButtonRight.setBackgroundColor(ContextCompat.getColor(TaskDetailsActivity.this, android.R.color.darker_gray));
                    switch (mTask.getStatus()) {
                        case 4:
                            mButtonRight.setText("已失效");
                            break;
                        default:
                            mButtonRight.setText("已被领取");
                            break;
                    }
                } else {
                    mButtonRight.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            acceptTask(uoc);
                        }
                    });
                }
                initTakers(uoc);
            } else if (msg.what == MSG_TAKE_SUCCESS) {
                Toast.makeText(TaskDetailsActivity.this, "成功接受任务", Toast.LENGTH_SHORT).show();
                mTask.setStatus(1);
                mButtonRight.setText("取消抢单");
                mButtonRight.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Message msg = new Message();
                        msg.what = MSG_CANCEL_TAKE;
                        // TODO 取消接单
                        mButtonRight.setText("抢单");
                        mTask.setStatus(0);
                    }
                });


            } else if (msg.what == MSG_COMMENT_GET) {
                addCommentView(mDetailContainer, 2);
            } else if (msg.what == MSG_COMMENT_SUCCESS) {
                addCommentView((String) msg.obj, mDetailContainer, 2);
            } else if (msg.what == MSG_COMMENT_FAILED) {
                mProgress.dismiss();
                Toast.makeText(TaskDetailsActivity.this, "评论失败", Toast.LENGTH_SHORT).show();
            } else if (msg.what == MSG_TAKERS_GET) {
                initTakersView();
            } else {
                Toast.makeText(TaskDetailsActivity.this, "错误：" + msg.obj, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void acceptTask(final UserOperatorController uoc) {
        Observable<String> observable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                e.onNext(ServerAccessApi.acceptTask(uoc.getId(), uoc.getPassport(), mTask.getPostID()));
                e.onComplete();
            }
        });
        Observer<String> observer = new Observer<String>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                mDisposables.add(d);
            }

            @Override
            public void onNext(@NonNull String s) {
                if ("success".equals(s)) {
                    Message msg = new Message();
                    msg.what = MSG_TAKE_SUCCESS;
                    mHandler.sendMessage(msg);
                } else {
                    Toast.makeText(TaskDetailsActivity.this, "接单失败：" + s, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                e.printStackTrace();
                Toast.makeText(TaskDetailsActivity.this, "接单失败：网络错误", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {

            }
        };
        observable.observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(observer);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent == null) return;
        if (resultCode == 1) {
            Message msg = new Message();
            msg.obj = intent.getSerializableExtra("Task");
            msg.what = 0;
            mHandler.sendMessage(msg);
        } else if (resultCode == 2) {
            finish();
        }
    }

    @OnClick(R.id.comment_bt)
    void comment() {
        showCommentPopup();
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
    }


    public View getChatView(UserChatItem userChatItem) {
        View v;
        if (mTask.getAuthorUserName().equals(userChatItem.name)) {
            v = mInflater.inflate(R.layout.task_detail_chat_item_right, mDetailContainer, false);
        } else {
            v = mInflater.inflate(R.layout.task_detail_chat_item_left, mDetailContainer, false);
        }
        SketchImageView avatar = (SketchImageView) v.findViewById(R.id.task_detail_chat_item_circle);
        DisplayOptions options = new DisplayOptions().setImageShaper(new CircleImageShaper());
        avatar.setOptions(options);
        // for testing
        if (userChatItem.authorAvatar.equals("test")) {
            avatar.displayResourceImage(R.drawable.model1);
        } else {
            avatar.displayImage(userChatItem.authorAvatar);
        }
        TextView tv = (TextView) v.findViewById(R.id.say_what);
        tv.setText(userChatItem.sayWhat);
        tv = (TextView) v.findViewById(R.id.time_before);
        tv.setText(UPublicTool.timeBefore(userChatItem.postDate));
        tv = (TextView) v.findViewById(R.id.name_of_chatter);
        tv.setText(userChatItem.name);
        return v;
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onStop() {
        mMapView.onStop();
        mThreadPool.shutdownNow();
        super.onStop();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        mDisposables.clear();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mTask.getAuthorID() == Integer.parseInt(UserOperatorController.getInstance().getId())) {
            MenuItem menuItem = menu.add(0, MENU_ITEM_DELETE, 100, "删除");
            menuItem.setIcon(R.drawable.ic_delete);
            menuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void initTakers(final UserOperatorController uoc) {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
                Thread.sleep(500);
                e.onNext(ServerAccessApi.getTakers(uoc.getId(),
                        uoc.getPassport(), mTask.getPostID()));
            }
        })
                .map(new Function<String, JSONArray>() {
                    @Override
                    public JSONArray apply(@NonNull String s) throws Exception {
                        return new JSONArray(s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<JSONArray>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposables.add(d);
                    }

                    @Override
                    public void onNext(@NonNull JSONArray data) {
                        try {
                            mTakers.clear();
                            int length = data.length();
                            for (int i = 0; i < length; i++) {
                                JSONObject person = data.getJSONObject(i);
                                JSONObject personDetail = person.getJSONObject("taker_details");
                                UserInfo info = new UserInfo();
                                if (uoc.getId().equals(person.getString("taker_id"))) {
                                    mButtonRight.setText("取消抢单");
                                }
                                info.setAvatar(UPublicTool.BASE_URL_AVATAR + personDetail.getString("avatar"))
                                        .setId(person.getString("taker_id"))
                                        .setUserName(personDetail.getString("username"))
                                        .setSex(personDetail.getInt("sex"))
                                        .setBirthday(personDetail.getString("birthday"))
                                        .setStudentId(personDetail.getString("studentid"))
                                        .setRealName(personDetail.getString("realname"))
                                        .setPhone(personDetail.getString("phone"))
                                        .setSignature(personDetail.getString("signature"));
                                mTakers.add(info);
                            }
                            mHandler.sendEmptyMessage(MSG_TAKERS_GET);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(TaskDetailsActivity.this, "获取接单人列表失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void initTakersView() {
        if (mTakers.size() > 0) {
            mTakersAdapter.setDatas(mTakers);
        } else {
            mTaskTakersList.setVisibility(View.GONE);
            mTakersNone.setVisibility(View.VISIBLE);
        }
//        mTaskTakersList.smoothScrollToPosition(0);
//        mTaskTakersList.requestFocus();
    }

    private void initComments(final UserOperatorController uoc) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = ServerAccessApi.getComment(uoc.getId(),
                            uoc.getPassport(), mTask.getPostID(), String.valueOf(0));
                    if (!response.equals("null")) {
                        setChatItems((List<UserChatItem>) new Gson().fromJson(response,
                                new TypeToken<List<UserChatItem>>() {
                                }.getType()));
                    } else {
                        getMUserChatItems().clear();
                    }
                    mHandler.sendEmptyMessage(MSG_COMMENT_GET);
                } catch (UServerAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initDetails(final UserOperatorController uoc) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject j = ServerAccessApi.getTaskPost(uoc.getId(), uoc.getPassport(), mTask.getPostID());
                    UTask task = new UTask()
                            .setPath(j.getString("path"))
                            .setTitle(j.getString("title"))
                            .setTag(j.getString("tag"))
                            .setPostDate(j.getLong("postdate"))
                            .setPrice(new BigDecimal(j.getString("price")))
                            .setAuthorID(j.getInt("author"))
                            .setDescription(j.getString("description"))
                            .setAuthorUserName(j.getString("authorUsername"))
                            .setAuthorCredit(j.getInt("authorCredit"))
                            .setPostID(mTask.getPostID())
                            .setActiveTime(j.getLong("activetime"))
                            .setStatus(j.getInt("status"));
                    String[] ps = j.getString("position").split(",");
                    task.setPosition(
                            new LatLng(Double.parseDouble(ps[0]), Double.parseDouble(ps[1]))
                    );
                    Message message = new Message();
                    message.what = MSG_REFRESH_SUCCESS;
                    message.obj = task;
                    mHandler.sendMessage(message);
                } catch (UServerAccessException e) {
                    e.printStackTrace();
                    Message message = new Message();
                    message.obj = e.getMessage();
                    message.what = MSG_REFRESH_FAIL;
                    mHandler.sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }


    public void listViewInit() {
//        View fv = View.inflate(this.getApplicationContext(), R.layout.task_detail_chat_list_footer_view, mListView);
        String description = mTask.getDescription();
        if (description == null) {
            mTaskDetailInfo.setText("加载中...");
        } else if ("".equals(description)) {
            mTaskDetailInfo.setText("该用户什么都木有填写~");
        } else {
            mTaskDetailInfo.setText(description);
        }
        mTaskReward.setText(String.format(Locale.ENGLISH, "¥%.2f", mTask.getPrice()));
        mTaskTitle.setText(mTask.getTitle());
        DisplayOptions options = new DisplayOptions();
        options.setImageShaper(new CircleImageShaper());
        mTaskAvatar.setOptions(options);
        if (mTask.getAvatar() == null) {
            mTaskAvatar.displayResourceImage(R.drawable.ic_person_grey600_48dp);
        } else {
            mTaskAvatar.displayImage(mTask.getAvatar());
        }
        mTaskAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(TaskDetailsActivity.this, SingleUserInfoActivity.class);
                intent.putExtra("userid", String.valueOf(mTask.getAuthorID()));
                startActivity(intent);
            }
        });
        mTaskPublisher.setText(mTask.getAuthorUserName());
        mTaskPublisherStars.setText(mTask.getStarString());
        if (mTask.getPosition() != null) {
            Point size = UPublicTool.getScreenSize(this.getApplicationContext(), 0.03, 0.03);
            SpannableStringBuilder str = UPublicTool.addICONtoString(this.getApplicationContext(), "#LO" + mTask.getToWhere(), "#LO", R.drawable.location, size.x, size.y);
            mTaskLocation.setText(str);
        }
        if (mTask.getStatus() == 0) {
            Date date = new Date((mTask.getPostDate() + mTask.getActiveTime()) * 1000);
            mTaskTimeLimit.setText("剩余时间" + UPublicTool.timeLeft(date));
        }
//        mTaskTakersList.setVisibility(View.GONE);
    }


//    private int getListScrollY() {//获取滚动距离
//        View c = mListView.getChildAt(0);
//        if (c == null) {
//            return 0;
//        }
//
//        int firstVisiblePosition = mListView.getFirstVisiblePosition();
//        int top = c.getTop();
//
//        int headerHeight = 0;
//        if (firstVisiblePosition >= 1) {
//            headerHeight = mListView.getHeight();
//        }
//        return -top + firstVisiblePosition * c.getHeight() + headerHeight;
//    }


    public void init() {
        mHeadlineLayout.setText(mTask.getTitle());
        mInflater = LayoutInflater.from(this);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("发布中...");
        mProgress.setIndeterminate(true);
        mProgress.setCanceledOnTouchOutside(false);
        mTakersAdapter = new TakersAdapter(this, mTakers);
        mTaskTakersList.setAdapter(mTakersAdapter);
        mTaskTakersList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false));
        mTaskTakersList.addOnItemTouchListener(new RecyclerViewTouchListener(mTaskTakersList) {
            @Override
            public void onItemClick(final int position, RecyclerView.ViewHolder viewHolder) {
                if (mTask.getAuthorID() != Integer.valueOf(UserOperatorController.getInstance().getId())
                        || mTakersAdapter.isVerifiedTaker()) return;
                new AlertDialog.Builder(TaskDetailsActivity.this)
                        .setMessage("是否选择" + mTakers.get(position).getUserName() + "作为接单人？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                verifyTaker(position);
                            }
                        })
                        .create()
                        .show();
            }

            @Override
            public void onItemLongClick(int position, RecyclerView.ViewHolder viewHolder) {

            }
        });
    }

    private void verifyTaker(final int position) {
        Observable.create(new ObservableOnSubscribe<PhalApiClientResponse>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<PhalApiClientResponse> e) throws Exception {
                e.onNext(UTaskManager.getInstance()
                        .verifyTaker(mTask.getPostID(), mTakers.get(position).getId()));
                e.onComplete();
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<PhalApiClientResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposables.add(d);
                    }

                    @Override
                    public void onNext(@NonNull PhalApiClientResponse s) {
                        Log.e(TAG, "verify taker: " + s.getData());
                        if (s.getRet() == 200) {
                            String data = s.getData();
                            if (data.equals("success")) {
                                Toast.makeText(TaskDetailsActivity.this, "选择接单人成功", Toast.LENGTH_SHORT).show();
                                takerChosen(position);
                            } else {
                                Toast.makeText(TaskDetailsActivity.this, "选择接单人失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(TaskDetailsActivity.this, "选择接单人失败: " + s.getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(TaskDetailsActivity.this, "网络异常，选择接单人失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void takerChosen(int position) {
        int oldSize = mTakers.size();
        List<UserInfo> deletingUsers = new ArrayList<>();
        for (int i = 0; i < mTakers.size(); i++) {
            if (i != position) deletingUsers.add(mTakers.get(i));
        }
        if (mTakers.removeAll(deletingUsers)) {
            if (position != 0) mTakersAdapter.notifyItemRangeRemoved(0, position);
            if (position != oldSize - 1)
                mTakersAdapter.notifyItemRangeRemoved(1, oldSize - position - 1);
        }
        mTakersAdapter.setVerifiedTaker(true);
    }

    public void mapInit() {
        mTencentMap.setZoom(18);
        mTencentMap.setCenter(mTask.getPosition());
        Marker marker = mTencentMap.addMarker(new MarkerOptions()
                .position(mTask.getPosition())
                .icon(BitmapDescriptorFactory.defaultMarker()).draggable(false)
        );
        marker.setTitle(mTask.getToWhere());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == MENU_ITEM_DELETE) {
            cancelTask();
        }
        return super.onOptionsItemSelected(item);
    }

    private void cancelTask() {
        Observable.create(new ObservableOnSubscribe<PhalApiClientResponse>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<PhalApiClientResponse> e) throws Exception {
                e.onNext(UTaskManager.getInstance().cancelTask(mTask.getPostID()));
                e.onComplete();
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<PhalApiClientResponse>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposables.add(d);
                    }

                    @Override
                    public void onNext(@NonNull PhalApiClientResponse phalApiClientResponse) {
                        if (phalApiClientResponse.getRet() == 200) {
                            String data = phalApiClientResponse.getData();
                            if ("success".equals(data)) {
                                Toast.makeText(TaskDetailsActivity.this, "取消成功", Toast.LENGTH_SHORT).show();
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        finish();
                                    }
                                }, 500);
                            } else {
                                Toast.makeText(TaskDetailsActivity.this, "取消失败：" + phalApiClientResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(TaskDetailsActivity.this, "取消失败：" + phalApiClientResponse.getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(TaskDetailsActivity.this, "取消失败：网络异常", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    public void initActivity(@Nullable Bundle savedInstanceState) {
        setSupportActionBar(mToolbar);
        setTitle("");
        Intent intent = getIntent();
        mTask = (UTask) intent.getSerializableExtra(EXTRA_UTASK);
        init();
        final UserOperatorController uoc = UserOperatorController.getInstance();
        mTencentMap = mMapView.getMap();
        mMapView.onCreate(savedInstanceState);

        if (!uoc.getIsLogined()) return;
        mThreadPool = Executors.newCachedThreadPool();
        initDetails(uoc);
        initComments(uoc);
        listViewInit();
    }

    @Override
    public void initContentView() {
        setContentView(R.layout.activity_task_details);
    }

    @Override
    public void onSubmitComment(@NotNull final String comment) {
        mProgress.show();
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                UserOperatorController uoc = UserOperatorController.getInstance();
                try {
                    String result = ServerAccessApi.postComment(uoc.getId(), uoc.getPassport(),
                            mTask.getPostID(), comment);
                    if (result.equals("success")) {
                        Message msg = Message.obtain();
                        msg.obj = comment;
                        msg.what = MSG_COMMENT_SUCCESS;
                        mHandler.sendMessage(msg);
                    } else {
                        mHandler.sendEmptyMessage(MSG_COMMENT_FAILED);
                    }
                } catch (UServerAccessException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_COMMENT_FAILED);
                }
            }
        });
    }

    public static class UserChatItem {
        @SerializedName("author")
        public int authorId;

        @SerializedName("body")
        public String sayWhat;

        @SerializedName("authorUsername")
        public String name = "";

        public String authorAvatar;
        public int commentID;
        public long postDate;

        public UserChatItem(int authorId, String sayWhat, String name, String authorAvatar,
                            int commentID, long postDate) {
            this.authorId = authorId;
            this.sayWhat = sayWhat;
            this.name = name;
            this.authorAvatar = authorAvatar;
            this.commentID = commentID;
            this.postDate = postDate;
        }
    }

    public static void startActivityFromMyTask(Context context, int position, int flag) {
        UTask uTask = new UTask();
        int index = 0;
        int temp = 0;
        if(position == 0) {
            index = 0;
        } else {
            if(position % 10 == 0) {
                index = position / 10 - 1;
            } else {
                index = position / 10;
            }
        }
        temp = position - 10 * index;
        startFromUTask(context, index, temp, flag);
    }

    private static void startFromUTask(final Context context, final int index, final int temp, final int flag) {
        Observable.create(new ObservableOnSubscribe<JSONArray>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<JSONArray> e) throws Exception {
                e.onNext(ServerAccessApi.getUserTask(index, flag));
                e.onComplete();
            }
        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<JSONArray>() {
                    @Override
                    public void accept(JSONArray jsonArray) throws Exception {
                        JSONObject json = jsonArray.getJSONObject(temp);
                        UTask uTask = new UTask();
                        uTask.setTitle(json.getString("title"))
                                .setStatus(Integer.parseInt(json.getString("status")))
                                .setAuthorID(Integer.parseInt(json.getString("author")))
                                .setAuthorAvatar(json.getString("authorAvatar"))
                                .setAuthorUserName(json.getString("authorUsername"))
                                .setAuthorCredit(Integer.parseInt(json.getString("authorCredit")))
                                .setTag(json.getString("tag"))
                                .setDescription(json.getString("description"))
                                .setPostDate(Long.parseLong(json.getString("postdate")))
                                .setActiveTime(Long.parseLong(json.getString("activetime")))
                                .setPath(json.getString("path"))
                                .setPrice(BigDecimal.valueOf(Double.parseDouble(json.getString("price"))))
                                .setPostID(json.getString("postID"))
                                .setTakersCount(Integer.parseInt(json.getString("taker")));
                        Intent intent = new Intent(context, TaskDetailsActivity.class);
                        intent.putExtra("UTask", uTask);
                        context.startActivity(intent);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

}
