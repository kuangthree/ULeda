package ecnu.uleda.function_module;

import android.support.annotation.NonNull;
import android.util.Log;

import net.phalapi.sdk.PhalApiClient;
import net.phalapi.sdk.PhalApiClientResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import ecnu.uleda.BuildConfig;
import ecnu.uleda.exception.UServerAccessException;
import ecnu.uleda.tool.AESUtils;
import ecnu.uleda.tool.MD5Utils;
import ecnu.uleda.tool.UPublicTool;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ServerAccessApi {
    public static final String BASE_URL = "http://118.89.156.167/mobile/";
    private static final int SET_TIME_OUT = 9999;
    public static final int USER_TASK_FLAG_RELEASED = 0;
    public static final int USER_TASK_FLAG_DOING = 1;
    public static final int USER_TASK_FLAG_TO_EVAL = 2;
    public static final int USER_TASK_FLAG_DONE = 3;
    public static final int USER_TASK_FLAG_MY = 4;

    public static String getMainKey()throws UServerAccessException {
        PhalApiClient client=createClient();
        PhalApiClientResponse response=client
                .withService("Default.GetMainKey")
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try{
                JSONObject data=new JSONObject(response.getData());
                return data.getString("mainKey");
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }
        throw new UServerAccessException(UServerAccessException.INTERNET_ERROR);
    }

    public static int Register(@NonNull String username,@NonNull String password,@NonNull String pcode,
    @NonNull String phone)throws UServerAccessException
    {
        String aes_key = MD5Utils.MD5(getMainKey()).substring(0,16);
        String aes = AESUtils.encrypt(password,aes_key);
        String md5 = MD5Utils.MD5(aes);
        username = UrlEncode(username);
        md5 = UrlEncode(md5);
        pcode = UrlEncode(pcode);
        phone = UrlEncode(phone);
        PhalApiClient client=createClient();
        PhalApiClientResponse response = client
            .withService("User.Register")
            .withParams("username",username)
            .withParams("password",md5)
            .withParams("pcode",pcode)
            .withParams("phone",phone)
            .withTimeout(SET_TIME_OUT)
            .request();
        if(response.getRet() == 200)
        {
           return 200;
        }
        else
        {
            throw new UServerAccessException(response.getRet());
        }
    }
    public static int ReleasedUcircle(@NonNull String title,@NonNull String content,File pic1,File pic2,
                                      File pic3)throws UServerAccessException
    {
        UserOperatorController user = UserOperatorController.getInstance();
        String id = user.getId();
        String passport = user.getPassport();
        UPictureUploader client = UPictureUploader.create("http://118.89.156.167/mobile/");
        if(pic1!=null)client.withFiles("pic1",pic1);
        if(pic2!=null)client.withFiles("pic2",pic2);
        if(pic3!=null)client.withFiles("pic3",pic3);
        int retCode = client.withService("UCircle.Post")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("title",title)
                .withParams("content",content)
                .upload();
        /*PhalApiClient client=createClient();
        PhalApiClientResponse response = client
                .withService("UCircle.Post")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("title",title)
                .withParams("content",content)
                .withTimeout(SET_TIME_OUT)
                .request();*/
        if(retCode == 200)
        {
           return 200;
        }
        else
        {
            throw new UServerAccessException(retCode);
        }
    }
    public static JSONArray getUserTask(@NonNull int page,@NonNull int flag)throws UServerAccessException
    {
            UserOperatorController user = UserOperatorController.getInstance();
            String id = user.getId();
            id = UrlEncode(id);
            String passport = user.getPassport();
            passport = UrlEncode(passport);
            String Page = String.valueOf(page);
            Page = UrlEncode(Page);
            String Flag = String.valueOf(flag);
            Flag = UrlEncode(Flag);
        PhalApiClient client=createClient();
        PhalApiClientResponse response = client
                .withService("Task.GetUserTasks")
            .withParams("id",id)
            .withParams("passport",passport)
            .withParams("page",Page)
            .withParams("flag",Flag)
            .withTimeout(SET_TIME_OUT)
            .request();
        if(response.getRet() == 200)
        {
            try {
                JSONArray info = new JSONArray(response.getData());
                return  info;
            }catch (JSONException e)
            {
                Log.e("ServerAccessApi",e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }

        }
        else
        {
            throw new UServerAccessException(response.getRet());
        }
    }
    public static JSONObject getUciclePost(@NonNull String postId)throws UServerAccessException
    {
        UserOperatorController user = UserOperatorController.getInstance();
        String Id = user.getId();
        String passport = user.getPassport();
        Id = UrlEncode(Id);
        passport = UrlEncode(passport);
        postId = UrlEncode(postId);
        PhalApiClient client = createClient();
        PhalApiClientResponse response = client
               .withService("UCircle.GetPost")
                .withParams("id",Id)
                .withParams("passport",passport)
                .withParams("postID",postId)
                .withTimeout(SET_TIME_OUT)
                .request();
        if (response.getRet() == 200) {
            try {
                JSONObject info = new JSONObject(response.getData());
                return info;
            } catch (JSONException e) {
                Log.e("ServerAccessApi", e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        } else {
            throw new UServerAccessException(response.getRet());
        }
    }
    public static String Comment(String postId,String content)throws UServerAccessException
    {
        UserOperatorController user = UserOperatorController.getInstance();
        String Id = user.getId();
        String passport = user.getPassport();
        Id = UrlEncode(Id);
        passport = UrlEncode(passport);
        postId = UrlEncode(postId);
        content = UrlEncode(content);
        PhalApiClient client = createClient();
        PhalApiClientResponse response = client
                .withService("UCircle.Comment")
                .withParams("id",Id)
                .withParams("passport",passport)
                .withParams("postID",postId)
                .withParams("content",content)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
                return "success";
        }else{
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }
    public static JSONArray getUCicleList(int frompost) throws UServerAccessException {
        UserOperatorController user = UserOperatorController.getInstance();
        String Id = user.getId();
        String passport = user.getPassport();
        Id = UrlEncode(Id);
        passport = UrlEncode(passport);
        String Frompost;
        if(frompost < 0)
        {
            Frompost = "";
        }
       else
        {
            Frompost = frompost + "";
        }
        Frompost = UrlEncode(Frompost);
        PhalApiClient client = createClient();
        PhalApiClientResponse response = client
                .withService("UCircle.GetList")
                .withParams("id", Id)
                .withParams("fromPost",Frompost)
                .withParams("passport", passport)
                .withTimeout(SET_TIME_OUT)
                .request();
        if (response.getRet() == 200) {
            try {
                JSONArray info = new JSONArray(response.getData());
                return info;
            } catch (JSONException e) {
                Log.e("ServerAccessApi", e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        } else {
            throw new UServerAccessException(response.getRet());
        }
    }

    public static PhalApiClientResponse cancelTask(@NonNull String postID) throws UServerAccessException {
        UserOperatorController user = UserOperatorController.getInstance();
        String id = user.getId();
        id = UrlEncode(id);
        String passport = user.getPassport();
        passport = UrlEncode(passport);
        postID = UrlEncode(postID);
        return createClient().withService("Task.Cancel")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("postID", postID)
                .withTimeout(SET_TIME_OUT)
                .request();
    }
    public static String getLoginToken(@NonNull String userName)throws UServerAccessException{
        //断言，保证传入参数的正确性，在DEBUG模式下才启用。
        if(BuildConfig.DEBUG){
            UPublicTool.UAssert( byteCount(userName)>=4 && byteCount(userName)<=25 );
            //当这个函数里表达式的值为false时，抛出断言异常，然后终止程序。
            //这么做是为了保证调用者进行了参数检查。参数的范围在文档里提到过。
        }
        userName=UrlEncode(userName);//对参数进行UrlEncode处理，才能POST出去
        //这个处理非常重要
        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("User.GetLoginToken")//接口的名称
                .withParams("username",userName)//插入一个参数对
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200) {
            try{
                JSONObject data=new JSONObject(response.getData());
                return data.getString("loginToken");
            }catch (JSONException e){
                Log.e("ServerAccessApi",e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else {
            throw new UServerAccessException(response.getRet());
        }
    }


    //这个需要返回一个数据包，所以返回类型是JSONObject
    public static JSONObject getTaskPost(@NonNull String id,@NonNull String passport,@NonNull String postID) throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);
        PhalApiClientResponse response=createClient()
                .withService("Task.GetPost")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("postID",postID)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try {
                Log.e("TaskDetailsActivity", response.getData());
                return new JSONObject(response.getData());
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }

    public static PhalApiClientResponse verifyTaker(@NonNull String id, @NonNull String passport,
                                                    @NonNull String postID, @NonNull String verifyID) throws UServerAccessException {
        id = UrlEncode(id);
        passport = UrlEncode(passport);
        postID = UrlEncode(postID);
        verifyID = UrlEncode(verifyID);
        Log.e("ServerAccessApi", "id = " + id + ", passport = " + passport + ", postID = " + postID + ", verifyID = " + verifyID);
        return createClient()
                .withService("Task.VerifyTaker")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("postID", postID)
                .withParams("verifyID", verifyID)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse giveUpTask(@NonNull String id, @NonNull String passport,
                                                   @NonNull String postID) throws UServerAccessException {
        id = UrlEncode(id);
        passport = UrlEncode(passport);
        postID = UrlEncode(postID);
        Log.e("ServiceAccessApi",  "id = " + id + ", passport = " + passport + ", postID = " + postID);
        return createClient()
                .withService("Task.GiveUpTask")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("postID", postID)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse forceGiveUpTask(@NonNull String id, @NonNull String passport,
                                                        @NonNull String postID) throws UServerAccessException {
        id = UrlEncode(id);
        passport = UrlEncode(passport);
        postID = UrlEncode(postID);
        Log.e("ServiceAccessApi",  "id = " + id + ", passport = " + passport + ", postID = " + postID);
        return createClient()
                .withService("Task.ForceGiveUpTask")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("postID", postID)
                .withTimeout(SET_TIME_OUT)
                .request();
    }


    public static String cancelTask(@NonNull String id,@NonNull String passport,@NonNull String postID)throws UServerAccessException{

        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("Task.Cancel")//接口的名称
                .withParams("id",id)//插入一个参数对
                .withParams("passport",passport)//插入一个参数对
                .withParams("postID",postID)//插入一个参数对
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
            return response.getData();
        }else{
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }



    public static String delComment(@NonNull String id,@NonNull String passport,@NonNull String commentID)throws UServerAccessException{

        id=UrlEncode(id);
        passport=UrlEncode(passport);
        commentID=UrlEncode(commentID);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("Task.DelComment")//接口的名称
                .withParams("id",id)//插入一个参数对
                .withParams("passport",passport)//插入一个参数对
                .withParams("commentID",commentID)//插入一个参数对
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
                try{
                JSONObject data=new JSONObject(response.getData());
                return data.getString("success");
            }catch (JSONException e){
                Log.e("ServerAccessApi",e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }



    public static String editTask(@NonNull String id,@NonNull String passport,@NonNull String postID,String title,
                                  String tag,String description,String price,String path,
                                  String activeTime,String position)throws UServerAccessException{
        if(BuildConfig.DEBUG){
            if(title!=null)
            UPublicTool.UAssert( byteCount(title)>=5 && byteCount(title)<=30 );
            if(tag!=null)
            UPublicTool.UAssert( tag.length()<=30 );
            if(description!=null)
            UPublicTool.UAssert( description.length()<=450);
            if(path!=null)
            UPublicTool.UAssert( path.length()<=400 );
        }

        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);
        title=UrlEncode(title);
        tag=UrlEncode(tag);
        description=UrlEncode(description);
        price=UrlEncode(price);
        path=UrlEncode(path);
        activeTime=UrlEncode(activeTime);
        position=UrlEncode(position);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("Task.Edit")//接口的名称
                .withParams("id",id)//插入一个参数对
                .withParams("passport",passport)
                .withParams("postID",postID)
                .withParams("title",title)
                .withParams("tag",tag)
                .withParams("description",description)
                .withParams("price",price)
                .withParams("path",path)
                .withParams("activetime",activeTime)
                .withParams("position",position)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
            return response.getData();
        }else{ 
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }

    public static PhalApiClientResponse verifyFinish(@NonNull String id, @NonNull String passport,
                                      @NonNull String taskID) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        Log.e("TaskDetailsActivity", "id = " + id + ", passport = " + passport + ", postID = " + taskID);
        return createClient()
                .withService("Task.VerifyFinish")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("taskID", taskID)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse finishTask(@NonNull String id, @NonNull String passport,
                                                   @NonNull String postID) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        return createClient()
                .withService("Task.FinishTask")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("taskID", postID)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static JSONArray getUserTasks(@NonNull String id,@NonNull String passport,int page,int flag)throws UServerAccessException{
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        PhalApiClientResponse response=createClient()
                .withService("Task.GetUserTasks")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("page",String.valueOf(page))
                .withParams("flag",String.valueOf(flag))
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try {
                return new JSONArray(response.getData());
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }
    public static String getComment(@NonNull String id,@NonNull String passport,@NonNull String postID,
                                        String start) throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);
        start=UrlEncode(start);
        PhalApiClientResponse response=createClient()
                .withService("Task.GetComment")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("postID",postID)
                .withParams("start",start)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            return response.getData();
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }

    public static String getTakers(@NonNull String id,@NonNull String passport,
                                   @NonNull String postID) throws UServerAccessException{
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);
        PhalApiClientResponse response=createClient()
                .withService("Task.GetTakers")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("task_id",postID)
                .withTimeout(SET_TIME_OUT)
                .request();
//        Log.e("haha", "response: " + response.getData());
        if(response.getRet()==200) {
            return response.getData();
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }

    public static PhalApiClientResponse getProjectList(@NonNull String id,@NonNull String passport,
                                                       @NonNull String start, @NonNull String num,
                                                       @NonNull String position) throws UServerAccessException {
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        start=UrlEncode(start);
        num=UrlEncode(num);
        position=UrlEncode(position);
        return createClient()
                .withService("Task.GetList")
                .withParams("id", id)
                .withParams("passport", "uledatest")
                .withParams("orderBy", "distance")
                .withParams("start", start)
                .withParams("num", num)
                .withParams("tag", UrlEncode(UTaskManager.TAG_PROJECT))
                .withParams("position", position)
                .withTimeout(SET_TIME_OUT)
                .request();
    }


    public static JSONArray getTaskList(@NonNull String id,@NonNull String passport,
            @NonNull String orderBy,@NonNull String start,
            @NonNull String num,@NonNull String tag,
            @NonNull String position) throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        orderBy=UrlEncode(orderBy);
        start=UrlEncode(start);
        num=UrlEncode(num);
        tag=UrlEncode(tag);
        position=UrlEncode(position);
        PhalApiClientResponse response=createClient()
                .withService("Task.GetList")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("orderBy",orderBy)
                .withParams("start",start)
                .withParams("num",num)
                .withParams("tag",tag)
                .withParams("position",position)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try {
                return new JSONArray(response.getData());
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }

    public static String postTask(@NonNull String id,@NonNull String passport,@NonNull String title,
                                    @NonNull String tag,String description,@NonNull String price,String path,
                                    @NonNull String activeTime,@NonNull String position)throws UServerAccessException{
        if(BuildConfig.DEBUG){
            UPublicTool.UAssert( byteCount(title)>=5 && byteCount(title)<=30 );
            UPublicTool.UAssert( byteCount(title)<=30 );
            if(description!=null)
                UPublicTool.UAssert( byteCount(description)<=450);
            if(path!=null)
                UPublicTool.UAssert( byteCount(path)<=400 );
        }

        id=UrlEncode(id);
        passport=UrlEncode(passport);
        title=UrlEncode(title);
        tag=UrlEncode(tag);
        description=UrlEncode(description);
        price=UrlEncode(price);
        path=UrlEncode(path);
        activeTime=UrlEncode(activeTime);
        position=UrlEncode(position);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("Task.Post")//接口的名称
                .withParams("id",id)//插入一个参数对
                .withParams("passport",passport)
                .withParams("title",title)
                .withParams("tag",tag)
                .withParams("description",description)
                .withParams("price",price)
                .withParams("path",path)
                .withParams("activeTime",activeTime)
                .withParams("position",position)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
            Log.e("TaskDetailsActivity", "successful release: " + response.getData());
            try{
                JSONObject data=new JSONObject(response.getData());
                return data.getString("postID");
            }catch (JSONException e){
                Log.e("ServerAccessApi",e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            //网络访问失败，抛出一个网络异常
            Log.e("TaskDetailsActivity", "fail release: " + response.getMsg());
            throw new UServerAccessException(response.getRet());
        }
    }

    public static String postComment(@NonNull String id,@NonNull String passport,
                                     @NonNull String postID,@NonNull String comment)throws UServerAccessException{
        if(BuildConfig.DEBUG){
            UPublicTool.UAssert( byteCount(comment)<=300 );
        }
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);
        comment=UrlEncode(comment);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("Task.PostComment")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("postID",postID)
                .withParams("comment",comment)
                .withTimeout(SET_TIME_OUT)
                .request();
        Log.e("haha", "status: " + response.getRet() + ", data: " + response.getData() + ", msg: " + response.getMsg());
        if(response.getRet()==200){//200的意思是正常返回
            return response.getData();
        }else{
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }

    public static JSONArray getActivities(@NonNull String id, @NonNull String passport, @NonNull String tag,
                                                String from, String count) throws UServerAccessException {
        id = UrlEncode(id);
        passport = UrlEncode(passport);
        count = UrlEncode(count);

        RequestBody body = new FormBody.Builder()
                .add("service", "Activity.GetActivityList")
                .add("id", id)
                .add("passport", passport)
                .add("tag", tag)
                .add("from", from)
                .add("count", count)
                .build();
        Request request = new Request.Builder().url(BASE_URL).post(body).build();
        OkHttpClient client = new OkHttpClient.Builder().build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                String bodyString = response.body().string();
                Log.e("wa", bodyString);
                JSONObject obj = new JSONObject(bodyString);
                return obj.getJSONArray("data");
            }
        } catch (IOException | JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;

//        PhalApiClientResponse response = createClient()
//                .withService("Activity.GetActivityList")
//                .withParams("id", id)
//                .withParams("passport", passport)
//                .withParams("tag", tag)
//                .withParams("from", "10")
//                .withParams("count", count)
//                .withTimeout(SET_TIME_OUT)
//                .request();
//        Log.e("wa", response.getRet() + ", " + response.getData() + ", " + response.getMsg());
//        if(response.getRet() == 200) {
//            try{
//                return new JSONArray(response.getData());
//            }catch (JSONException e){
//                Log.e("ServerAccessApi",e.toString());
//                //数据包无法解析，向上抛出一个异常
//            }
//        }
//        return null;
    }

    public static PhalApiClientResponse getPromotedActivities(@NonNull String id, @NonNull String passport) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        return createClient()
                .withService("Activity.GetPromotedActivity")
                .withParams("id", id)
                .withParams("passport", passport)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse getActivityDetail(@NonNull String id, @NonNull String passport,
                                                          @NonNull String actId) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        actId = UrlEncode(actId);
        return createClient()
                .withService("Activity.GetActivity")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("actId", actId)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse editActivity(@NonNull String id, @NonNull String passport,
                                                     @NonNull String actId, @NonNull String title,
                                                     @NonNull String description, @NonNull String holdTime,
                                                     @NonNull String lat, @NonNull String lon,
                                                     @NonNull String takerCountLimit, @NonNull String location)
        throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        actId = UrlEncode(actId);
        title = UrlEncode(title);
        description = UrlEncode(description);
        long activeTime = (Long.parseLong(holdTime) - System.currentTimeMillis()) / 1000;
        String activeTimeStr = UrlEncode(String.valueOf(activeTime));
        String position = lat + "," + lon;
        takerCountLimit = UrlEncode(takerCountLimit);
        location = UrlEncode(location);
        return createClient()
                .withService("Activity.Edit")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("actId", actId)
                .withParams("title", title)
                .withParams("description", description)
                .withParams("activeTime", activeTimeStr)
                .withParams("position", position)
                .withParams("takerCountLimit", takerCountLimit)
                .withParams("location", location)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse participateInActivity(@NonNull String id, @NonNull String passport,
                                                              @NonNull String actId) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        actId = UrlEncode(actId);
        return createClient()
                .withService("Activity.Participate")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("actId", actId)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse cancelParticipateInActivity(@NonNull String id, @NonNull String passport,
                                                              @NonNull String actId) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        actId = UrlEncode(actId);
        return createClient()
                .withService("Activity.CancelParticipation")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("actId", actId)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse postActivityComment(@NonNull String id, @NonNull String passport,
                                                            @NonNull String actId, @NonNull String content, @NonNull String postDate) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        actId = UrlEncode(actId);
        postDate = UrlEncode(postDate);
        content = UrlEncode(content);
        return createClient()
                .withService("Activity.AddComment")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("actId", actId)
                .withParams("content", content)
                .withParams("postdate", postDate)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static PhalApiClientResponse getActivityTakers(@NonNull String id, @NonNull String passport,
                                                          @NonNull String actId) throws UServerAccessException {
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        actId = UrlEncode(actId);
        return createClient()
                .withService("Activity.GetActivityTaker")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("actId", actId)
                .withTimeout(SET_TIME_OUT)
                .request();
    }

    public static int postActivity(@NonNull String id,@NonNull String passport,@NonNull String title,
                                  @NonNull String tag,String description,@NonNull String activeTime,
                                  double latitude, double longitude, @NonNull String takerCountLimit, @NonNull String location,
                                      List<String> imgPaths)
            throws UServerAccessException, IOException {
        if(BuildConfig.DEBUG){
            UPublicTool.UAssert(byteCount(title) >= 5 && byteCount(title) <= 30 && latitude > 0 && longitude > 0);
            if(description!=null)
                UPublicTool.UAssert(byteCount(description) <= 450);
        }

        String position = latitude + "," + longitude;
        id = UrlEncode(id);
        passport = UrlEncode(passport);
//        title = UrlEncode(title);
//        tag = UrlEncode(tag);
//        description = UrlEncode(description);
//        activeTime = UrlEncode(activeTime);
//        position = UrlEncode(position);
//        takerCountLimit = UrlEncode(takerCountLimit);
//        location = UrlEncode(location);

        UPictureUploader uploadBody = UPictureUploader.create(BASE_URL)
                .withService("Activity.Post")
                .withParams("id", id)
                .withParams("passport", passport)
                .withParams("title", title)
                .withParams("tag", tag)
                .withParams("description", description)
                .withParams("activeTime", activeTime)
                .withParams("position", position)
                .withParams("takerCountLimit", takerCountLimit)
                .withParams("location", location);
        for (int i = 0; i < imgPaths.size(); i++) {
            String path = imgPaths.get(i);
            File file = new File(path);
            if (!file.exists()) throw new IOException();
            uploadBody.withFiles("pic" + (i + 1), file);
        }

        int code = uploadBody.upload();
        if (code != 200) Log.e("upload", uploadBody.getRet());
        return code;
    }

    public static String followUser(@NonNull String id,@NonNull String passport,@NonNull String followByID)throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        followByID=UrlEncode(followByID);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("User.Follow")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("followByID",followByID)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
            try{
                JSONObject data=new JSONObject(response.getData());
                return data.getString("success");
            }catch (JSONException e){
                Log.e("ServerAccessApi",e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }

    public static JSONObject getBasicInfo(@NonNull String id,@NonNull String passport,@NonNull String getByID) throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        String k=getByID;
        getByID= UrlEncode(getByID);
        PhalApiClientResponse response=createClient()
                .withService("User.GetBasicInfo")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("getByID",getByID)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try {
                return new JSONObject(response.getData());
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }

    public static JSONObject getBasicInfoByName(@NonNull String id,@NonNull String passport,@NonNull String friendName) throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        friendName= UrlEncode(friendName);
        PhalApiClientResponse response=createClient()
                .withService("User.GetBasicInfoByName")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("getByName",friendName)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try {
                return new JSONObject(response.getData());
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }

    public static JSONObject login(@NonNull String username,@NonNull String passport) throws UServerAccessException{
        if(BuildConfig.DEBUG){
            UPublicTool.UAssert(byteCount(username)>=4 && byteCount(username)<=25 );
        }
        username=UrlEncode(username);
        passport=UrlEncode(passport);
        PhalApiClientResponse response=createClient()
                .withService("User.Login")
                .withParams("username",username)
                .withParams("passport",passport)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            try {
                return new JSONObject(response.getData());
            }catch (JSONException e){
                e.printStackTrace();
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }
    public static String acceptTask(@NonNull String id,@NonNull String passport,@NonNull String postID)throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        postID=UrlEncode(postID);
        PhalApiClient client=createClient();
        PhalApiClientResponse response = client
                .withService("Task.Accept")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("postID",postID)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){
            return "success";
        }else{
            return response.getMsg();
        }
    }
    public static String getPicture(@NonNull String id,@NonNull String passport,int picId)throws UServerAccessException{
        PhalApiClient client = createClient();
        PhalApiClientResponse response = client
                .withService("Picture.GetPicture")
                .withParams("picId",String.valueOf(picId))
                .withParams("id",id)
                .withParams("passport",passport)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet() == 200){
            return response.getData();
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }
    public static String uploadAvatar(@NonNull String id,@NonNull String passport,@NonNull String picture)throws UServerAccessException{
        id=UrlEncode(id);
        passport = UrlEncode(passport);
        picture = UrlEncode(picture);
        PhalApiClient client = createClient();
        PhalApiClientResponse response= client.withService("Picture.UploadAvatar")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("pic",picture)
                .request();
        if(response.getRet() == 200){
            return response.getData();
        }else{
            throw new UServerAccessException(response.getRet());
        }
    }
    public static String unfollowUser(@NonNull String id,@NonNull String passport,@NonNull String unfollowByID)throws UServerAccessException{
        id=UrlEncode(id);
        passport=UrlEncode(passport);
        unfollowByID=UrlEncode(unfollowByID);

        PhalApiClient client = createClient();
        PhalApiClientResponse response=client
                .withService("User.Unfollow")
                .withParams("id",id)
                .withParams("passport",passport)
                .withParams("followByID",unfollowByID)
                .withTimeout(SET_TIME_OUT)
                .request();
        if(response.getRet()==200){//200的意思是正常返回
            try{
                JSONObject data=new JSONObject(response.getData());
                return data.getString("success");
            }catch (JSONException e){
                Log.e("ServerAccessApi",e.toString());
                //数据包无法解析，向上抛出一个异常
                throw new UServerAccessException(UServerAccessException.ERROR_DATA);
            }
        }else{
            //网络访问失败，抛出一个网络异常
            throw new UServerAccessException(response.getRet());
        }
    }


    private static PhalApiClient createClient(){
        //这个函数创造一个客户端实例
        return PhalApiClient.create()
                .withHost("http://118.89.156.167/mobile/");
    }

    private static String UrlEncode(String str)throws UServerAccessException{
        try{
            if(str==null)return null;
            return URLEncoder.encode(str,"UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            throw new UServerAccessException(UServerAccessException.PARAMS_ERROR);
        }
    }

    private static String UrlDecode(String str)throws UServerAccessException{
        try{
            if(str==null)return null;
            return URLDecoder.decode(str, "UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            throw new UServerAccessException(UServerAccessException.PARAMS_ERROR);
        }
    }

    private static int byteCount(String s){
        return s.getBytes().length;
    }
    private ServerAccessApi(){
        //该类不生成实例
    }
}
