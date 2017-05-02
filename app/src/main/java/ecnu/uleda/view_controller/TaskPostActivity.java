package ecnu.uleda.view_controller;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import ecnu.uleda.R;
import ecnu.uleda.tool.UPublicTool;
import ecnu.uleda.exception.UServerAccessException;
import ecnu.uleda.function_module.UserOperatorController;
import ecnu.uleda.function_module.ServerAccessApi;
import io.rong.imageloader.utils.L;

import static android.R.attr.type;

public class TaskPostActivity extends AppCompatActivity {

    private static final String EXTRA_POST_TYPE = "extra_post_type";
    public static final int TYPE_TASK = 1;
    public static final int TYPE_PROJECT = 2;
    public static final int TYPE_ACTIVITY = 3;

    private UserOperatorController mUserOperatorController;

    private Handler mClickHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                Toast.makeText(TaskPostActivity.this, "提交成功～", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                UServerAccessException exception = (UServerAccessException) msg.obj;
                Toast.makeText(TaskPostActivity.this, "提交任务失败：" + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    final private static ArrayList<String> taskPostArray;

    static {
        taskPostArray = new ArrayList<>();
        taskPostArray.add("跑腿代步");
        taskPostArray.add("生活服务");
        taskPostArray.add("学习帮助");
        taskPostArray.add("技术难题");
        taskPostArray.add("寻物启示");
        taskPostArray.add("活动相关");
        taskPostArray.add("运动锻炼");
        taskPostArray.add("项目招人");
        taskPostArray.add("招聘实习");
        taskPostArray.add("其他");
    }

    private TextView mButtonBack;
    private Button mButtonTaskPost;
    private ArrayAdapter<String> taskPostAdapter;
    private EditText mEtTitle;
    private Spinner mSpinTag;
    private EditText mEtPrice;
    private EditText mEtActiveTime;
    private EditText mEtDescription;
    private TextView mTvMainTitle;
    private TextView mTvMajorName;
    private EditText mEtProDescription;
    private TextView mTvLocationName;
    private TextView mTvDetailName;
    private EditText mEtDetail;
    private TextView mTvTitleName;

    private LinearLayout mLlSponsor;
    private LinearLayout mLlTitle;
    private LinearLayout mLlMajor;
    private LinearLayout mLlActivityTime;
    private LinearLayout mLlLocation;
    private LinearLayout mLlCategory;
    private LinearLayout mLlFee;
    private LinearLayout mLlActiveTime;
    private LinearLayout mLlStart;
    private LinearLayout mLlDestination;
    private LinearLayout mLlDetails;
    private LinearLayout mLlAddPhoto;
    private LinearLayout mLlProDescription;



    private String mId;
    private String mPpassport;
    private String mTitle;
    private String mTag = "跑腿代步";  //tag任务分类
    private String mDescription;
    private String mPrice;
    private String mPath;
    private String mActiveTime;
    private String mPosition;
    private String mStart;
    private String mDestination;

    private Button buttonStart;
    private Button buttonDestination;
    private Button buttonDeleteStart;
    private Button buttonDeleteDestination;
    private float latitude = 0;
    private float longitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_post_activity);
        init();
        SpinnerInit();
        SpinnerEvent();

        Intent intent=getIntent();
        int data=intent.getIntExtra(EXTRA_POST_TYPE,1);
        switch (data){
            case TaskPostActivity.TYPE_TASK:{
                mTvMainTitle.setText("发布任务");
                mLlProDescription.setVisibility(View.GONE);
                mLlSponsor.setVisibility(View.GONE);
                mLlMajor.setVisibility(View.GONE);
                mLlActivityTime.setVisibility(View.GONE);
                mLlLocation.setVisibility(View.GONE);
                mLlCategory.setVisibility(View.GONE);
                break;
            }
            case TaskPostActivity.TYPE_PROJECT:{
                mTvTitleName.setText("主题");
                mTvMainTitle.setText("项目招人");
                mTvLocationName.setText("实验室地点");
                mTvDetailName.setText("招人要求");
                mEtDetail.setHint("您对招募同学能力、人数等方面的要求，限225字节内。");
                mLlSponsor.setVisibility(View.GONE);
                mLlFee.setVisibility(View.GONE);
                mLlStart.setVisibility(View.GONE);
                mLlDestination.setVisibility(View.GONE);
                mLlActivityTime.setVisibility(View.GONE);
                mLlActiveTime.setVisibility(View.GONE);
                mLlCategory.setVisibility(View.GONE);
                break;
            }
            case TaskPostActivity.TYPE_ACTIVITY:{
                mLlProDescription.setVisibility(View.GONE);
                mTvTitleName.setText("活动名称");
                mTvMainTitle.setText("活动宣传");
                mLlMajor.setVisibility(View.GONE);
                mLlFee.setVisibility(View.GONE);
                mLlActiveTime.setVisibility(View.GONE);
                mLlStart.setVisibility(View.GONE);
                mLlDestination.setVisibility(View.GONE);
                mEtDetail.setHint("简述活动具体内容，限225字节内。");
                break;

            }
        }

        mButtonBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        mEtPrice.addTextChangedListener(new MyTextWatcher());

        mButtonTaskPost.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getTaskPost();
                if (!judgeEditText()) {
                    return;
                }
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            ServerAccessApi.postTask(mId, mPpassport, mTitle, mTag, mDescription, mPrice, mPath, mActiveTime, mPosition);
                            Message message = new Message();
                            message.what = 0;
                            mClickHandler.sendMessage(message);
                        } catch (UServerAccessException e) {
                            e.printStackTrace();
                            Message message = new Message();
                            message.what = 1;
                            message.obj = e;
                            mClickHandler.sendMessage(message);
                        }
                    }
                }.start();
            }
        });
    }
    @Override
    public void onActivityResult(int request,int result,Intent data){
        if(data==null)return;
        if(request==100){
            buttonStart.setText(data.getStringExtra("title"));
        }else if(request==200){
            buttonDestination.setText(data.getStringExtra("title"));
            latitude=data.getFloatExtra("lat",0f);
            longitude=data.getFloatExtra("lng",0f);
        }
    }
    protected void init() {
        mButtonBack = (TextView) findViewById(R.id.button_task_post_back);
        mButtonTaskPost = (Button) findViewById(R.id.button_task_post);
        mEtTitle = (EditText) findViewById(R.id.task_post_title);
        mEtPrice = (EditText) findViewById(R.id.task_post_payment);
        mEtActiveTime = (EditText) findViewById(R.id.task_post_activeTime);
        mEtDescription = (EditText) findViewById(R.id.task_post_description);
        mTvMainTitle=(TextView) findViewById(R.id.task_post_main_title);
        mTvMajorName=(TextView)findViewById(R.id.task_post_major_name);
        mEtProDescription=(EditText) findViewById(R.id.task_post_project_description);
        mTvLocationName=(TextView)findViewById(R.id.task_post_activity_location_name);
        mTvDetailName=(TextView)findViewById(R.id.task_post_details_name);
        mEtDetail=(EditText)findViewById(R.id.task_post_description);
        mTvTitleName=(TextView)findViewById(R.id.task_post_title_name);


        mLlSponsor=(LinearLayout)findViewById(R.id.task_post_activity_sponsor_option);
        mLlTitle=(LinearLayout)findViewById(R.id.task_post_title_option);
        mLlMajor=(LinearLayout)findViewById(R.id.task_post_major_option);
        mLlActivityTime=(LinearLayout)findViewById(R.id.task_post_activity_time_option);
        mLlLocation=(LinearLayout)findViewById(R.id.task_post_activity_location_option);
        mLlCategory=(LinearLayout)findViewById(R.id.task_post_category_option);
        mLlFee=(LinearLayout)findViewById(R.id.task_post_fee_option);
        mLlActiveTime=(LinearLayout)findViewById(R.id.task_post_active_time_option);
        mLlStart=(LinearLayout)findViewById(R.id.task_post_start_option);
        mLlDestination=(LinearLayout)findViewById(R.id.task_post_destination_option);
        mLlDetails=(LinearLayout)findViewById(R.id.task_post_details_option);
        mLlAddPhoto=(LinearLayout)findViewById(R.id.task_post_add_photo_option);
        mLlProDescription=(LinearLayout)findViewById( R.id.task_post_project_details_option);


        LinearLayout a=(LinearLayout) findViewById(R.id.task_post_padding);
        ViewGroup.LayoutParams lp = a.getLayoutParams();
        lp.height = UPublicTool.getStatusBarHeight(this);


        buttonStart = (Button) findViewById(R.id.button_task_post_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent=new Intent(TaskPostActivity.this,LocationListActivity.class);
                startActivityForResult(intent,100);
            }
        });
        buttonDestination=(Button)findViewById(R.id.button_task_post_destination);
        buttonDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(TaskPostActivity.this,LocationListActivity.class);
                startActivityForResult(intent,200);
            }
        });
        buttonDeleteStart=(Button)findViewById(R.id.button_delete_start);
        buttonDeleteStart.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                buttonStart.setText("选择地址");
            }
        });
        buttonDeleteDestination=(Button)findViewById(R.id.button_delete_destination);
        buttonDeleteDestination.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                buttonDestination.setText("选择地址");
            }
        });

    }


    private void SpinnerInit() {
        mSpinTag = (Spinner) findViewById(R.id.spinner_task_post);
        taskPostAdapter = new ArrayAdapter<>(this.getApplicationContext(),
                R.layout.task_post_spinner, taskPostArray);
        taskPostAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        mSpinTag.setAdapter(taskPostAdapter);
    }

    private void SpinnerEvent() {
        mSpinTag.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mTag = mSpinTag.getSelectedItem().toString();
                adapterView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }


    private class MyTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            String price = mEtPrice.getText().toString();
            int posDot = price.indexOf('.');
            if (posDot>0 &&price.length() - posDot - 1 > 2) {
                String string;
                string = price.substring(0, price.length() - 1);
                mEtPrice.setText(string);
                mEtPrice.setSelection(string.length());
            }
        }

        public void afterTextChanged(Editable s) {
        }
    }

    private void getTaskPost() {
        mUserOperatorController = UserOperatorController.getInstance();
        mId = mUserOperatorController.getId();
        mPpassport = mUserOperatorController.getPassport();
        mTitle = mEtTitle.getText().toString();
        mDescription = mEtDescription.getText().toString();
        mPrice = mEtPrice.getText().toString();
        mStart = buttonStart.getText().toString();
        mDestination = buttonDestination.getText().toString();
        mPath = mStart+"|"+mDestination;
        String time=mEtActiveTime.getText().toString();
        if(time.length()==0)time="0";
        mActiveTime = String.valueOf(Integer.parseInt(time)*60);
        mPosition = latitude+","+longitude;
    }


    private boolean judgeEditText()
    {
        if(mTitle.length()==0)
        {
            Toast.makeText(TaskPostActivity.this, "标题不能为空哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(UPublicTool.byteCount(mTitle)<5)
        {
            Toast.makeText(TaskPostActivity.this, "标题不能少于5个字节哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(UPublicTool.byteCount(mTitle)>30){
            Toast.makeText(TaskPostActivity.this, "标题不能多于30个字节哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(mPrice.length()==0)
        {
            Toast.makeText(TaskPostActivity.this, "价格不能为空哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(UPublicTool.byteCount(mDescription)>225){
            Toast.makeText(TaskPostActivity.this, "描述不能多于225个字节哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(Float.parseFloat(mPrice)<0.5f)
        {
            Toast.makeText(TaskPostActivity.this, "价格不能低于0.5元哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(mActiveTime.length()==0)
        {
            Toast.makeText(TaskPostActivity.this, "时效不能为空哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(!mStart.equals("选择地址") && mDestination.equals("选择地址") )
        {
            Toast.makeText(TaskPostActivity.this,"请选择目的地哦～",Toast.LENGTH_SHORT).show();
            return false;
        }
        if(mStart.equals("选择地址") && !mDestination.equals("选择地址"))
        {
            mPath = ""+"|"+mDestination;
            return true;
        }
        if(mStart.equals("选择地址") && mDestination.equals("选择地址"))
        {
            mPosition="31.2267104411"+","+"121.4044582732";
            mPath = "|";
            return true;
        }

        return true;

    }

    public static void startActivity(Context context, int type) {
        Intent intent = new Intent(context, TaskPostActivity.class);
        intent.putExtra(EXTRA_POST_TYPE, type);
        context.startActivity(intent);
    }

}


