package com.example.httpsender.fragment;

import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.Nullable;

import com.example.httpsender.OnError;
import com.example.httpsender.R;
import com.example.httpsender.databinding.RxjavaFragmentBinding;
import com.example.httpsender.entity.Article;
import com.example.httpsender.entity.Location;
import com.example.httpsender.entity.Name;
import com.example.httpsender.entity.NewsDataXml;
import com.google.gson.Gson;
import com.rxjava.rxlife.RxLife;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import rxhttp.wrapper.param.RxHttp;

/**
 * 使用RxJava+OkHttp发请求
 * User: ljx
 * Date: 2020/4/24
 * Time: 18:16
 */
public class RxJavaFragment extends BaseFragment<RxjavaFragmentBinding> implements OnClickListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rxjava_fragment);
    }

    @Override
    public void onViewCreated(@NotNull RxjavaFragmentBinding binding, Bundle savedInstanceState) {
        super.onViewCreated(binding, savedInstanceState);
        binding.setClick(this);
    }

    @SuppressWarnings("deprecation")
    public void bitmap(View view) {
        String imageUrl = "http://img2.shelinkme.cn/d3/photos/0/017/022/755_org.jpg@!normal_400_400?1558517697888";
        RxHttp.get(imageUrl) //Get请求
            .asBitmap()  //这里返回Observable<Bitmap> 对象
            .to(RxLife.toMain(this))  //感知生命周期，并在主线程回调
            .subscribe(bitmap -> {
                mBinding.tvResult.setBackground(new BitmapDrawable(bitmap));
                //成功回调
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("图片加载失败,请稍后再试!");
            });
    }

    //发送Get请求，获取文章列表
    public void sendGet(View view) {
        RxHttp.get("/article/list/0/json")
            .asResponsePageList(Article.class)
            .to(RxLife.toMain(this))  //感知生命周期，并在主线程回调
            .subscribe(pageList -> {
                mBinding.tvResult.setText(new Gson().toJson(pageList));
                //成功回调
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("发送失败,请稍后再试!");
            });
    }

    //发送Post表单请求,根据关键字查询文章
    public void sendPostForm(View view) {
        RxHttp.postForm("/article/query/0/json")
            .add("k", "性能优化")
            .asResponsePageList(Article.class)
            .to(RxLife.toMain(this))  //感知生命周期，并在主线程回调
            .subscribe(pageList -> {
                mBinding.tvResult.setText(new Gson().toJson(pageList));
                //成功回调
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("发送失败,请稍后再试!");
            });
    }

    //发送Post Json请求，此接口不通，仅用于调试参数
    public void sendPostJson(View view) {
        //发送以下User对象
        /*
           {
               "name": "张三",
               "sex": 1,
               "height": 180,
               "weight": 70,
               "interest": [
                   "羽毛球",
                   "游泳"
               ],
               "location": {
                   "latitude": 30.7866,
                   "longitude": 120.6788
               },
               "address": {
                   "street": "科技园路.",
                   "city": "江苏苏州",
                   "country": "中国"
               }
           }
         */
        List<String> interestList = new ArrayList<>();//爱好
        interestList.add("羽毛球");
        interestList.add("游泳");
        String address = "{\"street\":\"科技园路.\",\"city\":\"江苏苏州\",\"country\":\"中国\"}";

        RxHttp.postJson("/article/list/0/json")
            .add("name", "张三")
            .add("sex", 1)
            .addAll("{\"height\":180,\"weight\":70}") //通过addAll系列方法添加多个参数
            .add("interest", interestList) //添加数组对象
            .add("location", new Location(120.6788, 30.7866))  //添加位置对象
            .addJsonElement("address", address) //通过字符串添加一个对象
            .asString()
            .to(RxLife.toMain(this))  //感知生命周期，并在主线程回调
            .subscribe(s -> {
                mBinding.tvResult.setText(s);
                //成功回调
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("发送失败,请稍后再试!");
            });
    }


    //发送Post JsonArray请求，此接口不通，仅用于调试参数
    public void sendPostJsonArray(View view) {
        //发送以下Json数组
        /*
           [
               {
                   "name": "张三"
               },
               {
                   "name": "李四"
               },
               {
                   "name": "王五"
               },
               {
                   "name": "赵六"
               },
               {
                   "name": "杨七"
               }
           ]
         */
        List<Name> names = new ArrayList<>();
        names.add(new Name("赵六"));
        names.add(new Name("杨七"));
        RxHttp.postJsonArray("/article/list/0/json")
            .add("name", "张三")
            .add(new Name("李四"))
            .addJsonElement("{\"name\":\"王五\"}")
            .addAll(names)
            .asString()
            .to(RxLife.toMain(this))
            .subscribe(s -> {
                mBinding.tvResult.setText(s);
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("发送失败,请稍后再试!");
            });
    }

    //使用XmlConverter解析数据，此接口返回数据太多，会有点慢
    public void xmlConverter(View view) {
        RxHttp.get("http://webservices.nextbus.com/service/publicXMLFeed?command=routeConfig&a=sf-muni")
            .setXmlConverter()
            .asClass(NewsDataXml.class)
            .to(RxLife.toMain(this))  //感知生命周期，并在主线程回调
            .subscribe(dataXml -> {
                mBinding.tvResult.setText(new Gson().toJson(dataXml));
                //成功回调
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("发送失败,请稍后再试!");
            });
    }

    //使用XmlConverter解析数据
    public void fastJsonConverter(View view) {
        RxHttp.get("/article/list/0/json")
            .setFastJsonConverter()
            .asResponsePageList(Article.class)
            .to(RxLife.toMain(this))  //感知生命周期，并在主线程回调
            .subscribe(pageList -> {
                mBinding.tvResult.setText(new Gson().toJson(pageList));
                //成功回调
            }, (OnError) error -> {
                mBinding.tvResult.setText(error.getErrorMsg());
                //失败回调
                error.show("发送失败,请稍后再试!");
            });
    }


    public void clearLog(View view) {
        mBinding.tvResult.setText("");
        mBinding.tvResult.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bitmap:
                bitmap(v);
                break;
            case R.id.sendGet:
                sendGet(v);
                break;
            case R.id.sendPostForm:
                sendPostForm(v);
                break;
            case R.id.sendPostJson:
                sendPostJson(v);
                break;
            case R.id.sendPostJsonArray:
                sendPostJsonArray(v);
                break;
            case R.id.xmlConverter:
                xmlConverter(v);
                break;
            case R.id.fastJsonConverter:
                fastJsonConverter(v);
                break;
            case R.id.bt_clear:
                clearLog(v);
                break;
        }
    }

}
